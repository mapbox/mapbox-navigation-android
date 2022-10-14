package com.mapbox.navigation.core.directions.session

import androidx.annotation.StringDef
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure

class RoutePreview private constructor(
    @State
    val state: String,
    val navigationRoutes: List<NavigationRoute>,
    val selectedIndex: Int?,
    val destinationName: String?,
    val failureReasons: List<RouterFailure>?,
    val routeOptions: RouteOptions?
) {
    companion object {
        const val EMPTY = "EMPTY"
        const val FETCHING = "FETCHING"
        const val CANCELED = "CANCELED"
        const val FAILED = "FAILED"
        const val READY = "READY"
    }

    /**
     * Retention policy for the EHorizonResultType
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        EMPTY,
        READY,
        CANCELED,
        FAILED
    )
    annotation class State

    internal fun toBuilder() = Builder()
        .navigationRoutes(navigationRoutes)
        .selectedIndex(selectedIndex)
        .destinationName(destinationName)
        .failureReasons(failureReasons)

    internal class Builder {
        var navigationRoutes: List<NavigationRoute> = emptyList()
        var selectedIndex = 0
        var destinationName = ""
        var failureReasons: List<RouterFailure>? = null
        var routeOptions: RouteOptions? = null

        fun navigationRoutes(navigationRoutes: List<NavigationRoute>) = apply {
            this.navigationRoutes = navigationRoutes
        }

        fun selectedIndex(selectedIndex: Int) = apply {
            check(selectedIndex > 0 && selectedIndex < navigationRoutes.size)
            this.selectedIndex = selectedIndex
        }

        fun destinationName(destinationName: String) = apply {
            this.destinationName = destinationName
        }

        fun failureReasons(failureReasons: List<RouterFailure>?) = apply {
            this.failureReasons = failureReasons
        }

        fun build(): RoutePreview {
            val state = when {
                navigationRoutes.isNotEmpty() -> READY
                navigationRoutes.isEmpty() -> EMPTY
                failureReasons != null -> FAILED
                else -> FAILED
            }
            return RoutePreview(
                state = state,
                navigationRoutes = navigationRoutes,
                selectedIndex = selectedIndex,
                destinationName = destinationName,
                failureReasons = failureReasons,
                routeOptions = routeOptions,
            )
        }
    }
}
