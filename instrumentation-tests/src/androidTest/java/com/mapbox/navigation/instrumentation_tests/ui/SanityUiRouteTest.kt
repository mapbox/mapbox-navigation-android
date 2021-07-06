package com.mapbox.navigation.instrumentation_tests.ui

import androidx.test.espresso.Espresso
import com.mapbox.navigation.instrumentation_tests.utils.idling.ArrivalIdlingResource
import org.junit.Test

class SanityUiRouteTest : SimpleMapViewNavigationTest() {

    @Test
    fun route_completes() {
        addRouteLine()
        addLocationPuck()
        addNavigationCamera()
        val arrivalIdlingResource = ArrivalIdlingResource(mapboxNavigation)
        arrivalIdlingResource.register()
        Espresso.onIdle()
        arrivalIdlingResource.unregister()
    }
}
