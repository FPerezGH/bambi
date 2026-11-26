package com.shieldstream.tv.details

// Compatibility for existing details callers and tests. Shared implementation in core:ui.
internal object TomatometerSource {
    fun parseScore(value: String): Int? = com.shieldstream.tv.ui.component.TomatometerSource.parseScore(value)
}
