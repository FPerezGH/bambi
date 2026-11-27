package com.shieldstream.tv.model

/** Stable section IDs shared by settings and the home catalog. */
object HomeSections {
    val labels = linkedMapOf(
        "continue_watching" to "Seguir viendo", "up_next" to "A continuación",
        "new_episodes" to "Nuevos episodios", "my_list" to "Mi lista",
        "recommended_for_you" to "Recomendado para ti", "cinefran_recent" to "Estrenos recientes",
        "top10_movies" to "Top 10 películas", "popular_movies" to "Películas populares",
        "popular_tv" to "Series populares", "top10_tv" to "Top 10 series",
        "now_playing" to "En cines", "action" to "Acción", "scifi" to "Ciencia ficción",
        "comedy" to "Comedia", "top_movies" to "Películas mejor valoradas", "top_tv" to "Series mejor valoradas",
        "m_top10" to "Top 10", "m_popular" to "Populares", "m_now" to "En cines",
        "m_action" to "Acción", "m_scifi" to "Ciencia ficción", "m_thriller" to "Thriller",
        "m_horror" to "Terror", "m_comedy" to "Comedia", "m_animation" to "Animación", "m_top" to "Mejor valoradas",
        "t_top10" to "Top 10", "t_popular" to "Populares", "t_drama" to "Drama",
        "t_crime" to "Crimen", "t_scifi" to "Ciencia ficción y fantasía", "t_action" to "Acción y aventura",
        "t_comedy" to "Comedia", "t_animation" to "Animación", "t_top" to "Mejor valoradas",
    )
    fun ids(filter: HomeFilter): List<String> = labels.keys.filter {
        when (filter) {
            HomeFilter.ALL -> !it.startsWith("m_") && !it.startsWith("t_")
            HomeFilter.MOVIES -> it.startsWith("m_") || it in shared
            HomeFilter.TV -> it.startsWith("t_") || it in shared
        }
    }
    private val shared = setOf("continue_watching", "up_next", "new_episodes", "my_list", "recommended_for_you", "cinefran_recent")
    fun key(filter: HomeFilter, id: String) = "${filter.name}:$id"
    fun apply(rows: List<CatalogRow>, profile: Profile, filter: HomeFilter): List<CatalogRow> {
        if (profile.kids) return rows
        val order = profile.homeRowOrder[filter.name].orEmpty()
        return rows.filterNot { key(filter, it.id) in profile.hiddenHomeRows || it.id in profile.hiddenHomeRows }
            .sortedBy { order.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
    }
}

enum class PlatformOpening { PLAY, DETAILS, APP }

object StreamingPlatforms {
    fun packages(key: String): List<String> = when (key) {
        "netflix" -> listOf("com.netflix.ninja")
        "prime" -> listOf("com.amazon.amazonvideo.livingroom")
        "max" -> listOf("com.wbd.stream", "com.wbd.hbomax", "com.hbo.hbonow")
        "movistar" -> listOf("es.plus.yomvi")
        "vodafone" -> listOf("com.vodafone.vtv")
        else -> emptyList()
    }
    val labels = linkedMapOf("netflix" to "Netflix", "prime" to "Prime Video", "max" to "HBO Max",
        "movistar" to "Movistar Plus+", "vodafone" to "Vodafone TV")
    val defaults: Set<String> get() = labels.keys.toSet()
    fun id(name: String): String? = when (val n = name.trim().lowercase(java.util.Locale.ROOT)) {
        "netflix", "netflix standard with ads" -> "netflix"
        "amazon prime video", "prime video", "amazon prime video with ads" -> "prime"
        "hbo max", "max", "hbo max basic with ads", "max with ads" -> "max"
        "vodafone tv" -> "vodafone"
        else -> if (n.startsWith("movistar plus")) "movistar" else null
    }
}
