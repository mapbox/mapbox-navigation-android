package com.mapbox.navigation.ui.routealert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk

class MapboxRouteAlertsDisplayerOptionsTest :
    BuilderTest<MapboxRouteAlertsDisplayerOptions, MapboxRouteAlertsDisplayerOptions.Builder>() {
    override fun getImplementationClass() = MapboxRouteAlertsDisplayerOptions::class

    override fun getFilledUpBuilder() = MapboxRouteAlertsDisplayerOptions.Builder(
        mockk()
    ).apply {
        showToll(true)
    }

    override fun trigger() {
        // see docs
    }
}
