package com.shieldstream.tv.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.Episode
import com.shieldstream.tv.model.MediaType
import com.shieldstream.tv.ui.component.tvClickable

internal object StremioLauncher {
    const val PACKAGE = "com.stremio.one"
    fun link(item: CatalogItem, episode: Episode?): String {
        val imdb = item.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }
            ?: return "stremio:///search?search=" + java.net.URLEncoder.encode(item.title, "UTF-8").replace("+", "%20")
        return if (item.type == MediaType.TV) {
            if (episode == null) "stremio:///detail/series/$imdb"
            else "stremio:///detail/series/$imdb/$imdb:${episode.season}:${episode.number}?autoPlay=false"
        } else "stremio:///detail/movie/$imdb/$imdb?autoPlay=false"
    }
    fun open(context: Context, item: CatalogItem, episode: Episode?, spanishOnly: Boolean) {
        // The external app owns source selection; never silently bypass the user's language preference.
        if (spanishOnly) Toast.makeText(context, "Solo en español: comprueba los filtros de Stremio antes de reproducir.", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link(item, episode))).setPackage(PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "Instala o actualiza Stremio para Android TV.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "No se ha podido abrir Stremio.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
internal fun StremioPlatformButton(enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(context) { runCatching { context.packageManager.getApplicationIcon(StremioLauncher.PACKAGE) }.getOrNull() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.size(52.dp).clip(shape)
        .background(if (focused) Color.White else Color(0xFF26232D))
        .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0xFF665782), shape)
        .onFocusChanged { focused = it.isFocused }
        .tvClickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        AsyncImage(model = logo ?: "https://raw.githubusercontent.com/Stremio/stremio-brand/master/favicons/stremio-favicon-96px.png",
            contentDescription = "Abrir en Stremio", contentScale = ContentScale.Fit, modifier = Modifier.size(44.dp))
    }
}
