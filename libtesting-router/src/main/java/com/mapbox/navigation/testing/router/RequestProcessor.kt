// ktlint-disable filename
@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.testing.router

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal sealed class RequestProcessingResult {
    class Failure(
        val code: Int,
        val body: String,
    ) : RequestProcessingResult() {
        companion object {
            fun wrongInput(message: String? = null) = Failure(
                422,
                "Wrong input" + message?.let { ": $it" }.orEmpty(),
            )

            fun mockServerError(message: String) = Failure(
                500,
                message,
            )
        }
    }

    class GetRouteResponse(
        val response: DirectionsResponse,
    ) : RequestProcessingResult()

    class RefreshRouteResponse(
        val response: DirectionsRefreshResponse,
    ) : RequestProcessingResult()

    object RequestNotSupported : RequestProcessingResult()
}

internal suspend fun processRequest(
    router: MapboxNavigationTestRouter,
    refresher: MapboxNavigationTestRouteRefresher,
    url: String,
): RequestProcessingResult {
    val parsedUrl = url.toHttpUrlOrNull() ?: return RequestProcessingResult.RequestNotSupported
    val routeOptions = parseRouteOptionsOrNull(parsedUrl)
    if (routeOptions != null) {
        return processRouteRequest(router, routeOptions)
    }

    val routeRefreshRequest = parseRouteRefreshRequestOrNull(parsedUrl)
    if (routeRefreshRequest != null) {
        return processRouteRefreshRequest(refresher, routeRefreshRequest)
    }
    return RequestProcessingResult.RequestNotSupported
}

private suspend fun processRouteRefreshRequest(
    refresher: MapboxNavigationTestRouteRefresher,
    routeRefreshRequest: RouteRefreshRequest,
): RequestProcessingResult {
    val refreshResult = refresher.refreshRoute(
        RefreshOptions(
            routeRefreshRequest.routeUUID,
            routeRefreshRequest.routeIndex,
        ),
    )
    return when (refreshResult) {
        is RefreshRouteResult.Failure -> RequestProcessingResult.Failure(
            refreshResult.failure.httpCode,
            refreshResult.failure.errorBody,
        )

        is RefreshRouteResult.Success -> {
            if (refreshResult.refreshedRoute.legs() == null) {
                RequestProcessingResult.Failure.mockServerError("refreshed route doesn't have legs")
            } else if (routeRefreshRequest.legIndex > refreshResult.refreshedRoute.legs()!!.size) {
                RequestProcessingResult.Failure.wrongInput(
                    "leg index size is ${routeRefreshRequest.legIndex}, " +
                        "while route has ${refreshResult.refreshedRoute.legs()!!.size} legs",
                )
            } else {
                val legGeometryIndex = calculateCurrentLegGeometryIndexOrNull(
                    refreshResult.refreshedRoute,
                    routeRefreshRequest.legIndex,
                    routeRefreshRequest.currentGeometryIndex,
                ) ?: return RequestProcessingResult.Failure(
                    500,
                    "Internal error: can't identify refreshed leg geometry index",
                )
                val directionsResponse: DirectionsRefreshResponse = buildDirectionsResponse(
                    refreshResult.refreshedRoute,
                    routeRefreshRequest.legIndex,
                    legGeometryIndex,
                )
                RequestProcessingResult.RefreshRouteResponse(directionsResponse)
            }
        }
    }
}

private suspend fun processRouteRequest(
    router: MapboxNavigationTestRouter,
    routeOptions: RouteOptions,
): RequestProcessingResult {
    val response = when (val routerResult = router.requestRoute(routeOptions)) {
        is GetRouteResult.Failure -> RequestProcessingResult.Failure(
            routerResult.failure.httpCode,
            routerResult.failure.errorBody,
        )

        is GetRouteResult.Success -> RequestProcessingResult.GetRouteResponse(
            routerResult.response,
        )
    }
    return response
}

