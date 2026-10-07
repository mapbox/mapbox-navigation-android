package com.mapbox.navigation.ui.androidauto.internal.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class ListExtensionsTest {

    private val items = listOf("a", "b", "c", "d")

    @Test
    fun `takeKeeping returns every item within the limit`() {
        assertEquals(items, items.takeKeeping(limit = 4, keepIndex = 3))
    }

    @Test
    fun `takeKeeping takes the first items when the kept item is within the limit`() {
        assertEquals(listOf("a", "b"), items.takeKeeping(limit = 2, keepIndex = 1))
    }

    @Test
    fun `takeKeeping replaces the last item with a kept item beyond the limit`() {
        assertEquals(listOf("a", "d"), items.takeKeeping(limit = 2, keepIndex = 3))
    }

    @Test
    fun `takeKeeping keeps only the kept item with a limit of one`() {
        assertEquals(listOf("c"), items.takeKeeping(limit = 1, keepIndex = 2))
    }

    @Test
    fun `takeKeeping ignores a kept index outside the list`() {
        assertEquals(listOf("a", "b"), items.takeKeeping(limit = 2, keepIndex = 7))
        assertEquals(listOf("a", "b"), items.takeKeeping(limit = 2, keepIndex = -1))
    }

    @Test
    fun `takeKeeping returns nothing for a limit of zero`() {
        assertEquals(emptyList<String>(), items.takeKeeping(limit = 0, keepIndex = 0))
    }

    @Test
    fun `keptIndex points at the kept item`() {
        assertEquals(1, keptIndex(keepIndex = 1, shownSize = 2))
        assertEquals(1, keptIndex(keepIndex = 3, shownSize = 2))
        assertEquals(0, keptIndex(keepIndex = -1, shownSize = 2))
    }
}
