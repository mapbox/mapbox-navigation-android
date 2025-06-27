package com.mapbox.navigation.ui.maps.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@ExperimentalPreviewMapboxNavigationAPI
class RouteCalloutData(val callouts: List<RouteCallout>) {

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