private data class RouteRefreshRequest(
    val routeUUID: String,
    val routeIndex: Int,
    val legIndex: Int,
    val currentGeometryIndex: Int,
)

private fun parseRouteRefreshRequestOrNull(url: HttpUrl): RouteRefreshRequest? {
    val expectedPrefix = "/directions-refresh/v1/mapbox/driving-traffic/"
    if (url.encodedPath.startsWith(expectedPrefix)) {
        val segments = url.encodedPathSegments
        if (segments.size < 7) return null
        val routeUUID = segments[4]
        val routeIndex = segments[5].toIntOrNull() ?: return null
        val legIndex = segments[6].toIntOrNull() ?: return null
        val currentGeometryIndex = url
            .queryParameter("current_route_geometry_index")?.toIntOrNull()
            ?: return null
        return RouteRefreshRequest(
            routeUUID = routeUUID,
            routeIndex = routeIndex,
            legIndex = legIndex,
            currentGeometryIndex = currentGeometryIndex,
        )
    }
    return null
}

private fun parseRouteOptionsOrNull(url: HttpUrl): RouteOptions? {
    val routeOptions = try {
        val options = RouteOptions.fromUrl(url.toUrl())
        options.coordinatesList() // make sure that coordinates could be parsed
        options
    } catch (t: Throwable) {
        null
    }
    return routeOptions
}

private fun buildDirectionsResponse(
    refreshedRoute: DirectionsRoute,
    refreshedRouteLegIndex: Int,
    refreshedLegGeometryIndex: Int,
): DirectionsRefreshResponse {
    val result = DirectionsRefreshResponse.builder()
        .route(
            DirectionsRouteRefresh.builder()
                .legs(
                    buildRefreshedLegs(
                        refreshedRoute.legs(),
                        refreshedRouteLegIndex,
                        refreshedLegGeometryIndex,
                    ),
                )
                .build(),
        )
        .code("Ok")
        .build()
    return result
}

private fun buildRefreshedLegs(
    routeLegs: List<RouteLeg>?,
    refreshedRouteLegIndex: Int,
    refreshedLegGeometryIndex: Int,
) = routeLegs?.mapIndexedNotNull { legIndex, routeLeg ->
    val traveledGeometry = when (legIndex) {
        in Int.MIN_VALUE until refreshedRouteLegIndex ->
            return@mapIndexedNotNull null

        refreshedRouteLegIndex -> refreshedLegGeometryIndex
        else -> 0
    }
    RouteLegRefresh.builder()
        .annotation(
            buildRefreshedAnnotation(
                routeLeg.annotation(),
                traveledGeometry,
            ),
        )
        .incidents(
            buildRefreshedIncidents(
                routeLeg.incidents(),
                traveledGeometry,
            ),
        )
        .closures(
            buildRefreshedClosures(
                routeLeg.closures(),
                traveledGeometry,
            ),
        )
        .build()
}

private fun buildRefreshedClosures(
    closures: List<Closure>?,
    traveledGeometry: Int,
) = closures?.map {
    it.toBuilder()
        .geometryIndexEnd(
            it.geometryIndexEnd() - traveledGeometry,
        )
        .geometryIndexStart(
            it.geometryIndexStart() - traveledGeometry,
        )
        .build()
}?.filter {
    it.geometryIndexEnd() >= 0 &&
        it.geometryIndexStart() >= 0
}

private fun buildRefreshedIncidents(
    incidents: List<Incident>?,
    traveledGeometry: Int,
) = incidents?.map {
    it.toBuilder()
        .geometryIndexEnd(
            it.geometryIndexEnd()
                ?.let { it - traveledGeometry },
        )
        .geometryIndexStart(
            it.geometryIndexStart()
                ?.let { it - traveledGeometry },
        )
        .build()
}?.filter {
    (it.geometryIndexEnd() ?: 0) >= 0 &&
        (it.geometryIndexStart() ?: 0) >= 0
}

