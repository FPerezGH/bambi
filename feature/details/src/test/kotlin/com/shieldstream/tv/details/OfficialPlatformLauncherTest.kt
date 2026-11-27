package com.shieldstream.tv.details

import com.shieldstream.tv.model.PlatformOpening
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialPlatformLauncherTest {
    @Test fun netflixMoviePlayBeforeDetail() {
        assertEquals(listOf("nflx://www.netflix.com/watch/80057281", "https://www.netflix.com/watch/80057281", "nflx://www.netflix.com/title/80057281", "https://www.netflix.com/title/80057281"),
            OfficialPlatformLauncher.links("netflix", "80057281", PlatformOpening.PLAY, false))
    }
    @Test fun seriesUsesTitlePage() {
        assertEquals(listOf("nflx://www.netflix.com/title/80057281", "https://www.netflix.com/title/80057281"),
            OfficialPlatformLauncher.links("netflix", "80057281", PlatformOpening.PLAY, true))
    }
    @Test fun maxUsesOfficialTitleLink() {
        assertEquals(listOf("https://play.hbomax.com/movie/dfa50804-e6f6-4fa2-a732-693dbc50527b", "https://play.max.com/movie/dfa50804-e6f6-4fa2-a732-693dbc50527b"),
            OfficialPlatformLauncher.links("max", "movie/dfa50804-e6f6-4fa2-a732-693dbc50527b", PlatformOpening.PLAY, false))
    }
    @Test fun movistarExactHostOnly() {
        assertEquals("https://ver.movistarplus.es/ficha/?id=4201526", OfficialPlatformLauncher.officialUrl("movistar", "https://ver.movistarplus.es/ficha/?id=4201526"))
        assertEquals(null, OfficialPlatformLauncher.officialUrl("movistar", "https://www.mediasetinfinity.es/ficha/?id=4201526"))
        assertEquals(null, OfficialPlatformLauncher.officialUrl("movistar", "https://ver.movistarplus.es.evil.test/ficha/?id=4201526"))
        assertEquals(listOf("es.plus.yomvi"), com.shieldstream.tv.model.StreamingPlatforms.packages("movistar"))
    }
    @Test fun primeAcceptsAsin() {
        assertTrue(OfficialPlatformLauncher.links("prime", "B07ZPDN57Q", PlatformOpening.PLAY, false).isNotEmpty())
    }
    @Test fun rejectsUnknownOrUnsafeIds() {
        assertTrue(OfficialPlatformLauncher.links("netflix", "https://example.com", PlatformOpening.PLAY, false).isEmpty())
        assertTrue(OfficialPlatformLauncher.links("movistar", "123456", PlatformOpening.PLAY, false).isEmpty())
    }
}
