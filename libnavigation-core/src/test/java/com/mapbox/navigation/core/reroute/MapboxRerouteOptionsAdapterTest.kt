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
    fun noAdapters() {
        val sut = MapboxRerouteOptionsAdapter(emptyList())

        assertEquals(inputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun singleAdapter() {
        val outputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        val adapter = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(inputOptions) } returns outputOptions
        }
        val sut = MapboxRerouteOptionsAdapter(listOf(adapter))

        assertEquals(outputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun multipleAdapters() {
        val outputOptions1 = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)
        val outputOptions2 = createRouteOptions(profile = DirectionsCriteria.PROFILE_WALKING)
        val outputOptions3 = createRouteOptions(profile = DirectionsCriteria.PROFILE_CYCLING)
        val adapter1 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(inputOptions) } returns outputOptions1
        }
        val adapter2 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions1) } returns outputOptions2
        }
        val adapter3 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions2) } returns outputOptions3
        }
        val sut = MapboxRerouteOptionsAdapter(listOf(adapter1, adapter2, adapter3))

        assertEquals(outputOptions3, sut.onRouteOptions(inputOptions))
    }
}
