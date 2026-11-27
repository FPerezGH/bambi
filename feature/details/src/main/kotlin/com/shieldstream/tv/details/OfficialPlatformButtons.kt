package com.shieldstream.tv.details

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shieldstream.tv.model.PlatformAvailability
import com.shieldstream.tv.model.PlatformOpening
import com.shieldstream.tv.model.CatalogItem
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.shieldstream.tv.ui.component.tvClickable

internal suspend fun openOfficialPlatform(
    context: Context, platform: String, item: CatalogItem, availabilityUrl: String?,
    mode: PlatformOpening = PlatformOpening.PLAY,
    episode: com.shieldstream.tv.model.Episode? = null,
): String? = OfficialPlatformLauncher.open(context, platform, item, availabilityUrl, mode, episode)

@Composable
internal fun OfficialPlatformButtons(platforms: List<PlatformAvailability>, item: CatalogItem, interactive: Boolean, mode: PlatformOpening, episode: com.shieldstream.tv.model.Episode? = null) {
    if (platforms.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var opening by remember(item.key) { mutableStateOf(false) }
    var message by remember(item.key) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            for (platform in platforms) {
                var focused by remember(platform.key) { mutableStateOf(false) }
                val enabled = platform.available && interactive && !opening
                val description = platform.name + when {
                    platform.available -> ": disponible"
                    !platform.availabilityKnown -> ": disponibilidad desconocida"
                    else -> ": no disponible"
                }
                val shape = RoundedCornerShape(8.dp)
                Box(
                    modifier = Modifier.size(52.dp).clip(shape)
                        .background(if (focused) Color.White else Color.White.copy(alpha = 0.10f))
                        .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.20f), shape)
                        .semantics { contentDescription = description }
                        .then(if (enabled) Modifier.onFocusChanged { focused = it.isFocused }.tvClickable {
                            scope.launch {
                                opening = true
                                try { message = openOfficialPlatform(context, platform.key, item, platform.availabilityUrl, mode, episode) }
                                finally { opening = false }
                            }
                        } else Modifier)
                        .alpha(if (platform.available) 1f else 0.30f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (platform.logoUrl != null) {
                        AsyncImage(model = platform.logoUrl, contentDescription = null,
                            contentScale = ContentScale.Fit, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(5.dp)))
                    } else {
                        Text(platform.name, color = if (focused) Color.Black else Color.White,
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }
                }
            }
        }
        if (platforms.any { !it.availabilityKnown }) {
            Text("No se ha podido verificar la disponibilidad.", color = Grey, style = MaterialTheme.typography.labelSmall)
        }
        if (opening) Text("Buscando enlace del título…", color = Grey, style = MaterialTheme.typography.labelSmall)
        message?.let { Text(it, color = Grey, style = MaterialTheme.typography.bodySmall) }
    }
}
