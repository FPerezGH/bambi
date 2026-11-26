package com.shieldstream.tv.details

import com.shieldstream.tv.model.PlatformOpening
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialPlatformLauncherTest {
    @Test fun netflixMoviePlayBeforeDetail() {
        assertEquals(listOf("https://www.netflix.com/watch/80057281", "https://www.netflix.com/title/80057281"),
            OfficialPlatformLauncher.links("netflix", "80057281", PlatformOpening.PLAY, false))
    }
    @Test fun seriesUsesTitlePage() {
        assertEquals(listOf("https://www.netflix.com/title/80057281"),
            OfficialPlatformLauncher.links("netflix", "80057281", PlatformOpening.PLAY, true))
    }
    @Test fun rejectsUnknownOrUnsafeIds() {
        assertTrue(OfficialPlatformLauncher.links("netflix", "https://example.com", PlatformOpening.PLAY, false).isEmpty())
        assertTrue(OfficialPlatformLauncher.links("movistar", "123456", PlatformOpening.PLAY, false).isEmpty())
    }
}
