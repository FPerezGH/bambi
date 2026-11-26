package com.shieldstream.tv.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchedCountTest {
    private val movie = CatalogItem("1", null, MediaType.MOVIE, "Test", "", null, null, null, null)
    private val done = WatchProgress(100, 100, 0)
    @Test fun movieUsesOnlyItsOwnHistory() {
        assertEquals(1, movie.watchedCount(mapOf("movie:1" to done)))
        assertEquals(0, movie.watchedCount(mapOf("movie:11" to done, "tv:1:1:1" to done)))
        assertEquals(0, movie.watchedCount(mapOf("movie:1" to done.copy(positionMs = 50))))
    }
    @Test fun seriesCountsWatchedEpisodesNotOtherTitles() {
        assertEquals(2, movie.copy(type = MediaType.TV).watchedCount(mapOf(
            "tv:1:1:1" to done, "tv:1:1:2" to done,
            "tv:1:1:3" to done.copy(positionMs = 10), "tv:11:1:1" to done, "movie:1" to done)))
    }
}
