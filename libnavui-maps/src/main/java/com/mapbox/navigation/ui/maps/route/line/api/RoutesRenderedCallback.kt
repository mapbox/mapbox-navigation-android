package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

/**
 Callback to be notified of rendered routes.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface RoutesRenderedCallback {

    /**
     * Invoked when routes are rendered.
     *
     * @param result [RoutesRenderedResult] describing rendering result
     */
    fun onRoutesRendered(result: RoutesRenderedResult)
}

/**
 * Class describing the result of routes rendering.
 *
 * @param successfullyRenderedRouteIds ids of the routes that were successfully rendered (correspond to [NavigationRoute.id])
 * @param renderingCancelledRouteIds ids of the routes whose rendering was cancelled (correspond to [NavigationRoute.id]).
 *  This may happen, for example, when a newer rendering operation had been queued before the previous one finished.
 * @param successfullyClearedRouteIds ids of the routes that were successfully cleared (correspond to [NavigationRoute.id])
 * @param clearingCancelledRouteIds ids of the routes whose clearance was cancelled (correspond to [NavigationRoute.id]).
 *  This may happen, for example, when a newer rendering operation had been queued before the previous one finished.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoutesRenderedResult internal constructor(
    val successfullyRenderedRouteIds: Set<String>,
    val renderingCancelledRouteIds: Set<String>,
    val successfullyClearedRouteIds: Set<String>,
    val clearingCancelledRouteIds: Set<String>,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesRenderedResult

        if (successfullyRenderedRouteIds != other.successfullyRenderedRouteIds) return false
        if (renderingCancelledRouteIds != other.renderingCancelledRouteIds) return false
        if (successfullyClearedRouteIds != other.successfullyClearedRouteIds) return false
        if (clearingCancelledRouteIds != other.clearingCancelledRouteIds) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = successfullyRenderedRouteIds.hashCode()
        result = 31 * result + renderingCancelledRouteIds.hashCode()
        result = 31 * result + successfullyClearedRouteIds.hashCode()
        result = 31 * result + clearingCancelledRouteIds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesRenderedResult(" +
            "successfullyRouteIds=$successfullyRenderedRouteIds, " +
            "renderingCancelledRouteIds=$renderingCancelledRouteIds, " +
            "successfullyClearedRouteIds=$successfullyClearedRouteIds, " +
            "clearingCancelledRouteIds=$clearingCancelledRouteIds" +
            ")"
    }
}
