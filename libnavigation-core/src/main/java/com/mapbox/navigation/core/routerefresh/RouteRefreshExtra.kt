package com.mapbox.navigation.core.routerefresh

import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshOptions

/**
 * Extra data of route refresh
 */
@ExperimentalPreviewMapboxNavigationAPI
object RouteRefreshExtra {

    /**
     * The state becomes [REFRESH_STATE_STARTED] when route refresh round is started.
     */
    const val REFRESH_STATE_STARTED = "STARTED"

    /**
     * The state becomes [REFRESH_STATE_FINISHED_SUCCESS] when the route is successfully refreshed.
     * The state is triggered in case if at least one route is refreshed successfully.
     */
    const val REFRESH_STATE_FINISHED_SUCCESS = "FINISHED_SUCCESS"

    /**
     * The state becomes [REFRESH_STATE_FINISHED_FAILED] when a route refresh failed and the route
     * is cleaned up of expired data, see [RouteRefreshOptions] for details.
     * The state is triggered in case if every single route refresh of a set of the routes is failed.
     */
    const val REFRESH_STATE_FINISHED_FAILED = "FINISHED_FAILED"

    /**
     * The state becomes [REFRESH_STATE_CANCELED] when a route refresh canceled. It occurs
     * when a new set of routes are set, that leads to interrupt route refresh process.
     */
    const val REFRESH_STATE_CANCELED = "CANCELED"

    /**
     * Route refresh states. See [RouteRefreshStatesObserver].
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        REFRESH_STATE_STARTED,
        REFRESH_STATE_FINISHED_SUCCESS,
        REFRESH_STATE_FINISHED_FAILED,
        REFRESH_STATE_CANCELED,
    )
    annotation class RouteRefreshState
}
