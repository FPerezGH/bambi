package com.shieldstream.tv.home

import com.shieldstream.tv.model.CatalogItem
import com.shieldstream.tv.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchReturnLocationTest {
    private fun items() = (0..89).map { CatalogItem(it.toString(), null, MediaType.MOVIE, "Title $it", "", null, null, null, null) }
    @Test fun restoresExactPageAndIndex() {
        assertEquals(2 to 7, searchReturnLocation(items(), "MOVIE_67", 30))
        assertEquals(0 to 0, searchReturnLocation(items(), "MOVIE_0", 30))
    }
    @Test fun locatesAfterReordering() {
        assertEquals(0 to 22, searchReturnLocation(items().reversed(), "MOVIE_67", 30))
    }
    @Test fun missingItemDoesNotRestoreUnrelatedTitle() {
        assertNull(searchReturnLocation(items(), "TV_67", 30))
    }
}
