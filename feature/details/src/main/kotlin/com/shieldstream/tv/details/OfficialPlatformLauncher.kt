package com.shieldstream.tv.details

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.MediaType
import com.shieldstream.tv.model.PlatformOpening
import com.shieldstream.tv.model.StreamingPlatforms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Public Wikidata mappings only; never authenticates to a streaming service. */
internal object OfficialPlatformLauncher {
    private const val TAG = "CinefranPlatforms"
    private const val CACHE = "cinefran_platform_links"
    private const val TTL = 7L * 24 * 60 * 60 * 1000

    internal fun links(platform: String, id: String, mode: PlatformOpening, series: Boolean): List<String> {
        return when (platform) {
            "netflix" -> if (id.matches(Regex("[0-9]{6,8}"))) {
                val detail = "https://www.netflix.com/title/$id"
                val nativeDetail = "nflx://www.netflix.com/title/$id"
                if (mode == PlatformOpening.PLAY && !series) listOf("nflx://www.netflix.com/watch/$id", "https://www.netflix.com/watch/$id", nativeDetail, detail) else listOf(nativeDetail, detail)
            } else emptyList()
            "prime" -> if (id.matches(Regex("[A-Z0-9]{26}|amzn[0-9]\\.dv\\.gti\\.[0-9a-fA-F-]{36}"))) {
                listOf("https://app.primevideo.com/detail?gti=$id", "https://www.primevideo.com/detail/$id")
            } else emptyList()
            "max" -> if (id.matches(Regex("(?:episode|extra|feature|movie|franchise|season|series|show|shows)/(?:urn:hbo:(?:episode|extra|feature|movie|franchise|season|series|show|shows):[A-Za-z0-9_-]{21}|[0-9a-fA-F-]{36})"))) {
                listOf("https://play.hbomax.com/$id")
            } else emptyList()
            else -> emptyList()
        }
    }

    private suspend fun resolve(context: Context, platform: String, item: CatalogItem): List<String> = withContext(Dispatchers.IO) {
        val tmdb = item.id.toIntOrNull()?.takeIf { it > 0 }
        val imdb = item.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }
        if (tmdb == null && imdb == null) return@withContext emptyList()
        val properties = when (platform) {
            "netflix" -> listOf("P1874")
            "prime" -> listOf("P8055")
            "max" -> listOf("P8298")
            else -> return@withContext emptyList()
        }
        val tmdbProp = if (item.type == MediaType.TV) "P4983" else "P4947"
        val prefs = context.applicationContext.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
        val cacheKey = "v2:$platform:${item.key}"
        val cached = prefs.getString(cacheKey, null)
        if (cached != null && System.currentTimeMillis() - prefs.getLong("$cacheKey:time", 0) < (if (cached.isBlank()) 15 * 60_000L else TTL))
            return@withContext cached.split("|").filter { it.isNotBlank() }
        val values = properties.joinToString(" ") { "wdt:$it" }
        val matches = listOfNotNull(tmdb?.let { "{ ?work wdt:$tmdbProp \"$it\" . }" }, imdb?.let { "{ ?work wdt:P345 \"$it\" . }" }).joinToString(" UNION ")
        val query = "SELECT DISTINCT ?work ?id WHERE { $matches VALUES ?prop { $values } ?work ?prop ?id . } LIMIT 12"
        val url = URL("https://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8"))
        var connection: HttpURLConnection? = null
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/sparql-results+json")
            connection.setRequestProperty("User-Agent", "CINEFRAN/0.3 (https://github.com/FPerezGH/bambi)")
            if (connection.responseCode != 200) return@withContext emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val rows = JSONObject(body).getJSONObject("results").getJSONArray("bindings")
            val works = mutableSetOf<String>()
            val ids = linkedSetOf<String>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                works += row.getJSONObject("work").getString("value")
                val id = row.getJSONObject("id").getString("value")
                if (links(platform, id, PlatformOpening.DETAILS, item.type == MediaType.TV).isNotEmpty()) ids += id
            }
            val result = if (works.size == 1) ids.toList() else emptyList()
            prefs.edit().putString(cacheKey, result.joinToString("|")).putLong("$cacheKey:time", System.currentTimeMillis()).apply()
            result
        } catch (e: Exception) {
            Log.w(TAG, "Public title mapping unavailable for $platform")
            emptyList()
        } finally { connection?.disconnect() }
    }

    suspend fun open(context: Context, platform: String, item: CatalogItem, availabilityUrl: String?, mode: PlatformOpening): String? {
        val pm = context.packageManager
        val packages = StreamingPlatforms.packages(platform)
        val prefs = context.applicationContext.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
        fun record(method: String) {
            prefs.edit().putString("last:$platform", method).apply()
            Log.i(TAG, "$platform: $method")
        }
        fun launch(intent: Intent, method: String): Boolean = try {
            if (intent.resolveActivity(pm) == null) false else {
                context.startActivity(intent)
                record(method)
                true
            }
        } catch (_: Exception) { false }
        if (mode != PlatformOpening.APP) {
            val ids = resolve(context, platform, item)
            for (id in ids) {
                for (url in links(platform, id, mode, item.type == MediaType.TV)) {
                    for (pkg in packages) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (platform == "netflix") intent.putExtra("source", "30")
                        if (launch(intent, "title-link")) return null
                    }
                }
            }
            for (pkg in packages) {
                if (launch(Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(SearchManager.QUERY, item.title), "app-search")) return null
            }
        }
        for (pkg in packages) {
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (intent != null && launch(intent, "app-home")) return if (mode == PlatformOpening.APP) null
                else "La plataforma se ha abierto, pero no acepta un enlace directo para este título."
        }
        val uri = availabilityUrl?.let(Uri::parse)
        if (mode != PlatformOpening.APP && uri?.scheme == "https" && uri.host in setOf("www.themoviedb.org", "themoviedb.org")) {
            if (launch(Intent(Intent.ACTION_VIEW, uri), "availability-page")) return null
        }
        record("unavailable")
        return "No se pudo abrir la plataforma. Comprueba que esté instalada."
    }
}
