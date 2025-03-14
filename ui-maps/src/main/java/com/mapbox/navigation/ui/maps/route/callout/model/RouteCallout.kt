package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.time.Duration

/**
 * @param route represents route that callout is attached to
 * @param isPrimary indicates if this callout is attached to the primary route
 * @param durationDifferenceWithPrimary represents the difference in duration between [route] and
 * primary route. In case [isPrimary] set to true this value equals to zero
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteCallout internal constructor(
    val route: NavigationRoute,
    val isPrimary: Boolean,
    val durationDifferenceWithPrimary: Duration,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteCallout

        if (route != other.route) return false
        if (isPrimary != other.isPrimary) return false
        if (durationDifferenceWithPrimary != other.durationDifferenceWithPrimary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + isPrimary.hashCode()
        result = 31 * result + durationDifferenceWithPrimary.hashCode()
        return result
    }

    override fun toString(): String {
        return "RouteCallout(" +
            "route=$route, " +
            "isPrimary=$isPrimary, " +
            "durationDifferenceWithPrimary=$durationDifferenceWithPrimary)"
    }
}
