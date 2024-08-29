package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.time.Duration

@ExperimentalPreviewMapboxNavigationAPI
class RouteCalloutData internal constructor(internal val callouts: List<RouteCallout>) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteCalloutData
        if (callouts != other.callouts) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return callouts.hashCode()
    }
}

internal sealed class RouteCallout {

    abstract val route: NavigationRoute
    abstract val geometry: Geometry
    data class Eta internal constructor(
        override val route: NavigationRoute,
        override val geometry: LineString,
        val isPrimary: Boolean,
    ) : RouteCallout()

    data class DurationDifference internal constructor(
        override val route: NavigationRoute,
        override val geometry: LineString,
        val duration: Duration,
        val type: DurationDifferenceType,
    ) : RouteCallout()
}
