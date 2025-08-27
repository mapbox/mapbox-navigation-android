package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.Keep
import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData

/**
 * Represents data used to remove the route line(s) from the map.
 *
 * @param primaryRouteSource a feature collection representing the primary route
 * @param alternativeRoutesSources feature collections representing alternative routes
 * @param waypointsSource a feature collection representing the origin and destination icons
 * @param callouts represents data about route callouts
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Keep
class RouteLineClearValue internal constructor(
    internal val primaryRouteSource: FeatureCollection,
    internal val alternativeRoutesSources: List<FeatureCollection>,
    internal val waypointsSource: FeatureCollection,
    internal val callouts: RouteCalloutData,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineClearValue

        if (primaryRouteSource != other.primaryRouteSource) return false
        if (alternativeRoutesSources != other.alternativeRoutesSources) return false
        if (waypointsSource != other.waypointsSource) return false
        if (callouts != other.callouts) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = primaryRouteSource.hashCode()
        result = 31 * result + alternativeRoutesSources.hashCode()
        result = 31 * result + waypointsSource.hashCode()
        result = 31 * result + callouts.hashCode()
        return result
    }
}
