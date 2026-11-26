package com.shieldstream.tv.ui.component

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Public Wikidata review statements, explicitly qualified as the critics' Tomatometer. */
object TomatometerSource {
    private const val TTL = 7L * 86400000L
    fun parseScore(value: String): Int? =
        value.trim().removeSuffix("%").trim().toIntOrNull()?.takeIf { it in 0..100 }

    suspend fun score(context: Context, item: CatalogItem): Int? = withContext(Dispatchers.IO) {
        val id = item.id.toIntOrNull()?.takeIf { it > 0 } ?: return@withContext null
        val prop = if (item.type == MediaType.TV) "P4983" else "P4947"
        val prefs = context.applicationContext.getSharedPreferences("cinefran_tomatometer_v1", Context.MODE_PRIVATE)
        val key = item.key
        val age = System.currentTimeMillis() - prefs.getLong("$key:time", 0)
        if (age in 0 until TTL && prefs.contains(key)) return@withContext prefs.getInt(key, -1).takeIf { it >= 0 }
        val query = "SELECT DISTINCT ?work ?score ?date WHERE { ?work wdt:$prop \"$id\"; p:P444 ?review. " +
            "?review ps:P444 ?score; pq:P447 wd:Q105584; pq:P459 wd:Q108403393. " +
            "FILTER NOT EXISTS { ?review wikibase:rank wikibase:DeprecatedRank } OPTIONAL { ?review pq:P585 ?date } } ORDER BY DESC(?date) LIMIT 30"
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8"))
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/sparql-results+json")
            connection.setRequestProperty("User-Agent", "CINEFRAN/0.3 (https://github.com/FPerezGH/bambi)")
            if (connection.responseCode != 200) return@withContext prefs.getInt(key, -1).takeIf { it >= 0 }
            val rows = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                .getJSONObject("results").getJSONArray("bindings")
            val works = mutableSetOf<String>()
            val scores = mutableListOf<Pair<String, Int>>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                works += row.getJSONObject("work").getString("value")
                val score = parseScore(row.getJSONObject("score").getString("value")) ?: continue
                scores += (row.optJSONObject("date")?.optString("value").orEmpty() to score)
            }
            val latest = scores.maxOfOrNull { it.first }
            val candidates = scores.filter { it.first == latest }.map { it.second }.distinct()
            val score = if (works.size == 1) candidates.singleOrNull() else null
            prefs.edit().putInt(key, score ?: -1).putLong("$key:time", System.currentTimeMillis()).apply()
            score
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { prefs.getInt(key, -1).takeIf { it >= 0 } }
        finally { connection?.disconnect() }
    }
}

@Composable
fun TomatometerRating(item: CatalogItem) {
    val context = LocalContext.current
    val score by produceState<Int?>(null, item.key) {
        value = null
        value = TomatometerSource.score(context, item)
    }
    val rating = score ?: return
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "Rotten Tomatoes, Tomatometer: $rating%" }) {
        Canvas(Modifier.size(21.dp)) {
            val w = size.width
            val h = size.height
            if (rating >= 60) {
                drawOval(Color(0xFFE83B36), Offset(w * .08f, h * .26f), Size(w * .84f, h * .65f))
                drawOval(Color(0xFFFF7770), Offset(w * .22f, h * .39f), Size(w * .19f, h * .12f))
                val leaves = Path().apply {
                    moveTo(w * .50f, h * .31f)
                    lineTo(w * .18f, h * .14f)
                    lineTo(w * .43f, h * .17f)
                    lineTo(w * .51f, h * .04f)
                    lineTo(w * .59f, h * .19f)
                    lineTo(w * .83f, h * .12f)
                    lineTo(w * .65f, h * .35f)
                    close()
                }
                drawPath(leaves, Color(0xFF43A047))
            } else {
                val green = Color(0xFF70B63E)
                drawCircle(green, w * .28f, Offset(w * .48f, h * .53f))
                for ((x, y) in listOf(.17f to .24f, .77f to .15f, .88f to .57f, .22f to .86f, .62f to .87f)) {
                    drawLine(green, Offset(w * .48f, h * .53f), Offset(w * x, h * y), w * .12f)
                    drawCircle(green, w * .09f, Offset(w * x, h * y))
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text("$rating%", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(14.dp))
    }
}
