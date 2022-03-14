package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.navigator.internal.mapToDirectionsResponse
import com.mapbox.navigator.RerouteCallback
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RerouteInfo

/**
 * Wrap Outer [NavigationRerouteController] in Native [RerouteControllerInterface]
 */
internal class RerouteControllerAdapter private constructor(
    private val accessToken: String?,
    private val reroutesObserver: RerouteControllersManager.Observer,
    private val navigationRerouteController: NavigationRerouteController,
) : RerouteControllerInterface {

    internal companion object {
        @VisibleForTesting
        internal const val ERROR_EMPTY_NAVIGATION_ROUTES_LIST =
            "List of NavigationRoute mustn't be empty"

        internal operator fun invoke(
            accessToken: String?,
            reroutesObserver: RerouteControllersManager.Observer,
            navigationRerouteController: NavigationRerouteController,
        ): RerouteControllerAdapter =
            RerouteControllerAdapter(accessToken, reroutesObserver, navigationRerouteController)
    }

    override fun reroute(url: String, callback: RerouteCallback) {
        navigationRerouteController.reroute { navRoutes: List<NavigationRoute>, origin ->
            val expected: Expected<RerouteError, RerouteInfo> = if (navRoutes.isNotEmpty()) {
                reroutesObserver.onNewRoutes(navRoutes)
                ExpectedFactory.createValue(
                    RerouteInfo(
                        navRoutes.mapToDirectionsResponse().toJson(),
                        navRoutes.first().routeOptions.toUrl(accessToken ?: "").toString(),
                        origin.mapToNativeRouteOrigin(),
                    )
                )
            } else {
                ExpectedFactory.createError(
                    RerouteError(
                        ERROR_EMPTY_NAVIGATION_ROUTES_LIST,
                        RerouteErrorType.ROUTER_ERROR,
                    )
                )
            }

            callback.run(expected)
        }
    }

    override fun cancel() {
        navigationRerouteController.interrupt()
    }
}
