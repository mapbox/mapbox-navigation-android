package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManeuverProcessorTest {

    @Test
    fun processVisibility_navigationState_empty_maneuverIsError() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.Empty,
            ExpectedFactory.createError(mockk<ManeuverError>())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_empty_maneuverIsValue() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.Empty,
            ExpectedFactory.createValue(listOf())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_freeDrive_maneuverIsError() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.FreeDrive,
            ExpectedFactory.createError(mockk<ManeuverError>())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_freeDrive_maneuverIsValue() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.FreeDrive,
            ExpectedFactory.createValue(listOf())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_routePreview_maneuverIsError() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.RoutePreview,
            ExpectedFactory.createError(mockk<ManeuverError>())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_routePreview_maneuverIsValue() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.RoutePreview,
            ExpectedFactory.createValue(listOf())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_activeNavigation_maneuverIsError() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.ActiveNavigation,
            ExpectedFactory.createError(mockk<ManeuverError>())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_activeNavigation_maneuverIsValue() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.ActiveNavigation,
            ExpectedFactory.createValue(listOf())
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_arrival_maneuverIsError() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.Arrival,
            ExpectedFactory.createError(mockk<ManeuverError>())
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_navigationState_Arrival_maneuverIsValue() {
        val result = ManeuverProcessor.ProcessVisibility(
            NavigationState.Arrival,
            ExpectedFactory.createValue(listOf())
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processRouteProgress() {
        val maneuvers = listOf<Maneuver>()
        val routeProgress = mockk<RouteProgress>()
        val maneuverApi = mockk<MapboxManeuverApi> {
            every { getManeuvers(routeProgress) } returns ExpectedFactory.createValue(maneuvers)
        }

        val result = ManeuverProcessor.ProcessRouteProgress(routeProgress, maneuverApi).process()

        assertEquals(maneuvers, result.maneuver.value)
    }
}
