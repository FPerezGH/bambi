package com.shieldstream.tv.settings

import com.shieldstream.tv.strings.R as StringsR
import com.shieldstream.tv.strings.localizedStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shieldstream.tv.model.Category
import com.shieldstream.tv.strings.localizedTitle
import com.shieldstream.tv.ui.component.tvClickable
import kotlinx.coroutines.delay

private val Panel = Color(0xFF1A1C27)
private val PanelFocused = Color(0xFF2A2D3D)
private val Dim = Color.White.copy(alpha = 0.6f)
private val Red = Color(0xFFE50914)

@Composable
internal fun HomeCategoriesDialog(
    hidden: Set<String>,
    onToggle: (Category) -> Unit,
    onShowAll: () -> Unit,
    onClose: () -> Unit,
    order: List<String> = emptyList(),
    onMove: (Category, Int) -> Unit = { _, _ -> },
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(3) {
            runCatching { first.requestFocus() }
            delay(100)
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(900.dp)
                .background(Panel, RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(28.dp),
        ) {
            Text(localizedStringResource(StringsR.string.settings_home_categories), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "OK: mostrar u ocultar · Flechas ↑ ↓: cambiar posición",
                color = Dim,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier.height(300.dp),
            ) {
                itemsIndexed((order + Category.entries.map { it.name }).distinct().mapNotNull { name -> Category.entries.firstOrNull { it.name == name } }, key = { _, c -> c.name }) { index, category ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryToggle(
                        category = category,
                        shown = category.name !in hidden,
                        onClick = { onToggle(category) },
                        modifier = Modifier.weight(1f).then(if (index == 0) Modifier.focusRequester(first) else Modifier),
                    )
                    DialogButton("↑") { onMove(category, -1) }
                    DialogButton("↓") { onMove(category, 1) }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton(localizedStringResource(StringsR.string.settings_show_all), onShowAll)
                DialogButton(localizedStringResource(StringsR.string.settings_done), onClose)
            }
        }
    }
}

@Composable
private fun CategoryToggle(category: Category, shown: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) = PreferenceToggle(category.localizedTitle(), shown, onClick, modifier)

@Composable
private fun PreferenceToggle(label: String, shown: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) PanelFocused else Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .background(if (shown) Red else Color.Transparent, RoundedCornerShape(5.dp))
                .border(2.dp, if (shown) Red else Color.White.copy(alpha = 0.4f), RoundedCornerShape(5.dp)),
        ) {
            if (shown) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (shown) Color.White else Dim,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DialogButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text,
        color = if (focused) Color.Black else Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

@Composable
internal fun HomeRowsPreferencesDialog(
    profile: com.shieldstream.tv.model.Profile,
    onToggle: (String) -> Unit,
    onMove: (com.shieldstream.tv.model.HomeFilter, String, Int) -> Unit,
    onClose: () -> Unit,
) {
    val sections = com.shieldstream.tv.model.HomeSections
    var filter by remember { mutableStateOf(com.shieldstream.tv.model.HomeFilter.ALL) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { first.requestFocus() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(760.dp).background(Panel, RoundedCornerShape(18.dp)).padding(24.dp)) {
            Text("Personalizar carruseles", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (tab in com.shieldstream.tv.model.HomeFilter.entries) {
                    val title = when (tab) {
                        com.shieldstream.tv.model.HomeFilter.ALL -> "Inicio"
                        com.shieldstream.tv.model.HomeFilter.MOVIES -> "Películas"
                        com.shieldstream.tv.model.HomeFilter.TV -> "Series"
                    }
                    DialogButton(if (tab == filter) "✓ $title" else title) { filter = tab }
                }
            }
            val ids = sections.ids(filter)
            val order = (profile.homeRowOrder[filter.name].orEmpty() + ids).distinct().filter { it in ids }
            LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(order, key = { _, id -> id }) { index, id ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val key = sections.key(filter, id)
                        PreferenceToggle(sections.labels[id] ?: id, key !in profile.hiddenHomeRows && id !in profile.hiddenHomeRows,
                            { onToggle(key) }, Modifier.weight(1f).then(if (index == 0) Modifier.focusRequester(first) else Modifier))
                        DialogButton("↑") { onMove(filter, id, -1) }
                        DialogButton("↓") { onMove(filter, id, 1) }
                    }
                }
            }
            DialogButton("Aceptar", onClose)
        }
    }
}

@Composable
internal fun PlatformPreferencesDialog(
    profile: com.shieldstream.tv.model.Profile,
    onToggle: (String) -> Unit,
    onOpening: (com.shieldstream.tv.model.PlatformOpening) -> Unit,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = context.packageManager
    val first = remember { FocusRequester() }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { delay(150); runCatching { first.requestFocus() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(820.dp).background(Panel, RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mis plataformas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Inicia sesión en las aplicaciones oficiales; CINEFRAN no guarda contraseñas.", color = Dim, fontSize = 15.sp)
            LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(com.shieldstream.tv.model.StreamingPlatforms.labels.entries.toList(), key = { _, e -> e.key }) { index, entry ->
                    val intent = com.shieldstream.tv.model.StreamingPlatforms.packages(entry.key).firstNotNullOfOrNull { pkg ->
                        pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreferenceToggle(entry.value, entry.key in profile.streamingPlatforms, { onToggle(entry.key) },
                            Modifier.weight(1f).then(if (index == 0) Modifier.focusRequester(first) else Modifier))
                        Text(if (intent != null) "Instalada" else "No instalada", color = Dim, fontSize = 14.sp)
                        DialogButton("Abrir") {
                            try {
                                if (intent != null) context.startActivity(intent)
                                else message = "Instala " + entry.value + " desde Google Play."
                            } catch (_: Exception) { message = "No se ha podido abrir " + entry.value }
                        }
                    }
                }
            }
            Text("Preferencia de apertura", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((mode, label) in listOf(
                    com.shieldstream.tv.model.PlatformOpening.PLAY to "Reproducir",
                    com.shieldstream.tv.model.PlatformOpening.DETAILS to "Ficha",
                    com.shieldstream.tv.model.PlatformOpening.APP to "Aplicación",
                )) {
                    DialogButton(if (mode == profile.platformOpening) "✓ $label" else label) { onOpening(mode) }
                }
            }
            Text("Stremio conserva sus fuentes originales.", color = Dim, fontSize = 14.sp)
            if (message.isNotEmpty()) Text(message, color = Dim, fontSize = 14.sp)
            DialogButton("Aceptar", onClose)
        }
    }
}
