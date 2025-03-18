package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DefaultRouteCalloutAdapterOptionsTest :
    BuilderTest<DefaultRouteCalloutAdapterOptions, DefaultRouteCalloutAdapterOptions.Builder>() {

    override fun getImplementationClass() = DefaultRouteCalloutAdapterOptions::class

    override fun getFilledUpBuilder(): DefaultRouteCalloutAdapterOptions.Builder {
        return DefaultRouteCalloutAdapterOptions.Builder()
            .textColor(100)
            .selectedTextColor(99)
            .backgroundColor(101)
            .selectedBackgroundColor(98)
            .fasterTextColor(102)
            .slowerTextColor(103)
            .durationTextAppearance(111)
            .routeCalloutType(RouteCalloutType.NAVIGATION)
            .similarDurationDelta(1.minutes)
    }

    override fun trigger() {
        //
    }
}
