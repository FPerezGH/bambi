package com.shieldstream.tv.details

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.Episode
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
            "prime" -> if (id.matches(Regex("[A-Z0-9]{26}|B[A-Z0-9]{9}|amzn[0-9]\\.dv\\.gti\\.[0-9a-fA-F-]{36}"))) {
                if (id.startsWith("amzn")) listOf("https://app.primevideo.com/detail?gti=$id", "https://www.primevideo.com/detail/$id") else listOf("https://www.primevideo.com/detail/$id")
            } else emptyList()
            "max" -> if (id.matches(Regex("(?:episode|extra|feature|movie|franchise|season|series|show|shows)/(?:urn:hbo:(?:episode|extra|feature|movie|franchise|season|series|show|shows):[A-Za-z0-9_-]{21}|[0-9a-fA-F-]{36})"))) {
                listOf("https://play.hbomax.com/$id", "https://play.max.com/$id")
            } else emptyList()
            else -> emptyList()
        }
    }

    private suspend fun resolve(context: Context, platform: String, item: CatalogItem, episode: Episode?, mode: PlatformOpening): List<String> = withContext(Dispatchers.IO) {
        val tmdb = (if (episode != null) episode.tmdbId else item.id.toIntOrNull())?.takeIf { it > 0 }
        val imdb = item.imdbId?.takeIf { episode == null && it.matches(Regex("tt[0-9]+")) }
        if (tmdb == null && imdb == null) return@withContext emptyList()
        val properties = when (platform) {
            "netflix" -> listOf("P1874", "P973")
            "prime" -> listOf("P14462", "P14440", "P8055", "P973")
            "max" -> listOf("P8298", "P973")
            "movistar" -> listOf("P973")
            else -> return@withContext emptyList()
        }
        val tmdbProp = if (episode != null) "P12559" else if (item.type == MediaType.TV) "P4983" else "P4947"
        val prefs = context.applicationContext.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
        val cacheKey = "v3:$platform:${item.key}:${episode?.season ?: 0}:${episode?.number ?: 0}:${mode.name}"
        val cached = prefs.getString(cacheKey, null)
        if (cached != null && System.currentTimeMillis() - prefs.getLong("$cacheKey:time", 0) < (if (cached.isBlank()) 15 * 60_000L else TTL))
            return@withContext cached.split("|").filter { it.isNotBlank() }
        val values = properties.joinToString(" ") { "wdt:$it" }
        val matches = listOfNotNull(tmdb?.let { "{ ?work wdt:$tmdbProp \"$it\" . }" }, imdb?.let { "{ ?work wdt:P345 \"$it\" . }" }).joinToString(" UNION ")
        val query = "SELECT DISTINCT ?work ?id WHERE { { $matches } VALUES ?prop { $values } ?work ?prop ?id . } LIMIT 100"
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
                val id = row.getJSONObject("id").getString("value")
                val candidates = links(platform, id, mode, episode == null && item.type == MediaType.TV) + listOfNotNull(officialUrl(platform, id))
                if (candidates.isNotEmpty()) {
                    works += row.getJSONObject("work").getString("value")
                    ids.addAll(candidates)
                }
            }
            val result = if (works.size == 1) ids.toList() else emptyList()
            prefs.edit().putString(cacheKey, result.joinToString("|")).putLong("$cacheKey:time", System.currentTimeMillis()).apply()
            result
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            Log.w(TAG, "Public title mapping unavailable for $platform")
            emptyList()
        } finally { connection?.disconnect() }
    }

    internal fun officialUrl(platform: String, url: String): String? {
        val parsed = runCatching { java.net.URI(url) }.getOrNull() ?: return null
        if (parsed.scheme !in setOf("http", "https") || parsed.userInfo != null || parsed.port != -1 || parsed.fragment != null) return null
        val host = parsed.host?.lowercase(java.util.Locale.ROOT) ?: return null
        val path = parsed.path.orEmpty()
        return when (platform) {
            "movistar" -> if (host == "ver.movistarplus.es" && path.trimEnd('/') == "/ficha" && parsed.rawQuery?.matches(Regex("id=[0-9]{1,12}")) == true)
                "https://ver.movistarplus.es/ficha/?" + parsed.rawQuery else null
            "netflix" -> if (host in setOf("www.netflix.com", "netflix.com") && path.matches(Regex("/(?:title|watch)/[0-9]{6,8}"))) "https://www.netflix.com$path" else null
            "prime" -> if (host in setOf("www.primevideo.com", "primevideo.com") && path.matches(Regex("/detail/(?:[A-Z0-9]{26}|B[A-Z0-9]{9}|amzn[0-9]\\.dv\\.gti\\.[0-9a-fA-F-]{36})"))) "https://www.primevideo.com$path" else null
            "max" -> if (host in setOf("play.hbomax.com", "play.max.com") && links("max", path.removePrefix("/"), PlatformOpening.DETAILS, false).isNotEmpty()) "https://$host$path" else null
            else -> null
        }
    }

    suspend fun open(context: Context, platform: String, item: CatalogItem, @Suppress("UNUSED_PARAMETER") availabilityUrl: String?, mode: PlatformOpening, episode: Episode? = null): String? {
        val pm = context.packageManager
        val packages = StreamingPlatforms.packages(platform).filter { pkg ->
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(info).toString()
                info.enabled && !(platform == "movistar" && (name.contains("infinity", true) || name.contains("mitele", true)))
            }.getOrDefault(false)
        }
        val label = StreamingPlatforms.labels[platform] ?: platform
        fun launch(intent: Intent, pkg: String, method: String): Boolean = try {
            intent.setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val target = intent.resolveActivity(pm)
            if (target == null || target.packageName != pkg) false
            else {
                intent.component = target
                context.startActivity(intent)
                Log.i(TAG, "$platform: $method -> $pkg/" + target.className)
                true
            }
        } catch (e: Exception) {
            Log.i(TAG, "$platform: $method rejected by $pkg (" + e.javaClass.simpleName + ")")
            false
        }
        if (packages.isEmpty()) return "$label no está instalada o no es compatible."
        if (mode != PlatformOpening.APP) {
            val targetEpisode = episode?.takeIf { item.type == MediaType.TV && mode == PlatformOpening.PLAY }
            for (url in resolve(context, platform, item, targetEpisode, mode)) for (pkg in packages) {
                if (launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE), pkg, if (targetEpisode == null) "title-link" else "episode-link")) return null
            }
            if (targetEpisode != null) {
                for (url in resolve(context, platform, item, null, PlatformOpening.DETAILS)) for (pkg in packages) {
                    if (launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE), pkg, "series-detail"))
                        return "No hay enlace verificado para ese episodio; se ha abierto la serie en $label."
                }
            }
            for (pkg in packages) {
                if (launch(Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, item.title), pkg, "app-search"))
                    return "No hay enlace directo disponible; se ha abierto la búsqueda en $label."
            }
        }
        for (pkg in packages) {
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (intent != null && launch(intent, pkg, "app-home")) return if (mode == PlatformOpening.APP) null
                else "No hay enlace directo compatible para este título; se ha abierto $label."
        }
        // Do not open TMDB's generic availability page or any unbound intent.
        Log.i(TAG, "$platform: no compatible activity")
        return "No se ha podido abrir $label. Comprueba que esté instalada."
    }
}
