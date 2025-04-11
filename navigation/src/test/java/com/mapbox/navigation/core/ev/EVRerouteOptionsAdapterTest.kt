package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.isEVRoute
import com.mapbox.navigation.core.reroute.defaultRouteOptionsAdapterParams
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EVRerouteOptionsAdapterTest {

    private val evDynamicDataHolder = mockk<EVDynamicDataHolder>(relaxed = true)
    private val sut = EVRerouteOptionsAdapter(evDynamicDataHolder)

    @Test
    fun `non EV route`() {
        val options = createRouteOptions(unrecognizedProperties = null)

        assertTrue(options === sut.onRouteOptions(options, defaultRouteOptionsAdapterParams))
    }

    @Test
    fun `EV route with empty unrecognized properties and empty EV data`() {
        val unrecognizedProperties = mapOf("engine" to JsonPrimitive("electric"))
        every { evDynamicDataHolder.currentData(unrecognizedProperties) } returns emptyMap()
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(options, sut.onRouteOptions(options, defaultRouteOptionsAdapterParams))
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
                "cc" to JsonPrimitive("dd"),
            ),
        )

        assertEquals(expectedOptions, sut.onRouteOptions(options, defaultRouteOptionsAdapterParams))
    }

    @Test
    fun `EV route with non empty unrecognized properties and empty EV data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "aaa" to JsonPrimitive("bbb"),
        )
        every { evDynamicDataHolder.currentData(unrecognizedProperties) } returns emptyMap()
        val options = createRouteOptions(unrecognizedProperties = unrecognizedProperties)

        assertEquals(options, sut.onRouteOptions(options, defaultRouteOptionsAdapterParams))
    }

    @Test
    fun `EV route with non empty unrecognized properties and non empty EV data`() {
        val unrecognizedProperties = mapOf(
            "engine" to JsonPrimitive("electric"),
            "eee" to JsonPrimitive("fff"),
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
                "cc" to JsonPrimitive("dd"),
            ),
        )

        assertEquals(expectedOptions, sut.onRouteOptions(options, defaultRouteOptionsAdapterParams))
    }

    @Test
    fun `EV route (recognized implicitly with extension fun) with null unrecognized fields`() {
        mockkStatic(RouteOptions::isEVRoute) {
            every {
                evDynamicDataHolder.currentData(emptyMap())
            } returns mapOf("aaa" to "bbb", "cc" to "dd")
            val options = createRouteOptions(unrecognizedProperties = null)
            every { options.isEVRoute() } returns true
            val expectedOptions = createRouteOptions(
                unrecognizedProperties = mapOf(
                    "aaa" to JsonPrimitive("bbb"),
                    "cc" to JsonPrimitive("dd"),
                ),
            )

            assertEquals(
                expectedOptions,
                sut.onRouteOptions(options, defaultRouteOptionsAdapterParams),
            )
        }
    }
}
