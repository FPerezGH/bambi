package com.shieldstream.tv.details

import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.Episode
import com.shieldstream.tv.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class StremioLauncherTest {
    private val movie = CatalogItem("1", "tt1234567", MediaType.MOVIE, "Título de prueba", "", null, null, 2024, null)
    @Test fun movieLink() {
        assertEquals("stremio:///detail/movie/tt1234567/tt1234567?autoPlay=false", StremioLauncher.link(movie, null))
    }
    @Test fun showLink() {
        assertEquals("stremio:///detail/series/tt1234567", StremioLauncher.link(movie.copy(type = MediaType.TV), null))
    }
    @Test fun episodeLink() {
        val episode = Episode(2, 3, "", "", null, null)
        assertEquals("stremio:///detail/series/tt1234567/tt1234567:2:3?autoPlay=false", StremioLauncher.link(movie.copy(type = MediaType.TV), episode))
    }
    @Test fun missingImdbUsesEncodedSearch() {
        assertEquals("stremio:///search?search=T%C3%ADtulo%20de%20prueba", StremioLauncher.link(movie.copy(imdbId = null), null))
    }
}
