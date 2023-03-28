@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mapbox.navigation.testing.ui.utils.coroutines

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.core.RoutesSetSuccess
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun MapboxNavigation.routesUpdates(): Flow<RoutesUpdatedResult> {
    return loggedCallbackFlow(
        { RoutesObserver(it) },
        { registerRoutesObserver(it) },
        { unregisterRoutesObserver(it) },
        "RoutesUpdatedResult"
    )
}

@ExperimentalPreviewMapboxNavigationAPI
fun MapboxNavigation.routesPreviewUpdates(): Flow<RoutesPreviewUpdate> {
    return loggedCallbackFlow(
        { RoutesPreviewObserver(it) },
        { registerRoutesPreviewObserver(it) },
        { unregisterRoutesPreviewObserver(it) },
        "RoutesPreviewUpdate"
    )
}

fun MapboxNavigation.routeProgressUpdates(): Flow<RouteProgress> {
    return loggedCallbackFlow(
        { RouteProgressObserver(it) },
        { registerRouteProgressObserver(it) },
        { unregisterRouteProgressObserver(it) },
        "RouteProgress"
    )
}

fun MapboxNavigation.offRouteUpdates(): Flow<Boolean> {
    return loggedCallbackFlow(
        { OffRouteObserver(it) },
        { registerOffRouteObserver(it) },
        { unregisterOffRouteObserver(it) },
        "OffRoute"
    )
}

fun MapboxNavigation.roadObjectsOnRoute(): Flow<List<UpcomingRoadObject>> {
    return loggedCallbackFlow(
        { RoadObjectsOnRouteObserver(it) },
        { registerRoadObjectsOnRouteObserver(it) },
        { unregisterRoadObjectsOnRouteObserver(it) },
        "UpcomingRoadObject"
    )
}

fun MapboxNavigation.rawLocationUpdates(): Flow<Location> {
    return loggedCallbackFlow(
        {
            object : LocationObserver {
                override fun onNewRawLocation(rawLocation: Location) {
                    it(rawLocation)
                }

                override fun onNewLocationMatcherResult(
                    locationMatcherResult: LocationMatcherResult
                ) {
                }
            }
        },
        { registerLocationObserver(it) },
        { unregisterLocationObserver(it) },
        "RawLocation"
    )
}

suspend fun MapboxNavigation.requestRoutes(options: RouteOptions) =
    suspendCancellableCoroutine<RouteRequestResult> { continuation ->
        val callback = object : NavigationRouterCallback {
            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                continuation.resume(RouteRequestResult.Success(routes, routerOrigin))
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                continuation.resume(RouteRequestResult.Failure(reasons))
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            }
        }
        val id = requestRoutes(options, callback)
        continuation.invokeOnCancellation {
            cancelRouteRequest(id)
        }
    }

suspend fun MapboxNavigation.setNavigationRoutesAsync(
    routes: List<NavigationRoute>,
    initialLegIndex: Int = 0,
) = suspendCoroutine<Expected<RoutesSetError, RoutesSetSuccess>> { continuation ->
    val callback = RoutesSetCallback { result -> continuation.resume(result) }
    setNavigationRoutes(routes, initialLegIndex, callback)
}

sealed class RouteRequestResult {
    data class Success(
        val routes: List<NavigationRoute>,
        val routerOrigin: RouterOrigin
    ) : RouteRequestResult()

    data class Failure(
        val reasons: List<RouterFailure>
    ) : RouteRequestResult()
}

sealed class SetRoutesResult {
    data class Success(
        val routes: List<NavigationRoute>,
    ) : SetRoutesResult()

    data class Failure(
        val routes: List<NavigationRoute>,
        val error: String,
    ) : SetRoutesResult()
}

fun RouteRequestResult.getSuccessfulResultOrThrowException(): RouteRequestResult.Success {
    return when (this) {
        is RouteRequestResult.Success -> this
        is RouteRequestResult.Failure -> error("result is failure: ${this.reasons}")
    }
}

suspend fun MapboxNavigation.bannerInstructions(): Flow<BannerInstructions> {
    return loggedCallbackFlow(
        { BannerInstructionsObserver(it) },
        { registerBannerInstructionsObserver(it) },
        { unregisterBannerInstructionsObserver(it) },
        "BannerInstructions"
    )
}

suspend fun MapboxNavigation.voiceInstructions(): Flow<VoiceInstructions> {
    return loggedCallbackFlow(
        { VoiceInstructionsObserver(it) },
        { registerVoiceInstructionsObserver(it) },
        { unregisterVoiceInstructionsObserver(it) },
        "VoiceInstructions"
    )
}

suspend fun MapboxHistoryRecorder.stopRecording(): String? = suspendCoroutine { cont ->
    stopRecording { path ->
        cont.resume(path)
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
suspend fun MapboxRouteLineView.renderRouteDrawDataAsync(
    map: MapboxMap,
    style: Style,
    expected: Expected<RouteLineError, RouteSetValue>
): RoutesRenderedResult = suspendCoroutine { cont ->
    renderRouteDrawData(style, expected, map, object : RoutesRenderedCallback {

        override fun onRoutesRendered(result: RoutesRenderedResult) {
            cont.resume(result)
        }
    })
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
suspend fun MapboxRouteLineView.renderClearRouteLineValueAsync(
    map: MapboxMap,
    style: Style,
    expected: Expected<RouteLineError, RouteLineClearValue>
): RoutesRenderedResult = suspendCoroutine { cont ->
    renderClearRouteLineValue(style, expected, map, object : RoutesRenderedCallback {

        override fun onRoutesRendered(result: RoutesRenderedResult) {
            cont.resume(result)
        }
    })
}

fun <T, OBSERVER> loggedCallbackFlow(
    observerCreator: (block: (T) -> Unit) -> OBSERVER,
    observerRegistrator: (OBSERVER) -> Unit,
    observerUnregistrator: (OBSERVER) -> Unit,
    tag: String,
): Flow<T> {
    return callbackFlow {
        var lastUpdate: T? = null
        val observer = observerCreator {
            lastUpdate = it
            trySend(it)
        }
        observerRegistrator(observer)
        awaitClose {
            Log.d("[$tag]", "Last update: $lastUpdate")
            observerUnregistrator(observer)
        }
    }
}

suspend fun MapboxNavigation.navigateNextRouteLeg() = suspendCancellableCoroutine<Unit> { cont ->
    navigateNextRouteLeg {
        if (!it) {
            throw IllegalStateException("Could not navigate to next leg")
        }
        cont.resume(Unit)
    }
}
