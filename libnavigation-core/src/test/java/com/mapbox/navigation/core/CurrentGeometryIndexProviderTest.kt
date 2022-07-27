package com.mapbox.navigation.core

import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentGeometryIndexProviderTest {

    private val provider = CurrentGeometryIndexProvider()

    @Test
    fun initialStateIsNull() {
        assertNull(provider())
    }

    @Test
    fun stateAfterUpdate() {
        val index = 78
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns index
        }

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(index, provider())
    }

    @Test
    fun stateAfterUpdateTwice() {
        val index1 = 78
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns index1
        }
        val index2 = 44
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns index2
        }
        provider.onRouteProgressChanged(routeProgress1)

        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(index2, provider())
    }
}
