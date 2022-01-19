package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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
    json: String,
    options: RouteOptions,
): Expected<Throwable, NavigationRoute> =
    withContext(dispatcher) {
        return@withContext try {
            // todo remove after https://github.com/mapbox/mapbox-navigation-native/issues/5142
            // replacing all nulls that are used to pad the response array with fake routes
            // because DirectionsResponse#routes cannot have null values
            val jsonObject = JSONObject(json)
            val jsonRoutes = jsonObject.getJSONArray(ROUTES_RESPONSE_FIELD)
            var alternativeRouteIndex: Int? = null
            val nonNullRoutes = JSONArray()
            for (i in 0 until jsonRoutes.length()) {
                val nonNullRoute = if (!jsonRoutes.isNull(i)) {
                    check(alternativeRouteIndex == null) {
                        "alternative route object should have only one route"
                    }
                    alternativeRouteIndex = i
                    jsonRoutes[i]
                } else {
                    // build a fake route to fill up the index gap in the response
                    JSONObject(
                        DirectionsRoute.builder()
                            .distance(0.0)
                            .duration(0.0)
                            .build()
                            .toJson()
                    )
                }
                nonNullRoutes.put(i, nonNullRoute)
            }
            jsonObject.put(ROUTES_RESPONSE_FIELD, nonNullRoutes)

            check(alternativeRouteIndex != null) {
                "alternative route object contains no routes or " +
                    "all padded routes in the alternative route object are null"
            }

            val nonNullJsonResponse = jsonObject.toString()
            val navigationRoute = NavigationRoute(
                directionsResponse = DirectionsResponse.fromJson(nonNullJsonResponse),
                routeIndex = alternativeRouteIndex,
                routeOptions = options,
            )
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

private const val ROUTES_RESPONSE_FIELD = "routes"
