package com.mapbox.navigation.core.routerefresh

import junit.framework.Assert.assertEquals
import org.junit.Test

class EVDataHolderTest {

    private val evDataHolder = EVDataHolder()

    @Test
    fun `currentData() with default`() {
        assertEquals(0, evDataHolder.currentData().size)
    }

    @Test
    fun `currentData() after one update`() {
        val data = mapOf("aaa" to "bbb", "ccc" to "ddd")
        evDataHolder.onEVDataUpdated(data)

        assertEquals(data, evDataHolder.currentData())
    }

    @Test
    fun `currentData() after two updates`() {
        val data1 = mapOf("aaa" to "bbb", "ccc" to "ddd", "eee" to "fff")
        val data2 = mapOf("ccc" to "zzz", "eee" to null, "ggg" to "yyy")
        val expected = mapOf("aaa" to "bbb", "ccc" to "zzz", "ggg" to "yyy")
        evDataHolder.onEVDataUpdated(data1)
        evDataHolder.onEVDataUpdated(data2)

        assertEquals(expected, evDataHolder.currentData())
    }
}
