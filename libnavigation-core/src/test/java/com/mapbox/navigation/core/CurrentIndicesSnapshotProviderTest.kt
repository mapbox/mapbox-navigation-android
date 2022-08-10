package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentIndicesSnapshotProviderTest {

    private val provider = CurrentIndicesSnapshotProvider()
    private val currentLegIndex = 9
    private val routeGeometryIndex = 44
    private val legGeometryIndex = 33
    private val routeProgress = mockk<RouteProgress> {
        every { currentRouteGeometryIndex } returns routeGeometryIndex
        every { currentLegProgress } returns mockk {
            every { legIndex } returns currentLegIndex
            every { geometryIndex } returns legGeometryIndex
        }
    }
    private val expected = CurrentIndicesSnapshot(
        currentLegIndex,
        routeGeometryIndex,
        legGeometryIndex
    )

    @Test
    fun initialStateIsDefault() {
        assertEquals(CurrentIndicesSnapshot(), provider())
        assertEquals(CurrentIndicesSnapshot(), provider.freezeAndGet())
    }

    @Test
    fun stateAfterUpdate() {
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun stateAfterUpdateLegProgressIsNull() {
        val routeIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns null
        }
        val expected = CurrentIndicesSnapshot(0, routeIndex, null)

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun stateAfterUpdateTwice() {
        val legIndex1 = 5
        val routeGeometryIndex1 = 78
        val legGeometryIndex1 = 61
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex1
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex1
                every { geometryIndex } returns legGeometryIndex1
            }
        }
        val legIndex2 = 9
        val routeGeometryIndex2 = 44
        val legGeometryIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex2
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex2
                every { geometryIndex } returns legGeometryIndex2
            }
        }
        val expected = CurrentIndicesSnapshot(legIndex2, routeGeometryIndex2, legGeometryIndex2)
        provider.onRouteProgressChanged(routeProgress1)

        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun freezeAndGetForbidsUpdates() {
        val expected = provider.freezeAndGet()
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun invokeDoesNotForbidUpdates() {
        provider()
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun unfreezeAllowsUpdates() {
        provider.freezeAndGet()

        provider.unfreeze()
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }

    @Test
    fun valuesAreRememberedInFrozenState() {
        provider.freezeAndGet()
        provider.onRouteProgressChanged(routeProgress)
        provider.unfreeze()

        assertEquals(expected, provider())
        assertEquals(expected, provider.freezeAndGet())
    }
}
