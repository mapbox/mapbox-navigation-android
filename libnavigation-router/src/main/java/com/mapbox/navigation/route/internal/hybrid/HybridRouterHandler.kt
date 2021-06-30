package com.mapbox.navigation.route.internal.hybrid

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router

internal sealed class HybridRouterHandler(
    protected val primaryRouter: Router,
    protected val fallbackRouter: Router,
    protected val logger: Logger
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
        logger: Logger
    ) : HybridRouterHandler(primaryRouter, fallbackRouter, logger) {

        private inner class PrimaryCallback(
            private val clientCallback: Router.Callback,
            private val routeOptions: RouteOptions
        ) : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                clientCallback.onResponse(routes)
            }

            override fun onFailure(throwable: Throwable) {
                logger.w(
                    TAG,
                    Message(
                        """
                            Route request for $primaryRouterName failed with:
                            throwable = $throwable
                            Trying to fallback to $fallbackRouterName...
                        """.trimMargin()
                    )
                )

                fallbackRouterRequestId = fallbackRouter.getRoute(
                    routeOptions,
                    FallbackCallback(clientCallback, throwable)
                )
            }

            override fun onCanceled() {
                clientCallback.onCanceled()
            }
        }

        private inner class FallbackCallback(
            private val clientCallback: Router.Callback,
            private val primaryThrowable: Throwable
        ) : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                clientCallback.onResponse(routes)
            }

            override fun onFailure(throwable: Throwable) {
                clientCallback.onFailure(
                    Throwable(
                        """
                            Primary router failed with:
                            message = ${primaryThrowable.message}
                            stack = ${primaryThrowable.stackTraceToString()}
                            
                            Fallback router failed with:
                            message = ${throwable.message}
                            stack = ${throwable.stackTraceToString()}
                        """.trimIndent(),
                    )
                )
            }

            override fun onCanceled() {
                clientCallback.onCanceled()
            }
        }

        fun getRoute(
            routeOptions: RouteOptions,
            callback: Router.Callback
        ) {
            primaryRouterRequestId = primaryRouter.getRoute(
                routeOptions,
                PrimaryCallback(
                    callback,
                    routeOptions
                )
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
        logger: Logger
    ) : HybridRouterHandler(primaryRouter, fallbackRouter, logger) {

        private inner class PrimaryCallback(
            private val route: DirectionsRoute,
            private val legIndex: Int,
            private val clientCallback: RouteRefreshCallback
        ) : RouteRefreshCallback {
            override fun onRefresh(directionsRoute: DirectionsRoute) {
                clientCallback.onRefresh(directionsRoute)
            }

            override fun onError(error: RouteRefreshError) {
                logger.w(
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
                logger.w(
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
                logger.e(
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
