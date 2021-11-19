package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    json: String,
    options: RouteOptions?,
    onMetadata: (String) -> Unit,
): Expected<Throwable, List<DirectionsRoute>> =
    withContext(dispatcher) {
        return@withContext try {
            val jsonObject = JSONObject(json)
            val uuid: String? = if (jsonObject.has(UUID)) {
                jsonObject.getString(UUID)
            } else {
                null
            }

            // TODO remove after https://github.com/mapbox/navigation-sdks/issues/1229
            if (jsonObject.has(METADATA)) {
                onMetadata(jsonObject.getString(METADATA))
            }

            check(jsonObject.has("routes")) {
                """route response should contain "routes" array"""
            }
            // TODO simplify when https://github.com/mapbox/mapbox-java/issues/1292 is finished
            val response = DirectionsResponse.fromJson(json, options, uuid)
            val routes = response.routes()
            check(routes.size > 0) {
                "route response should contain at least one route"
            }
            ExpectedFactory.createValue(routes)
        } catch (ex: Exception) {
            when (ex) {
                is JSONException,
                is IllegalStateException,
                is NullPointerException -> ExpectedFactory.createError(ex)
                else -> throw ex
            }
        }
    }

suspend fun parseNativeDirectionsAlternative(
    dispatcher: CoroutineDispatcher,
    json: String,
    options: RouteOptions?,
): Expected<Throwable, DirectionsRoute> =
    withContext(dispatcher) {
        return@withContext try {
            val jsonObject = JSONObject(json)
            val uuid: String? = if (jsonObject.has(UUID)) {
                jsonObject.getString(UUID)
            } else {
                null
            }

            val jsonRoutes = jsonObject.getJSONArray("routes")
            check(jsonRoutes.length() > 0) {
                "route alternatives response should contain at least one route"
            }
            val jsonRoute: DirectionsRoute = jsonRoutes.let { routesArray ->
                for (i in 0 until routesArray.length()) {
                    if (!routesArray.isNull(i)) {
                        // TODO simplify after https://github.com/mapbox/mapbox-java/issues/1292
                        return@let DirectionsRoute.fromJson(
                            routesArray.getJSONObject(i).toString(),
                            options,
                            uuid
                        ).run {
                            toBuilder()
                                .routeIndex(i.toString())
                                .build()
                        }
                    }
                }
                throw IllegalArgumentException("all alternative routes are null")
            }
            ExpectedFactory.createValue(jsonRoute)
        } catch (ex: Exception) {
            when (ex) {
                is JSONException,
                is IllegalStateException,
                is IllegalArgumentException -> ExpectedFactory.createError(ex)
                else -> throw ex
            }
        }
    }

private const val UUID = "uuid"
private const val METADATA = "metadata"
