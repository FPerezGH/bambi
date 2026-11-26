package com.shieldstream.tv.model

/** Metadata for the currently focused title; unknown fields are not fabricated. */
data class TitlePreview(
    val kind: String? = null,
    val genres: List<String> = emptyList(),
    val runtimeMinutes: Int? = null,
    val certification: String? = null,
    val providers: List<OfficialProvider> = emptyList(),
    val availabilityKnown: Boolean = false,
)
