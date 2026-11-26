package com.shieldstream.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shieldstream.tv.domain.CatalogRepository
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.MediaType
import com.shieldstream.tv.model.TitlePreview
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TitlePreviewEntryPoint {
    fun catalogRepository(): CatalogRepository
}

/** Load only the currently focused title, never every poster in a carousel. */
@Composable
fun TitlePreviewMetadata(item: CatalogItem, modifier: Modifier = Modifier, fallbackGenre: String? = null,
                         fallbackDuration: String? = null, fallbackCertification: String? = null) {
    val context = LocalContext.current.applicationContext
    val catalog = remember(context) {
        runCatching { EntryPointAccessors.fromApplication(context, TitlePreviewEntryPoint::class.java).catalogRepository() }.getOrNull()
    }
    val preview by produceState<TitlePreview?>(null, item.key, catalog) {
        value = null
        delay(350)
        value = try { catalog?.getPreview(item) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }
    val info = preview
    val kind = info?.kind ?: if (item.type == MediaType.TV) "Serie" else "Película"
    val genres = info?.genres.orEmpty().ifEmpty { item.genres.ifEmpty { listOfNotNull(fallbackGenre) } }
    val duration = info?.runtimeMinutes?.let { minutes ->
        if (item.type == MediaType.TV) "$minutes min/ep." else if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
    } ?: fallbackDuration
    val certification = info?.certification ?: fallbackCertification
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(listOfNotNull(kind, genres.takeIf { it.isNotEmpty() }?.joinToString(", "), item.year?.toString(), duration)
            .joinToString(" · "), color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            certification?.let { cert ->
                Text(cert, color = Color.White, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.background(Color(0xFF444444), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            item.rating?.takeIf { it > 0f }?.let { rating ->
                Text("★ TMDB " + String.format(java.util.Locale("es", "ES"), "%.1f", rating), color = Color(0xFFFFC94A), style = MaterialTheme.typography.labelLarge)
            }
            TomatometerRating(item)
            if (info != null && info.availabilityKnown) {
                val providers = info.providers.distinctBy { it.name }.take(8)
                if (providers.isEmpty()) {
                    AsyncImage(model = "https://raw.githubusercontent.com/Stremio/stremio-brand/master/favicons/stremio-favicon-96px.png",
                        contentDescription = "Consultar fuentes Stremio; disponibilidad no comprobada", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                    Text("Consultar fuentes", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                } else {
                    providers.forEach { provider ->
                        if (provider.logoUrl != null) AsyncImage(model = provider.logoUrl, contentDescription = provider.name,
                            contentScale = ContentScale.Fit, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)))
                        else Text(provider.name, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    if (info.providers.size > providers.size) Text("+${info.providers.size - providers.size}", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
