package com.mapbox.navigation.base.internal.utils

import com.google.gson.JsonSyntaxException
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.route.toNavigationRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsResponse
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException

suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    responseJson: DataRef,
    requestUrl: String,
    routerOrigin: RouterOrigin,
    responseTimeElapsedSeconds: Long,
    parsingArguments: ParseArguments
): Expected<Throwable, List<NavigationRoute>> =
    withContext(dispatcher) {
        return@withContext try {
            val routes = NavigationRoute.createAsync(
                directionsResponseJson = responseJson,
                routeRequestUrl = requestUrl,
                routerOrigin,
                responseTimeElapsedSeconds,
                optimiseMemory = parsingArguments.optimiseDirectionsResponseStructure
            )
            if (routes.isEmpty()) {
                ExpectedFactory.createError(
                    IllegalStateException("no routes returned, collection is empty")
                )
            } else {
                ExpectedFactory.createValue(routes)
            }
        } catch (ex: Exception) {
            when (ex) {
                is JSONException,
                is JsonSyntaxException -> ExpectedFactory.createError(ex)
                else -> throw ex
            }
        }
    }

fun parseRouteInterfaces(
    routes: List<RouteInterface>,
    responseTimeElapsedSeconds: Long,
    parseArgs: ParseArguments
): Expected<Throwable, List<NavigationRoute>> {
    return try {
        routes.groupBy { it.responseUuid }
            .map { (_, routes) ->
                val directionsResponse = routes.first().responseJsonRef.toDirectionsResponse()
                routes.map {
                    it.toNavigationRoute(
                        responseTimeElapsedSeconds,
                        directionsResponse,
                        parseArgs.optimiseDirectionsResponseStructure
                    )
                }
            }
            .flatten()
            .sortedBy { routes.indexOf(it.nativeRoute) }
            .let { ExpectedFactory.createValue(it) }
    }
    catch (ex: Exception) {
        when (ex) {
            is JSONException,
            is IllegalStateException,
            is IllegalArgumentException -> ExpectedFactory.createError(ex)
            else -> throw ex
        }
    }
}
