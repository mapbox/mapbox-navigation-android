package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxRerouteOptionsAdapterTest {

    private val inputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)

    @Test
    fun noModifiers() {
        val sut = MapboxRerouteOptionsAdapter(emptyList())

        assertEquals(inputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun singleModifier() {
        val outputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        val modifier = mockk<RouteOptionsModifier> {
            every { modify(inputOptions) } returns outputOptions
        }
        val sut = MapboxRerouteOptionsAdapter(listOf(modifier))

        assertEquals(outputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun multipleModifiers() {
        val outputOptions1 = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)
        val outputOptions2 = createRouteOptions(profile = DirectionsCriteria.PROFILE_WALKING)
        val outputOptions3 = createRouteOptions(profile = DirectionsCriteria.PROFILE_CYCLING)
        val modifier1 = mockk<RouteOptionsModifier> {
            every { modify(inputOptions) } returns outputOptions1
        }
        val modifier2 = mockk<RouteOptionsModifier> {
            every { modify(outputOptions1) } returns outputOptions2
        }
        val modifier3 = mockk<RouteOptionsModifier> {
            every { modify(outputOptions2) } returns outputOptions3
        }
        val sut = MapboxRerouteOptionsAdapter(listOf(modifier1, modifier2, modifier3))

        assertEquals(outputOptions3, sut.onRouteOptions(inputOptions))
    }
}
