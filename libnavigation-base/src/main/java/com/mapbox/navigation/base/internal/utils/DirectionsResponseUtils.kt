package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteAlternative
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException

suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    json: String,
    options: RouteOptions,
): Expected<Throwable, DirectionsResponse> =
    withContext(dispatcher) {
        return@withContext try {
            val response = DirectionsResponse.fromJson(json, options)
            if (response.routes().isEmpty()) {
                ExpectedFactory.createError(
                    IllegalStateException("no routes returned, collection is empty")
                )
            } else {
                ExpectedFactory.createValue(response)
            }
        } catch (ex: Exception) {
            when (ex) {
                is JSONException,
                is NullPointerException -> ExpectedFactory.createError(ex)
                else -> throw ex
            }
        }
    }

suspend fun parseNativeDirectionsAlternative(
    dispatcher: CoroutineDispatcher,
    routeAlternative: RouteAlternative
): Expected<Throwable, NavigationRoute> =
    withContext(dispatcher) {
        return@withContext try {
            val navigationRoute = NavigationRoute.create(
                directionsResponseJson = routeAlternative.route.response,
                routeRequestUrl = routeAlternative.route.request
            )[routeAlternative.route.index]
            ExpectedFactory.createValue(navigationRoute)
        } catch (ex: Exception) {
            when (ex) {
                is JSONException,
                is IllegalStateException,
                is IllegalArgumentException -> ExpectedFactory.createError(ex)
                else -> throw ex
            }
        }
    }
