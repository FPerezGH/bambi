package com.shieldstream.tv.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TomatometerSourceTest {
    @Test fun parsesOnlyPercentages() {
        assertEquals(94, TomatometerSource.parseScore("94%"))
        assertEquals(0, TomatometerSource.parseScore("0%"))
        assertEquals(100, TomatometerSource.parseScore("100"))
        assertNull(TomatometerSource.parseScore("8.7/10"))
        assertNull(TomatometerSource.parseScore("101%"))
        assertNull(TomatometerSource.parseScore("N/A"))
    }
}
