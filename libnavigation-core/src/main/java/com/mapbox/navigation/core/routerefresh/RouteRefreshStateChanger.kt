package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object RouteRefreshStateChanger {

    private val allowedTransitions = mapOf(
        null to listOf(
            RouteRefreshExtra.REFRESH_STATE_STARTED,
            RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
        ),
        RouteRefreshExtra.REFRESH_STATE_STARTED to listOf(
            RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            RouteRefreshExtra.REFRESH_STATE_CANCELED,
            null,
        ),
        RouteRefreshExtra.REFRESH_STATE_CANCELED to listOf(
            RouteRefreshExtra.REFRESH_STATE_STARTED,
            RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            null,
        ),
        RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED to listOf(
            RouteRefreshExtra.REFRESH_STATE_STARTED,
            null,
        ),
        RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS to listOf(
            RouteRefreshExtra.REFRESH_STATE_STARTED,
            null
        ),
    )

    fun canChange(
        @RouteRefreshExtra.RouteRefreshState from: String?,
        @RouteRefreshExtra.RouteRefreshState to: String?
    ): Boolean {
        return to in allowedTransitions[from]!!
    }
}
