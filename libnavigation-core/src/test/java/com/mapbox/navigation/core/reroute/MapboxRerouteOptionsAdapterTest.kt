package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxRerouteOptionsAdapterTest {

    private val externalAdapter = mockk<RerouteOptionsAdapter>()
    private val inputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)

    @Test
    fun noInternalAdaptersNorExternal() {
        val sut = MapboxRerouteOptionsAdapter(emptyList())

        assertEquals(inputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun noInternalAdaptersHasExternal() {
        val outputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        every { externalAdapter.onRouteOptions(inputOptions) } returns outputOptions

        val sut = MapboxRerouteOptionsAdapter(emptyList())
        sut.externalOptionsAdapter = externalAdapter

        assertEquals(outputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun noInternalAdaptersHasRemovedExternal() {
        val outputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        every { externalAdapter.onRouteOptions(inputOptions) } returns outputOptions

        val sut = MapboxRerouteOptionsAdapter(emptyList())
        sut.externalOptionsAdapter = externalAdapter
        sut.externalOptionsAdapter = null

        assertEquals(inputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun singleInternalAdapterNoExternal() {
        val outputOptions = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        val adapter = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(inputOptions) } returns outputOptions
        }
        val sut = MapboxRerouteOptionsAdapter(listOf(adapter))

        assertEquals(outputOptions, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun multipleInternalAdaptersHasExternal() {
        val outputOptions1 = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)
        val outputOptions2 = createRouteOptions(profile = DirectionsCriteria.PROFILE_WALKING)
        val outputOptions3 = createRouteOptions(profile = DirectionsCriteria.PROFILE_CYCLING)
        val outputOptionsExternal =
            createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        val adapter1 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(inputOptions) } returns outputOptions1
        }
        val adapter2 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions1) } returns outputOptions2
        }
        val adapter3 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions2) } returns outputOptions3
        }
        every { externalAdapter.onRouteOptions(outputOptions3) } returns outputOptionsExternal

        val sut = MapboxRerouteOptionsAdapter(listOf(adapter1, adapter2, adapter3))
        sut.externalOptionsAdapter = externalAdapter

        assertEquals(outputOptionsExternal, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun multipleInternalAdaptersHasRemovedExternal() {
        val outputOptions1 = createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING)
        val outputOptions2 = createRouteOptions(profile = DirectionsCriteria.PROFILE_WALKING)
        val outputOptions3 = createRouteOptions(profile = DirectionsCriteria.PROFILE_CYCLING)
        val outputOptionsExternal =
            createRouteOptions(profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        val adapter1 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(inputOptions) } returns outputOptions1
        }
        val adapter2 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions1) } returns outputOptions2
        }
        val adapter3 = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(outputOptions2) } returns outputOptions3
        }
        every { externalAdapter.onRouteOptions(outputOptions3) } returns outputOptionsExternal

        val sut = MapboxRerouteOptionsAdapter(listOf(adapter1, adapter2, adapter3))
        sut.externalOptionsAdapter = externalAdapter
        sut.externalOptionsAdapter = null

        assertEquals(outputOptions3, sut.onRouteOptions(inputOptions))
    }

    @Test
    fun multipleInternalAdaptersNoExternal() {
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
