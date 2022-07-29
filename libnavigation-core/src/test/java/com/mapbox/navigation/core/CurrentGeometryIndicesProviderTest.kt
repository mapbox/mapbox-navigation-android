package com.mapbox.navigation.core

import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentGeometryIndicesProviderTest {

    private val provider = CurrentGeometryIndicesProvider()

    @Test
    fun initialStateIsNulls() {
        assertEquals(null to null, provider())
    }

    @Test
    fun stateAfterUpdate() {
        val routeIndex = 78
        val legIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns mockk {
                every { geometryIndex } returns legIndex
            }
        }

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(routeIndex to legIndex, provider())
    }

    @Test
    fun stateAfterUpdateLegProgressIsNull() {
        val routeIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns null
        }

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(routeIndex to null, provider())
    }

    @Test
    fun stateAfterUpdateTwice() {
        val routeIndex1 = 78
        val legIndex1 = 61
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex1
            every { currentLegProgress } returns mockk {
                every { geometryIndex } returns legIndex1
            }
        }
        val routeIndex2 = 44
        val legIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex2
            every { currentLegProgress } returns mockk {
                every { geometryIndex } returns legIndex2
            }
        }
        provider.onRouteProgressChanged(routeProgress1)

        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(routeIndex2 to legIndex2, provider())
    }
}
