package com.mapbox.navigation.instrumentation_tests.ui

import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import kotlinx.coroutines.flow.first
import org.junit.Test

class SanityUiRouteTest : SimpleMapViewNavigationTest() {

    @Test
    fun route_completes() = sdkTest {
        addRouteLine()
        addLocationPuck()
        addNavigationCamera()
        mapboxNavigation.flowOnFinalDestinationArrival().first()
    }
}
