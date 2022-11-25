package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EVRouteOptionsModifierTest {

    private val evDynamicDataHolder = mockk<EVDynamicDataHolder>(relaxed = true)
    private val sut = EVRouteOptionsModifier(evDynamicDataHolder)

    @Test
    fun `non EV route`() {
        val options = createRouteOptions(unrecognizedProperties = null)

        assertTrue(options === sut.modify(options))
    }

    @Test
    fun `EV route with empty unrecognized properties and empty EV data`() {
        val unrecognizedProperties = mapOf("engine" to JsonPrimitive("electric"))
        every { evDynamicDataHolder.currentData(unrecognizedProperties) } returns emptyMap()
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(options, sut.modify(options))
    }

    @Test
    fun `EV route with empty unrecognized properties and non empty EV data`() {
        val unrecognizedProperties = mapOf("engine" to JsonPrimitive("electric"))
        every {
            evDynamicDataHolder.currentData(unrecognizedProperties)
        } returns mapOf("aaa" to "bbb", "cc" to "dd")
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)
        val expectedOptions = createRouteOptions(
            unrecognizedProperties = mapOf(
                "engine" to JsonPrimitive("electric"),
                "aaa" to JsonPrimitive("bbb"),
                "cc" to JsonPrimitive("dd")
            )
        )

        assertEquals(expectedOptions, sut.modify(options))
    }

    @Test
    fun `EV route with non empty unrecognized properties and empty EV data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "aaa" to JsonPrimitive("bbb")
        )
        every { evDynamicDataHolder.currentData(unrecognizedProperties) } returns emptyMap()
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(options, sut.modify(options))
    }

    @Test
    fun `EV route with non empty unrecognized properties and non empty EV data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "eee" to JsonPrimitive("fff")
        )
        every {
            evDynamicDataHolder.currentData(unrecognizedProperties)
        } returns mapOf("aaa" to "bbb", "cc" to "dd")
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)
        val expectedOptions = createRouteOptions(
            unrecognizedProperties = mapOf(
                "engine" to JsonPrimitive("electric"),
                "eee" to JsonPrimitive("fff"),
                "aaa" to JsonPrimitive("bbb"),
                "cc" to JsonPrimitive("dd")
            )
        )

        assertEquals(expectedOptions, sut.modify(options))
    }
}
