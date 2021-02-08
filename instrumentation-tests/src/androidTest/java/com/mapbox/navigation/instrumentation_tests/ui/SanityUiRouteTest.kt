package com.mapbox.navigation.instrumentation_tests.ui

import androidx.test.espresso.Espresso
import com.mapbox.navigation.instrumentation_tests.utils.idling.ArrivalIdlingResource
import org.junit.Test

class SanityUiRouteTest : SimpleMapViewNavigationTest() {

    @Test
    fun route_completes() {
        // puck needs to be added first,
        // see https://github.com/mapbox/mapbox-navigation-android-internal/issues/102
        addLocationPuck()
        addRouteLine()
        addNavigationCamera()
        val arrivalIdlingResource = ArrivalIdlingResource(mapboxNavigation)
        arrivalIdlingResource.register()
        Espresso.onIdle()
        arrivalIdlingResource.unregister()
    }
}
