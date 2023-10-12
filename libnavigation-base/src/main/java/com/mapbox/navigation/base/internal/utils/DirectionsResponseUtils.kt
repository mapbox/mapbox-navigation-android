package com.mapbox.navigation.base.internal.utils

import android.util.Log
import com.google.gson.JsonSyntaxException
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.route.toNavigationRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsResponse
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException

suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    responseJson: DataRef,
    requestUrl: String,
    routerOrigin: RouterOrigin,
    responseTimeElapsedSeconds: Long
): Expected<Throwable, List<NavigationRoute>> =
    withContext(dispatcher) {
        return@withContext try {
            val routes = NavigationRoute.createAsync(
                directionsResponseJson = responseJson,
                routeRequestUrl = requestUrl,
                routerOrigin,
                responseTimeElapsedSeconds
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

// this function assumes that all interfaces are generated to the same
fun parseRouteInterfaces(
    routes: List<RouteInterface>,
    responseTimeElapsedSeconds: Long
): Expected<Throwable, List<NavigationRoute>> {
    if (routes.isEmpty()) {
        return ExpectedFactory.createValue(emptyList())
    }
    return try {
        Log.d("vadzim-test", "parsing directions response from the first alternative")
        val directionsResponse = routes.first().responseJsonRef.toDirectionsResponse()
        Log.d("vadzim-test", "finished parsing response from the first alternative")
        return ExpectedFactory.createValue(routes.map {
            it.toNavigationRoute(responseTimeElapsedSeconds, directionsResponse)
        })
    } catch (ex: Exception) {
        when (ex) {
            is JSONException,
            is IllegalStateException,
            is IllegalArgumentException -> ExpectedFactory.createError(ex)
            else -> throw ex
        }
    }
}
