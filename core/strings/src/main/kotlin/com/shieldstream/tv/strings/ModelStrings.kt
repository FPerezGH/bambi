package com.shieldstream.tv.strings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.shieldstream.tv.model.Category
import com.shieldstream.tv.model.Episode
import com.shieldstream.tv.model.FeaturedBadge
import com.shieldstream.tv.model.SubtitleColor

@get:StringRes
val Category.titleResource: Int
    get() = when (this) {
        Category.ACCLAIMED -> R.string.category_acclaimed
        Category.CLASSICS -> R.string.category_classics
        Category.DRAMAS -> R.string.category_dramas
        Category.THRILLERS -> R.string.category_thrillers
        Category.HORROR -> R.string.category_horror
        Category.MOODS -> R.string.category_moods
        Category.CRIME -> R.string.category_crime
        Category.SPANISH -> R.string.category_spanish
        Category.ACTION -> R.string.category_action
        Category.ADVENTURE -> R.string.category_adventure
        Category.BOOKS -> R.string.category_books
        Category.COMEDY -> R.string.category_comedy
        Category.HALLOWEEN -> R.string.category_halloween
        Category.REALITY -> R.string.category_reality
        Category.FANTASY -> R.string.category_fantasy
        Category.SCIFI -> R.string.category_scifi
        Category.MYSTERY -> R.string.category_mystery
        Category.ANIMATION -> R.string.category_animation
        Category.KIDS -> R.string.category_kids
        Category.ROMANCE -> R.string.category_romance
        Category.FAMILY -> R.string.category_family
        Category.ANIME -> R.string.category_anime
        Category.KOREAN -> R.string.category_korean
        Category.DOCUMENTARY -> R.string.category_documentary
        Category.TRUE_CRIME -> R.string.category_true_crime
        Category.WAR_HISTORY -> R.string.category_war_history
        Category.WESTERN -> R.string.category_western
        Category.MUSIC -> R.string.category_music
        Category.ASTROLOGY -> R.string.category_astrology
        Category.STANDUP -> R.string.category_standup
    }

@get:StringRes
val FeaturedBadge.labelResource: Int
    get() = when (this) {
        FeaturedBadge.NEW -> R.string.featured_new
        FeaturedBadge.POPULAR -> R.string.featured_popular
        FeaturedBadge.SERIES_NOW -> R.string.featured_series_now
        FeaturedBadge.MOVIE_NOW -> R.string.featured_movie_now
        FeaturedBadge.RISING -> R.string.featured_rising
        FeaturedBadge.ACCLAIMED -> R.string.featured_acclaimed
        FeaturedBadge.BUZZ -> R.string.featured_buzz
        FeaturedBadge.UPCOMING -> R.string.featured_upcoming
        FeaturedBadge.WEEKEND -> R.string.featured_weekend
        FeaturedBadge.RELAX -> R.string.featured_relax
        FeaturedBadge.ADRENALINE -> R.string.featured_adrenaline
        FeaturedBadge.THINK -> R.string.featured_think
        FeaturedBadge.SHORT -> R.string.featured_short
        FeaturedBadge.MINISERIES -> R.string.featured_miniseries
        FeaturedBadge.FAMILY -> R.string.featured_family
        FeaturedBadge.HIDDEN_GEM -> R.string.featured_hidden_gem
        FeaturedBadge.CLASSIC -> R.string.featured_classic
        FeaturedBadge.BECAUSE_WATCHED -> R.string.featured_because_watched
        FeaturedBadge.AUDIENCE_FAVORITE -> R.string.featured_audience_favorite
        FeaturedBadge.SPANISH_CINEMA -> R.string.featured_spanish_cinema
        FeaturedBadge.ON_YOUR_PLATFORMS -> R.string.featured_on_your_platforms
    }

@get:StringRes
val SubtitleColor.labelResource: Int
    get() = when (this) {
        SubtitleColor.WHITE -> R.string.subtitle_color_white
        SubtitleColor.YELLOW -> R.string.subtitle_color_yellow
        SubtitleColor.GREEN -> R.string.subtitle_color_green
        SubtitleColor.CYAN -> R.string.subtitle_color_cyan
        SubtitleColor.RED -> R.string.subtitle_color_red
        SubtitleColor.BLACK -> R.string.subtitle_color_black
        SubtitleColor.TRANSPARENT -> R.string.subtitle_color_transparent
        SubtitleColor.SHADE -> R.string.subtitle_color_shade
    }

@get:StringRes
val com.shieldstream.tv.model.SubtitleFont.labelResource: Int
    get() = when (this) {
        com.shieldstream.tv.model.SubtitleFont.DEFAULT -> R.string.subtitle_font_default
        com.shieldstream.tv.model.SubtitleFont.SERIF -> R.string.subtitle_font_serif
        com.shieldstream.tv.model.SubtitleFont.MONOSPACE -> R.string.subtitle_font_mono
        com.shieldstream.tv.model.SubtitleFont.CONDENSED -> R.string.subtitle_font_condensed
        com.shieldstream.tv.model.SubtitleFont.BOLD -> R.string.subtitle_font_bold
    }

@get:StringRes
val com.shieldstream.tv.model.SubtitleEdge.labelResource: Int
    get() = when (this) {
        com.shieldstream.tv.model.SubtitleEdge.OUTLINE -> R.string.subtitle_edge_outline
        com.shieldstream.tv.model.SubtitleEdge.SHADOW -> R.string.subtitle_edge_shadow
    }

@Composable
fun Category.localizedTitle(): String = localizedStringResource(titleResource)

fun Texts.categoryTitle(category: Category): String = get(category.titleResource)

@Composable
fun FeaturedBadge.localizedLabel(): String = localizedStringResource(labelResource)

@Composable
fun Episode.localizedCode(): String = localizedStringResource(R.string.episode_code, season, number)

fun Texts.episodeCode(episode: Episode): String = get(R.string.episode_code, episode.season, episode.number)

@Composable
fun localizedSubtitleLanguage(code: String): String = subtitleLanguageResource(code)?.let {
    localizedStringResource(it)
} ?: code.uppercase()

fun Texts.subtitleLanguage(code: String): String = subtitleLanguageResource(code)?.let { get(it) } ?: code.uppercase()

@StringRes
private fun subtitleLanguageResource(code: String): Int? = when (code) {
    "es" -> R.string.language_spanish
    "en" -> R.string.language_english
    "ca" -> R.string.language_catalan
    "fr" -> R.string.language_french
    "it" -> R.string.language_italian
    "pt" -> R.string.language_portuguese
    "de" -> R.string.language_german
    else -> null
}
