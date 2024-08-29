package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class EVRefreshDataProviderTest {

    private val evDynamicDataHolder = mockk<EVDynamicDataHolder>(relaxed = true)

    private val sut = EVRefreshDataProvider(evDynamicDataHolder)

    @Test
    fun `non EV route`() {
        val options = createRouteOptions(unrecognizedProperties = null)

        assertEquals(emptyMap<String, String>(), sut.get(options))
        verify(exactly = 0) { evDynamicDataHolder.currentData(any()) }
    }

    @Test
    fun `EV route empty data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "aaa" to JsonPrimitive(11),
        )
        every {
            evDynamicDataHolder.currentData(unrecognizedProperties)
        } returns emptyMap()
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(mapOf("engine" to "electric"), sut.get(options))
    }

    @Test
    fun `EV route non empty data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "aaa" to JsonPrimitive(11),
        )
        every {
            evDynamicDataHolder.currentData(unrecognizedProperties)
        } returns mapOf("aa" to "bb", "ccc" to "ddd")
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(
            mapOf(
                "engine" to "electric",
                "aa" to "bb",
                "ccc" to "ddd",
            ),
            sut.get(options),
        )
    }
}