private fun buildRefreshedAnnotation(
    legAnnotation: LegAnnotation?,
    traveledGeometry: Int,
) = LegAnnotation
    .builder()
    .duration(
        legAnnotation?.duration()
            ?.drop(traveledGeometry),
    )
    .distance(
        legAnnotation?.distance()
            ?.drop(traveledGeometry),
    )
    .speed(
        legAnnotation?.speed()?.drop(traveledGeometry),
    )
    .maxspeed(
        legAnnotation?.maxspeed()
            ?.drop(traveledGeometry),
    )
    .congestion(
        legAnnotation?.congestion()
            ?.drop(traveledGeometry),
    )
    .congestionNumeric(
        legAnnotation?.congestionNumeric()
            ?.drop(traveledGeometry),
    )
    .freeflowSpeed(
        legAnnotation?.freeflowSpeed()
            ?.drop(traveledGeometry),
    )
    .currentSpeed(
        legAnnotation?.currentSpeed()
            ?.drop(traveledGeometry),
    )
    .unrecognizedJsonProperties(
        legAnnotation?.unrecognizedJsonProperties?.map { (k, v) ->
            val newValue: JsonElement = if (v.isJsonArray) {
                v.asJsonArray.drop(traveledGeometry)
                    .let {
                        val array = JsonArray(it.size)
                        it.forEachIndexed { _, jsonElement ->
                            array.add(
                                jsonElement,
                            )
                        }
                        array
                    }
            } else {
                v
            }
            k to newValue
        }?.toMap(),
    )
    .build()

private fun calculateCurrentLegGeometryIndexOrNull(
    route: DirectionsRoute,
    currentLegIndex: Int,
    currentRouteGeometryIndex: Int,
): Int? {
    val geometryLengthOfPassedLegs = route
        .legs()!!
        .take(currentLegIndex)
        .sumOf { getAnnotationLengthOrNull(it.annotation()) ?: return null }
    val currentLegGeometryIndex =
        (currentRouteGeometryIndex - geometryLengthOfPassedLegs).let {
            if (it < 0) 0 else it
        }
    return currentLegGeometryIndex
}

private fun getAnnotationLengthOrNull(annotation: LegAnnotation?): Int? {
    return if (annotation != null) {
        annotation.congestionNumeric()?.size
            ?: annotation.congestion()?.size
            ?: annotation.speed()?.size
            ?: annotation.distance()?.size
            ?: annotation.duration()?.size
            ?: annotation.maxspeed()?.size
            ?: annotation.trafficTendency()?.size
    } else {
        null
    }
}

private suspend fun MapboxNavigationTestRouteRefresher.refreshRoute(options: RefreshOptions) =
    suspendCoroutine { continuation ->
        getRouteRefresh(
            options,
            object : RouteRefreshCallback {
                override fun onRefresh(directionsRoute: DirectionsRoute) {
                    continuation.resume(RefreshRouteResult.Success(directionsRoute))
                }

                override fun onFailure(failure: TestRefresherFailure) {
                    continuation.resume(RefreshRouteResult.Failure(failure))
                }
            },
        )
    }

private suspend fun MapboxNavigationTestRouter.requestRoute(options: RouteOptions) =
    suspendCoroutine { continuation ->
        getRoute(
            options,
            object : RouterCallback {
                override fun onRoutesReady(response: DirectionsResponse) {
                    continuation.resume(GetRouteResult.Success(response))
                }

                override fun onFailure(failure: TestRouterFailure) {
                    continuation.resume(GetRouteResult.Failure(failure))
                }
            },
        )
    }

private sealed class GetRouteResult {
    data class Success(
        val response: DirectionsResponse,
    ) : GetRouteResult()

    data class Failure(
        val failure: TestRouterFailure,
    ) : GetRouteResult()
}

private sealed class RefreshRouteResult {
    data class Success(
        val refreshedRoute: DirectionsRoute,
    ) : RefreshRouteResult()

    data class Failure(
        val failure: TestRefresherFailure,
    ) : RefreshRouteResult()
}
