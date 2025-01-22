package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteCalloutApiOptionsTest :
    BuilderTest<MapboxRouteCalloutApiOptions, MapboxRouteCalloutApiOptions.Builder>() {

    override fun getImplementationClass() = MapboxRouteCalloutApiOptions::class

    override fun getFilledUpBuilder(): MapboxRouteCalloutApiOptions.Builder {
        return MapboxRouteCalloutApiOptions.Builder()
            .routeCalloutType(RouteCalloutType.RelativeDurationsOnAlternative)
            .similarDurationDelta(1.minutes)
    }

    override fun trigger() {
        //
    }
}
