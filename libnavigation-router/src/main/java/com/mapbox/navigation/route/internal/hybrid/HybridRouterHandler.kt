package com.mapbox.navigation.route.internal.hybrid

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.LoggerProvider

internal sealed class HybridRouterHandler(
    protected val primaryRouter: Router,
    protected val fallbackRouter: Router,
) {

    private companion object {
        private val TAG = Tag("MbxHybridRouter")
    }

    protected val primaryRouterName = primaryRouter::class.java.canonicalName
    protected val fallbackRouterName = fallbackRouter::class.java.canonicalName

    protected var primaryRouterRequestId: Long? = null
        set(value) {
            if (field != null) {
                throw IllegalArgumentException(
                    "primaryRouterRequestId was already set for this request"
                )
            }
            field = value
        }
    protected var fallbackRouterRequestId: Long? = null
        set(value) {
            if (field != null) {
                throw IllegalArgumentException(
                    "fallbackRouterRequestId was already set for this request"
                )
            }
            field = value
        }

    internal class Directions(
        primaryRouter: Router,
        fallbackRouter: Router,
    ) : HybridRouterHandler(primaryRouter, fallbackRouter) {

        private inner class PrimaryCallback(
            private val clientCallback: RouterCallback
        ) : RouterCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
                clientCallback.onRoutesReady(routes, routerOrigin)
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                LoggerProvider.logger.w(
                    TAG,
                    Message(
                        """
                            Route request for $primaryRouterName failed with:
                            $reasons
                            Trying to fallback to $fallbackRouterName...
                        """.trimIndent()
                    )
                )

                fallbackRouterRequestId = fallbackRouter.getRoute(
                    routeOptions,
                    FallbackCallback(clientCallback, reasons)
                )
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                clientCallback.onCanceled(routeOptions, routerOrigin)
            }
        }

        private inner class FallbackCallback(
            private val clientCallback: RouterCallback,
            private val primaryFailureReasons: List<RouterFailure>
        ) : RouterCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
                clientCallback.onRoutesReady(routes, routerOrigin)
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                clientCallback.onFailure(
                    primaryFailureReasons.map { failure ->
                        RouterFailure(
                            url = failure.url,
                            routerOrigin = failure.routerOrigin,
                            message =
                            "Primary router ($primaryRouterName), " +
                                "origin ${failure.routerOrigin}, " +
                                "failed with: ${failure.message}",
                            code = failure.code,
                            throwable = failure.throwable
                        )
                    }.plus(
                        reasons.map { failure ->
                            RouterFailure(
                                url = failure.url,
                                routerOrigin = failure.routerOrigin,
                                message =
                                "Fallback router ($fallbackRouterName), " +
                                    "origin ${failure.routerOrigin}, " +
                                    "failed with: ${failure.message}",
                                code = failure.code,
                                throwable = failure.throwable
                            )
                        }
                    ),
                    routeOptions
                )
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                clientCallback.onCanceled(routeOptions, routerOrigin)
            }
        }

        fun getRoute(
            routeOptions: RouteOptions,
            callback: RouterCallback
        ) {
            primaryRouterRequestId = primaryRouter.getRoute(
                routeOptions,
                PrimaryCallback(callback)
            )
        }

        fun cancelRouteRequest() {
            primaryRouterRequestId?.let {
                primaryRouter.cancelRouteRequest(it)
            }
            fallbackRouterRequestId?.let {
                fallbackRouter.cancelRouteRequest(it)
            }
        }
    }

    internal class Refresh(
        primaryRouter: Router,
        fallbackRouter: Router,
    ) : HybridRouterHandler(primaryRouter, fallbackRouter) {

        private inner class PrimaryCallback(
            private val route: DirectionsRoute,
            private val legIndex: Int,
            private val clientCallback: RouteRefreshCallback
        ) : RouteRefreshCallback {
            override fun onRefresh(directionsRoute: DirectionsRoute) {
                clientCallback.onRefresh(directionsRoute)
            }

            override fun onError(error: RouteRefreshError) {
                LoggerProvider.logger.w(
                    TAG,
                    Message(
                        """
                            Route refresh for $primaryRouterName failed for
                            UUID = ${route.requestUuid()}
                            legIndex = $legIndex
                            
                            message = ${error.message}
                            stack = ${error.throwable}
                            Trying to fallback to $fallbackRouterName...
                        """.trimIndent()
                    )
                )

                fallbackRouterRequestId = fallbackRouter.getRouteRefresh(
                    route,
                    legIndex,
                    FallbackCallback(route, legIndex, clientCallback, error)
                )
            }
        }

        private inner class FallbackCallback(
            private val route: DirectionsRoute,
            private val legIndex: Int,
            private val clientCallback: RouteRefreshCallback,
            private val primaryError: RouteRefreshError
        ) : RouteRefreshCallback {

            override fun onRefresh(directionsRoute: DirectionsRoute) {
                LoggerProvider.logger.w(
                    TAG,
                    Message(
                        """
                            Route refresh successful fallback to a $fallbackRouterName.
                        """.trimIndent()
                    )
                )
                clientCallback.onRefresh(directionsRoute)
            }

            override fun onError(error: RouteRefreshError) {
                LoggerProvider.logger.e(
                    TAG,
                    Message(
                        """
                            Fallback route refresh for $fallbackRouterName failed with:
                            UUID = ${route.requestUuid()}
                            legIndex = $legIndex
                            
                            message = ${error.message}
                            stack = ${error.throwable}
                        """.trimIndent()
                    )
                )
                clientCallback.onError(
                    RouteRefreshError(
                        """
                           ${primaryError.message}
                           ${error.message}
                        """.trimIndent(),
                        Throwable(
                            """
                                Primary route refresh failed with:
                                message = ${primaryError.message}
                                stack = ${primaryError.throwable?.stackTraceToString()}
                                
                                Fallback route refresh failed with:
                                message = ${error.message}
                                stack = ${error.throwable?.stackTraceToString()}
                            """.trimIndent()
                        )
                    )
                )
            }
        }

        fun getRouteRefresh(
            route: DirectionsRoute,
            legIndex: Int,
            callback: RouteRefreshCallback
        ) {
            primaryRouterRequestId = primaryRouter.getRouteRefresh(
                route,
                legIndex,
                PrimaryCallback(route, legIndex, callback)
            )
        }

        fun cancelRouteRefreshRequest() {
            primaryRouterRequestId?.let {
                primaryRouter.cancelRouteRefreshRequest(it)
            }
            fallbackRouterRequestId?.let {
                fallbackRouter.cancelRouteRefreshRequest(it)
            }
        }
    }
}
