package com.shieldstream.tv.data.security

import android.content.Context
import android.content.SharedPreferences
import com.shieldstream.tv.domain.ApiService
import com.shieldstream.tv.domain.PortableBackup
import com.shieldstream.tv.domain.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

internal object BackupCipher {
    private val magic = "CFRAN01".toByteArray(Charsets.US_ASCII)
    private const val ROUNDS = 600_000
    private const val MAX_BYTES = 16 * 1024 * 1024
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ROUNDS, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally { spec.clearPassword() }
    }
    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 12 && data.size <= MAX_BYTES)
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        return magic + salt + nonce + cipher.doFinal(data)
    }
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val offset = magic.size
        require(data.size in (offset + 44)..(MAX_BYTES + 64) && data.copyOfRange(0, offset).contentEquals(magic))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, data.copyOfRange(offset, offset + 16)), GCMParameterSpec(128, data.copyOfRange(offset + 16, offset + 28)))
        cipher.updateAAD(magic)
        return cipher.doFinal(data.copyOfRange(offset + 28, data.size))
    }
}

@Singleton
class PortableBackupImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: ProfileRepository,
    private val tokens: TokenStore,
) : PortableBackup {
    override val allowed: Boolean get() = !profiles.active.value.kids && android.os.Build.VERSION.SDK_INT >= 26

    private fun pref(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private fun names(ids: List<String>): Set<String> = setOf("settings", "profiles") + ids.flatMap { id ->
        listOf(if (id == "default") "watch_progress" else "watch_progress_$id", "my_list_$id", "episode_tracking_$id")
    }
    private val secretNames = ApiService.entries.map(TokenStore::keyNameFor) + "trakt_app_credentials"

    override fun export(password: CharArray): ByteArray {
        check(allowed)
        val ids = profiles.profiles.value.map { it.id }
        val data = JSONObject()
        names(ids).forEach { name -> data.put(name, encode(pref(name).all)) }
        val secrets = JSONObject()
        secretNames.forEach { name -> secrets.put(name, tokens.getSecret(name) ?: JSONObject.NULL) }
        val root = JSONObject().put("schema", 1).put("preferences", data).put("credentials", secrets)
        val plain = root.toString().toByteArray(Charsets.UTF_8)
        return try { BackupCipher.encrypt(plain, password) } finally { plain.fill(0) }
    }

    override fun restore(encrypted: ByteArray, password: CharArray) {
        check(allowed)
        val plain = BackupCipher.decrypt(encrypted, password)
        val root = try { JSONObject(String(plain, Charsets.UTF_8)) } finally { plain.fill(0) }
        require(root.getInt("schema") == 1)
        val data = root.getJSONObject("preferences")
        val decoded = data.keys().asSequence().associateWith { name -> decode(data.getJSONObject(name)) }
        val profileValues = decoded.getValue("profiles").toMutableMap()
        val stored = JSONArray(profileValues["list"] as? String ?: error("Missing profiles"))
        require(stored.length() in 1..5)
        val ids = (0 until stored.length()).map { i ->
            val profile = stored.getJSONObject(i)
            profile.getString("id").also { require(it.matches(Regex("[A-Za-z0-9_-]{1,64}"))) }
        }
        require(ids.distinct().size == ids.size && decoded.keys == names(ids))
        // Trakt OAuth sessions are device-local; reconnect after restore, keep local history.
        for (i in 0 until stored.length()) stored.getJSONObject(i).remove("traktUser")
        profileValues["list"] = stored.toString()
        require(profileValues["active"] == null || profileValues["active"] in ids)
        val credentials = root.getJSONObject("credentials")
        require(credentials.keys().asSequence().toSet() == secretNames.toSet())
        val newSecrets = secretNames.associateWith { name -> if (credentials.isNull(name)) null else credentials.getString(name).also { require(it.length <= 131072) } }
        val previous = decoded.keys.associateWith { pref(it).all.toMap() }
        val oldSecrets = secretNames.associateWith(tokens::getSecret)
        try {
            decoded.forEach { (name, values) -> write(pref(name), if (name == "profiles") profileValues else values) }
            tokens.restoreSecrets(newSecrets)
        } catch (e: Exception) {
            previous.forEach { (name, values) -> runCatching { write(pref(name), values) } }
            runCatching { tokens.restoreSecrets(oldSecrets) }
            throw e
        }
    }

    private fun encode(values: Map<String, *>): JSONObject = JSONObject().apply {
        values.forEach { (name, value) ->
            val entry = JSONObject()
            when (value) {
                is String -> entry.put("t", "s").put("v", value)
                is Boolean -> entry.put("t", "b").put("v", value)
                is Int -> entry.put("t", "i").put("v", value)
                is Long -> entry.put("t", "l").put("v", value.toString())
                is Float -> entry.put("t", "f").put("v", value.toString())
                is Set<*> -> entry.put("t", "set").put("v", JSONArray(value.toList()))
                else -> error("Unsupported preference")
            }
            put(name, entry)
        }
    }
    private fun decode(obj: JSONObject): Map<String, Any> = obj.keys().asSequence().associateWith { name ->
        require(name.length <= 256)
        val item = obj.getJSONObject(name)
        when (item.getString("t")) {
            "s" -> item.getString("v")
            "b" -> item.getBoolean("v")
            "i" -> item.getInt("v")
            "l" -> item.getString("v").toLong()
            "f" -> item.getString("v").toFloat().also { require(it.isFinite()) }
            "set" -> item.getJSONArray("v").let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }
            else -> error("Unsupported preference")
        }
    }
    private fun write(prefs: SharedPreferences, values: Map<String, *>) {
        val editor = prefs.edit().clear()
        values.forEach { (name, value) ->
            when (value) {
                is String -> editor.putString(name, value)
                is Boolean -> editor.putBoolean(name, value)
                is Int -> editor.putInt(name, value)
                is Long -> editor.putLong(name, value)
                is Float -> editor.putFloat(name, value)
                is Set<*> -> editor.putStringSet(name, value.map { it as String }.toSet())
                else -> error("Invalid preference")
            }
        }
        check(editor.commit()) { "Preference storage write failed" }
    }
}
