package com.shieldstream.tv.home

import com.shieldstream.tv.strings.R as StringsR
import com.shieldstream.tv.strings.localizedStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.ui.component.AppIcons
import com.shieldstream.tv.ui.component.PosterCard
import com.shieldstream.tv.ui.component.tvClickable
import kotlinx.coroutines.delay
import com.shieldstream.tv.ui.component.LineIcons
import com.shieldstream.tv.ui.component.ActionMenuItem
import com.shieldstream.tv.ui.component.ActionMenuDialog
import androidx.compose.foundation.lazy.grid.GridItemSpan

private const val KEYS = "abcdefghijklmnopqrstuvwxyz1234567890"
private const val KEY_COLUMNS = 6

private val QWERTY_ROWS = listOf("1234567890", "qwertyuiop", "asdfghjklñ", "zxcvbnm")

@Composable
fun SearchScreen(
    onItemClick: (CatalogItem) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val configuredPageSize by viewModel.pageSize.collectAsStateWithLifecycle()
    var orderMode by rememberSaveable { mutableStateOf("rating") }
    var carouselMode by rememberSaveable { mutableStateOf(false) }
    var orderYear by rememberSaveable { mutableStateOf(0) }
    var page by rememberSaveable(query, orderMode, orderYear, configuredPageSize) { mutableStateOf(0) }
    var lockedPick by remember { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(3_500)
            viewModel.consumeNotice()
        }
    }
    lockedPick?.let { item ->
        ActionMenuDialog(
            title = item.title,
            onDismiss = { lockedPick = null },
            actions = listOf(
                ActionMenuItem(localizedStringResource(StringsR.string.home_cancel), LineIcons.Close) { lockedPick = null },
                ActionMenuItem(localizedStringResource(StringsR.string.home_ask_an_adult_action), AppIcons.Lock) {
                    viewModel.requestTitle(item)
                    lockedPick = null
                },
            ),
        )
    }
    val qwerty by viewModel.qwerty.collectAsStateWithLifecycle()

    val firstKey = remember { FocusRequester() }
    val layoutKey = remember { FocusRequester() }
    val cardRequester = remember { FocusRequester() }
    var lastCardKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        val backOnCard = lastCardKey != null && run {
            repeat(4) {
                if (runCatching { cardRequester.requestFocus() }.isSuccess) return@run true
                delay(60)
            }
            false
        }
        if (!backOnCard) runCatching { firstKey.requestFocus() }
    }

    val keyboard = LocalSoftwareKeyboardController.current
    val fieldRequester = remember { FocusRequester() }
    var systemKeyboard by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query, systemKeyboard) {
        if (!systemKeyboard && field.text != query) field = TextFieldValue(query, TextRange(query.length))
    }
    LaunchedEffect(systemKeyboard) {
        if (systemKeyboard) {
            withFrameNanos { }
            runCatching { fieldRequester.requestFocus() }
            keyboard?.show()
        }
    }
    fun closeSystemKeyboard() {
        keyboard?.hide()
        systemKeyboard = false
        runCatching { layoutKey.requestFocus() }
    }

    Row(Modifier.fillMaxSize().background(Color.Black)) {
        if (!carouselMode) Column(
            Modifier
                .width(if (qwerty) 452.dp else 330.dp)
                .fillMaxHeight()
                .padding(start = 40.dp, top = 40.dp, end = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1C1C1F))
                    .then(if (systemKeyboard) Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            viewModel.setQuery(it.text.replace("\n", ""))
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { closeSystemKeyboard() }, onDone = { closeSystemKeyboard() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fieldRequester)
                            .focusProperties { canFocus = systemKeyboard }
                            .onPreviewKeyEvent { event ->
                                val exit = event.key == Key.DirectionDown || event.key == Key.Back || event.key == Key.Escape
                                if (!exit) return@onPreviewKeyEvent false
                                if (event.type == KeyEventType.KeyUp) closeSystemKeyboard()
                                true
                            }
                            .onFocusChanged { if (!it.isFocused && systemKeyboard) systemKeyboard = false },
                    )
                    if (query.isEmpty()) {
                        Text(
                            text = localizedStringResource(StringsR.string.home_search),
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OnScreenKeyboard(
                qwerty = qwerty,
                firstKeyRequester = firstKey,
                layoutKeyRequester = layoutKey,
                onChar = viewModel::type,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clear,
                onToggleLayout = viewModel::toggleKeyboardLayout,
                onSystemKeyboard = { systemKeyboard = true },
            )
        }

        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 40.dp, end = 24.dp)) {
            val header = when (val s = state) {
                SearchUiState.Loading -> localizedStringResource(StringsR.string.home_searching)
                is SearchUiState.Suggestions -> localizedStringResource(StringsR.string.home_most_watched_today)
                is SearchUiState.Results -> localizedStringResource(StringsR.string.home_search_results_for, s.query)
                is SearchUiState.Empty -> localizedStringResource(StringsR.string.home_no_results_for, s.query)
                is SearchUiState.Error -> s.message
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = header,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                KeyButton(width = 94.dp, onClick = { carouselMode = !carouselMode }) {
                    Text(if (carouselMode) "Cuadrícula" else "Carrusel", color = it, style = MaterialTheme.typography.bodySmall)
                }
                CatalogOrderControl(orderMode, orderYear, { orderMode = it }, { orderYear = it })
            }
            val originalItems = when (val s = state) {
                is SearchUiState.Suggestions -> s.items
                is SearchUiState.Results -> s.items
                else -> emptyList()
            }
            val sortedItems = remember(originalItems, orderMode, orderYear) {
                orderCatalogItems(originalItems, orderMode, orderYear)
            }
            val allItems = sortedItems.take(500)
            if (originalItems.isNotEmpty() && allItems.isEmpty()) {
                Text("No hay títulos para el año seleccionado", color = Color.White,
                    modifier = Modifier.padding(12.dp))
            }
            val results = state as? SearchUiState.Results
            val sortedLocked = orderCatalogItems(results?.locked.orEmpty(), orderMode, orderYear)
            val allLocked = sortedLocked.take((500 - allItems.size).coerceAtLeast(0))
            val pending = results?.pending.orEmpty()
            val pageSize = configuredPageSize.coerceIn(10, 100)
            val total = allItems.size + allLocked.size
            if (sortedItems.size + sortedLocked.size > 500) {
                Text("Mostrando los primeros 500 resultados según el orden seleccionado", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
            }
            val pages = ((total + pageSize - 1) / pageSize).coerceAtLeast(1)
            val currentPage = page.coerceIn(0, pages - 1)
            val start = currentPage * pageSize
            val end = (start + pageSize).coerceAtMost(total)
            val items = allItems.drop(start).take(pageSize)
            val locked = allLocked.drop((start - allItems.size).coerceAtLeast(0))
                .take((end - maxOf(start, allItems.size)).coerceAtLeast(0))
            val gridState = rememberLazyGridState()
            fun changePage(next: Int) {
                page = next.coerceIn(0, pages - 1)
                lastCardKey = null
            }
            LaunchedEffect(currentPage) {
                gridState.scrollToItem(0)
            }
            if (pages > 1) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (currentPage > 0) KeyButton(width = 136.dp, onClick = { changePage(currentPage - 1) }) {
                        Text("‹ Anterior", color = it)
                    }
                    Text("Página ${currentPage + 1}/$pages · ${start + 1}–$end de $total",
                        color = Color.White, style = MaterialTheme.typography.bodySmall)
                    if (currentPage < pages - 1) KeyButton(width = 136.dp, onClick = { changePage(currentPage + 1) }) {
                        Text("Siguiente ›", color = it)
                    }
                }
            }
            notice?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            if (carouselMode && items.isNotEmpty() && locked.isEmpty()) {
                val rows = remember(items) {
                    listOf(com.shieldstream.tv.model.CatalogRow("search_page", "Resultados", items))
                }
                Box(Modifier.fillMaxSize()) {
                    androidx.compose.runtime.key(currentPage, query, orderMode, orderYear) {
                        RowsBrowserContent(
                            rows = rows,
                            onItemClick = onItemClick,
                            onRemoveContinue = {},
                            onRemoveMyList = {},
                            onExitTop = { carouselMode = false },
                            onBack = { carouselMode = false },
                        )
                    }
                }
            } else LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(118.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.key }) { item ->
                    PosterCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onFocused = { lastCardKey = item.key },
                        width = 118.dp,
                        modifier = if (item.key == lastCardKey) Modifier.focusRequester(cardRequester) else Modifier,
                    )
                }
                if (locked.isNotEmpty()) {
                    item(key = "locked_header", span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(
                                localizedStringResource(StringsR.string.home_ask_an_adult),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                localizedStringResource(StringsR.string.home_ask_an_adult_hint),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    items(locked, key = { "locked_" + it.key }) { item ->
                        val asked = item.key in pending
                        PosterCard(
                            item = item,
                            onClick = { if (!asked) lockedPick = item },
                            onFocused = { lastCardKey = "locked_" + item.key },
                            width = 118.dp,
                            modifier = if ("locked_" + item.key == lastCardKey) Modifier.focusRequester(cardRequester) else Modifier,
                            overlay = { LockedOverlay(asked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnScreenKeyboard(
    qwerty: Boolean,
    firstKeyRequester: FocusRequester,
    layoutKeyRequester: FocusRequester,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleLayout: () -> Unit,
    onSystemKeyboard: () -> Unit,
) {
    var toggled by remember { mutableStateOf(false) }
    LaunchedEffect(qwerty) {
        if (toggled) {
            withFrameNanos { }
            runCatching { layoutKeyRequester.requestFocus() }
        }
    }
    val rows = if (qwerty) QWERTY_ROWS else KEYS.chunked(KEY_COLUMNS)
    val keyWidth = if (qwerty) 36.dp else 42.dp
    val fullWidth = if (qwerty) 36.dp * 10 + 4.dp * 9 else 42.dp * 6 + 4.dp * 5
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIndex, rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowKeys.forEachIndexed { colIndex, c ->
                    val mod = if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(firstKeyRequester) else Modifier
                    KeyButton(width = keyWidth, onClick = { onChar(c) }, modifier = mod) {
                        Text(c.uppercase(), style = MaterialTheme.typography.titleMedium, color = it)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(width = fullWidth - 42.dp - 88.dp - 8.dp, onClick = { onChar(' ') }) {
                Text(localizedStringResource(StringsR.string.home_space), style = MaterialTheme.typography.bodyMedium, color = it)
            }
            KeyButton(width = 42.dp, onClick = onBackspace) {
                Icon(AppIcons.Backspace, contentDescription = localizedStringResource(StringsR.string.home_clear), tint = it, modifier = Modifier.size(20.dp))
            }
            KeyButton(width = 88.dp, onClick = onClear) {
                Text(localizedStringResource(StringsR.string.home_clear), style = MaterialTheme.typography.bodyMedium, color = it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                width = 88.dp,
                onClick = {
                    toggled = true
                    onToggleLayout()
                },
                modifier = Modifier.focusRequester(layoutKeyRequester),
            ) {
                Text(if (qwerty) "ABC" else "QWERTY", style = MaterialTheme.typography.bodyMedium, color = it)
            }
            KeyButton(width = fullWidth - 88.dp - 4.dp, onClick = onSystemKeyboard) {
                Text(localizedStringResource(StringsR.string.home_system_keyboard), style = MaterialTheme.typography.bodyMedium, color = it, maxLines = 1)
            }
        }
    }
}

@Composable
private fun KeyButton(
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .width(width)
            .height(40.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(4.dp))
            .background(if (focused) Color.White else Color(0xFF1C1C1F))
            .then(if (focused) Modifier else Modifier.border(1.dp, Color(0xFF2C2C30), RoundedCornerShape(4.dp)))
            .tvClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(if (focused) Color.Black else Color.White)
    }
}

/** Candado sobre la carátula: el título no es infantil; si ya se pidió, lo indica. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.LockedOverlay(asked: Boolean) {
    Box(
        Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.55f)),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.align(Alignment.Center).padding(8.dp),
    ) {
        Icon(if (asked) AppIcons.Check else AppIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = localizedStringResource(if (asked) StringsR.string.home_requested else StringsR.string.home_locked),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}
