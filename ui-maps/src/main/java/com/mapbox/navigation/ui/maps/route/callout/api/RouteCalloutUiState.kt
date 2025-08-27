package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout

/**
 * Represents UI data required to attach a DVA with this callout for a specified route.
 * See [RouteCalloutUiStateData] for more details and supported use cases.
 *
 * @param routeCallout route callout data, see [com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout] for details.
 * @param layerId use this layerId to attach the DVA to.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteCalloutUiState internal constructor(
    val routeCallout: RouteCallout,
    val layerId: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteCalloutUiState

        if (routeCallout != other.routeCallout) return false
        if (layerId != other.layerId) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeCallout.hashCode()
        result = 31 * result + layerId.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteCalloutUiState(routeCallout=$routeCallout, layerId='$layerId')"
    }
}
