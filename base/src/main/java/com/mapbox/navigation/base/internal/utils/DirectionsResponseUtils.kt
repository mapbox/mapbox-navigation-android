package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.toNavigationRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    responseJson: DataRef,
    requestUrl: String,
    @RouterOrigin routerOrigin: String,
    responseTimeElapsedMillis: Long,
): Expected<Throwable, RoutesResponse> =
    withContext(dispatcher) {
        return@withContext try {
            val response = PerformanceTracker.trackPerformanceAsync("NavigationRoute#createAsync") {
                NavigationRoute.createAsync(
                    directionsResponseJson = responseJson,
                    routeRequestUrl = requestUrl,
                    routerOrigin,
                    responseTimeElapsedMillis,
                )
            }

            if (response.routes.isEmpty()) {
                ExpectedFactory.createError(
                    IllegalStateException("no routes returned, collection is empty"),
                )
            } else {
                ExpectedFactory.createValue(response)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (ex: Throwable) {
            logE { "Route parsing failed: ${ex.message}" }
            ExpectedFactory.createError(ex)
        }
    }

fun parseRouteInterfaces(
    routes: List<RouteInterface>,
    responseTimeElapsedSeconds: Long,
    routeLookup: (routeId: String) -> NavigationRoute?,
    routeToDirections: (route: RouteInterface) -> DirectionsResponse,
): Expected<Throwable, List<NavigationRoute>> {
    return try {
        routes.groupBy { it.responseUuid }
            .map { (_, routes) ->
                val directionsResponse by lazy {
                    routeToDirections(routes.first())
                }

                routes.map {
                    routeLookup(it.routeId) ?: it.toNavigationRoute(
                        responseTimeElapsedSeconds,
                        directionsResponse,
                    )
                }
            }
            .flatten()
            .sortedBy { routes.indexOf(it.nativeRoute) }
            .let { ExpectedFactory.createValue(it) }
    } catch (ex: Throwable) {
        logE { "Alternative route parsing failed: ${ex.message}" }
        ExpectedFactory.createError(ex)
    }
}
