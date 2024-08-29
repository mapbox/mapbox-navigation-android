package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteCalloutViewOptionsTest :
    BuilderTest<MapboxRouteCalloutViewOptions, MapboxRouteCalloutViewOptions.Builder>() {

    override fun getImplementationClass() = MapboxRouteCalloutViewOptions::class

    override fun getFilledUpBuilder(): MapboxRouteCalloutViewOptions.Builder {
        return MapboxRouteCalloutViewOptions.Builder()
            .textColor(100)
            .selectedTextColor(99)
            .backgroundColor(101)
            .selectedBackgroundColor(98)
            .fasterTextColor(102)
            .slowerTextColor(103)
            .durationTextAppearance(111)
    }

    override fun trigger() {
        //
    }
}
