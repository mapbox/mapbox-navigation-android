package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue

object RouteLineExpectedFactory {

    fun routeLineError(message: String): RouteLineError {
        return RouteLineError(message, null)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun routeSetValue(
        primary: RouteLineEventData,
        alternatives: List<RouteLineEventData>,
        waypointSource: FeatureCollection,
        masking: RouteLineDynamicEventData?,
    ): RouteSetValue {
        return RouteSetValue(
            primary.toRouteLineData(),
            alternatives.map { it.toRouteLineData() },
            waypointSource,
            RouteCalloutData(emptyList()),
            masking?.toRouteLineDynamicData(),
        )
    }

    fun routeLineUpdateValue(
        primary: RouteLineDynamicEventData?,
        alternatives: List<RouteLineDynamicEventData>,
        masking: RouteLineDynamicEventData?,
    ): RouteLineUpdateValue {
        return RouteLineUpdateValue(
            primary?.toRouteLineDynamicData(),
            alternatives.map { it.toRouteLineDynamicData() },
            masking?.toRouteLineDynamicData(),
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun routeLineClearValue(
        primary: FeatureCollection,
        alternatives: List<FeatureCollection>,
        waypointSource: FeatureCollection,
    ): RouteLineClearValue {
        return RouteLineClearValue(
            primary,
            alternatives,
            waypointSource,
            RouteCalloutData(emptyList()),
        )
    }
}
