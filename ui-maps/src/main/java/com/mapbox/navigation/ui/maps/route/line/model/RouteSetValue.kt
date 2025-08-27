package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData

/**
 * Represents the side effects for drawing routes on a map.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteSetValue internal constructor(
    internal val primaryRouteLineData: RouteLineData,
    internal val alternativeRouteLinesData: List<RouteLineData>,
    internal val waypointsSource: FeatureCollection,
    internal val callouts: RouteCalloutData,
    internal val routeLineMaskingLayerDynamicData: RouteLineDynamicData? = null,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteSetValue

        if (primaryRouteLineData != other.primaryRouteLineData) return false
        if (alternativeRouteLinesData != other.alternativeRouteLinesData) return false
        if (waypointsSource != other.waypointsSource) return false
        if (callouts != other.callouts) return false
        if (routeLineMaskingLayerDynamicData != other.routeLineMaskingLayerDynamicData) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = primaryRouteLineData.hashCode()
        result = 31 * result + alternativeRouteLinesData.hashCode()
        result = 31 * result + waypointsSource.hashCode()
        result = 31 * result + callouts.hashCode()
        result = 31 * result + (routeLineMaskingLayerDynamicData?.hashCode() ?: 0)
        return result
    }
}
