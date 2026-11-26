package com.shieldstream.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.watchedCount
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun rememberWatchedCount(item: CatalogItem): Int {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        runCatching { EntryPointAccessors.fromApplication(context, TitlePreviewEntryPoint::class.java).watchProgressRepository() }.getOrNull()
    }
    val count = remember(repository, item.key) {
        repository?.progress?.map { item.watchedCount(it) }?.distinctUntilChanged() ?: flowOf(0)
    }
    val watched by count.collectAsState(initial = repository?.progress?.value?.let { item.watchedCount(it) } ?: 0)
    return watched
}

/** Display actual watched history, never infer that a series is completed. */
@Composable
fun WatchedBadge(count: Int, series: Boolean, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (count <= 0) return
    val label = if (!series) "Visto" else if (count == 1) "1 episodio visto" else "$count episodios vistos"
    Row(modifier.background(Color(0xE61B422B), RoundedCornerShape(5.dp))
        .semantics { contentDescription = label }.padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (compact) { if (series) "✓ $count" else "✓" } else "✓ $label",
            color = Color(0xFF9AF5AD), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
