package com.shieldstream.tv.model

enum class FeaturedBadge {
    NEW,
    POPULAR,
    SERIES_NOW,
    MOVIE_NOW,
    RISING,
    ACCLAIMED,
    BUZZ,
    UPCOMING,
    WEEKEND,
    RELAX,
    ADRENALINE,
    THINK,
    SHORT,
    MINISERIES,
    FAMILY,
    HIDDEN_GEM,
    CLASSIC,
    BECAUSE_WATCHED,
}

data class FeaturedItem(
    val item: CatalogItem,
    val badge: FeaturedBadge,
    val logoUrl: String?,
    val genre: String?,
    val durationText: String?,
    val certification: String?,
    /** Clave de YouTube del tráiler que se reproduce en la portada. `null` = solo arte estático. */
    val trailerKey: String? = null,
    val reasonTitle: String? = null,
)

enum class Category(val forMovies: Boolean = true, val forTv: Boolean = true) {
    ACCLAIMED,
    CLASSICS(forTv = false),
    DRAMAS,
    THRILLERS,
    HORROR,
    MOODS,
    CRIME,
    SPANISH,
    ACTION,
    ADVENTURE,
    BOOKS,
    COMEDY,
    HALLOWEEN,
    REALITY(forMovies = false),
    FANTASY,
    SCIFI,
    MYSTERY,
    ANIMATION,
    KIDS,
    ROMANCE,
    FAMILY,
    ANIME,
    KOREAN,
    DOCUMENTARY,
    TRUE_CRIME,
    WAR_HISTORY,
    WESTERN,
    MUSIC(forTv = false),
    ASTROLOGY,
    STANDUP,
    ;

    companion object {
        val DEFAULT_VISIBLE = setOf(ACCLAIMED, CLASSICS, DRAMAS, SPANISH, ACTION, COMEDY, SCIFI, KIDS, FAMILY, DOCUMENTARY)
        val DEFAULT_HIDDEN: Set<String> = entries.filterNot { it in DEFAULT_VISIBLE }.map { it.name }.toSet()

        val KIDS_SAFE = setOf(KIDS, FAMILY)

        fun forFilter(filter: HomeFilter): List<Category> = entries.filter {
            when (filter) {
                HomeFilter.ALL -> true
                HomeFilter.MOVIES -> it.forMovies
                HomeFilter.TV -> it.forTv
            }
        }
    }
}

/** Local Monday-based week identity, shared by the disk and memory caches. */
fun featuredWeek(): String {
    val c = java.util.Calendar.getInstance()
    c.firstDayOfWeek = java.util.Calendar.MONDAY
    c.minimalDaysInFirstWeek = 4
    val week = c.get(java.util.Calendar.WEEK_OF_YEAR)
    val month = c.get(java.util.Calendar.MONTH)
    val year = c.get(java.util.Calendar.YEAR) + when {
        month == java.util.Calendar.JANUARY && week >= 52 -> -1
        month == java.util.Calendar.DECEMBER && week == 1 -> 1
        else -> 0
    }
    return "$year-W$week"
}
