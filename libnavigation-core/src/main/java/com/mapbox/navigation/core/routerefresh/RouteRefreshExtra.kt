package com.mapbox.navigation.core.routerefresh

import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

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
     * The state becomes [REFRESH_STATE_FINISHED_FAILED] when a route refresh failed after all
     * the retry attempts (whose number may differ depending on refresh mode: planned or on-demand).
     * The state is triggered in case refresh of every route from routes failed.
     */
    const val REFRESH_STATE_FINISHED_FAILED = "FINISHED_FAILED"

    /**
     * The state becomes [REFRESH_STATE_CLEARED_EXPIRED] when expired incidents and congestion
     * annotations are removed from the route due to failure of a route refresh request.
     */
    const val REFRESH_STATE_CLEARED_EXPIRED = "CLEARED_EXPIRED"

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
        REFRESH_STATE_CLEARED_EXPIRED,
        REFRESH_STATE_CANCELED,
    )
    annotation class RouteRefreshState
}
