package com.mapbox.navigation.dropin.component.tripprogress

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripProgressProcessorTest {

    @Test
    fun processVisibility_activeNavigation() {
        val result =
            TripProgressProcessor.ProcessVisibility(NavigationState.ActiveNavigation).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibility_arrival() {
        val result =
            TripProgressProcessor.ProcessVisibility(NavigationState.Arrival).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibility_empty() {
        val result =
            TripProgressProcessor.ProcessVisibility(NavigationState.Empty).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_freeDrive() {
        val result =
            TripProgressProcessor.ProcessVisibility(NavigationState.FreeDrive).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibility_routePreview() {
        val result =
            TripProgressProcessor.ProcessVisibility(NavigationState.RoutePreview).process()

        assertFalse(result.isVisible)
    }
}
