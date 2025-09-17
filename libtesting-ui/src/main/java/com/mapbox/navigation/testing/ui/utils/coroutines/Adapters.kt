package com.mapbox.navigation.testing.ui.utils.coroutines

import android.util.Log
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.Location
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingFailure
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.mapmatching.MapMatchingSuccessfulResult
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationVersionSwitchObserver
import com.mapbox.navigation.core.RoutesInvalidatedObserver
import com.mapbox.navigation.core.RoutesInvalidatedParams
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.core.RoutesSetSuccess
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.routerefresh.RouteRefreshStateResult
import com.mapbox.navigation.core.routerefresh.RouteRefreshStatesObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.logD
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

fun MapboxNavigation.routesInvalidatedResults(): Flow<RoutesInvalidatedParams> {
    return loggedCallbackFlow(
        { RoutesInvalidatedObserver(it) },
        { registerRoutesInvalidatedObserver(it) },
        { unregisterRoutesInvalidatedObserver(it) },
        "RoutesInvalidatedResult"
    )
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
fun MapboxNavigation.refreshStates(): Flow<RouteRefreshStateResult> {
    return loggedCallbackFlow(
        { RouteRefreshStatesObserver(it) },
        { routeRefreshController.registerRouteRefreshStateObserver(it) },
        { routeRefreshController.unregisterRouteRefreshStateObserver(it) },
        "RouteRefreshState"
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

fun RerouteController.rerouteStates(): Flow<RerouteState> {
    return loggedCallbackFlow(
        { RerouteController.RerouteStateObserver(it) },
        { registerRerouteStateObserver(it) },
        { unregisterRerouteStateObserver(it) },
        "RerouteState"
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

sealed interface NavigationRouteAlternativesResult {
    data class OnRouteAlternatives(
        val routeProgress: RouteProgress,
        val alternatives: List<NavigationRoute>,
        @RouterOrigin
        val routerOrigin: String
    ) : NavigationRouteAlternativesResult

    data class OnRouteAlternativeError(
        val error: RouteAlternativesError
    ) : NavigationRouteAlternativesResult
}

suspend fun MapboxNavigation.requestRoutes(options: RouteOptions) =
    suspendCancellableCoroutine<RouteRequestResult> { continuation ->
        val callback = object : NavigationRouterCallback {
            override fun onRoutesReady(
                routes: List<NavigationRoute>,
                @RouterOrigin routerOrigin: String
            ) {
                continuation.resume(RouteRequestResult.Success(routes, routerOrigin))
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                continuation.resume(RouteRequestResult.Failure(reasons))
            }

            override fun onCanceled(routeOptions: RouteOptions, @RouterOrigin routerOrigin: String) {
            }
        }
        val id = requestRoutes(options, callback)
        continuation.invokeOnCancellation {
            cancelRouteRequest(id)
        }
    }

@ExperimentalPreviewMapboxNavigationAPI
sealed class MapMatchingRequestResult {
    data class Success(
        val value: MapMatchingSuccessfulResult
    ) : MapMatchingRequestResult()

    data class Failure(
        val reasons: MapMatchingFailure
    ) : MapMatchingRequestResult()

    object Cancelled: MapMatchingRequestResult()

    fun getSuccessfulOrThrowException() = (this as Success).value
}

@ExperimentalPreviewMapboxNavigationAPI
suspend fun MapboxNavigation.requestMapMatching(options: MapMatchingOptions) =
    suspendCancellableCoroutine<MapMatchingRequestResult> { continuation ->
        val callback = object : MapMatchingAPICallback {
            override fun success(result: MapMatchingSuccessfulResult) {
                continuation.resume(MapMatchingRequestResult.Success(result))
            }
            override fun failure(failure: MapMatchingFailure) {
                continuation.resume(MapMatchingRequestResult.Failure(failure))
            }
            override fun onCancel() {
            }
        }
        val id = requestMapMatching(options, callback)
        continuation.invokeOnCancellation {
            cancelMapMatchingRequest(id)
        }
    }

suspend fun MapboxNavigation.setNavigationRoutesAsync(
    routes: List<NavigationRoute>,
    initialLegIndex: Int = 0,
) = suspendCoroutine<Expected<RoutesSetError, RoutesSetSuccess>> { continuation ->
    val callback = RoutesSetCallback { result -> continuation.resume(result) }
    setNavigationRoutes(routes, initialLegIndex, callback)
}

@ExperimentalPreviewMapboxNavigationAPI
suspend fun MapboxNavigation.switchToAlternativeAsync(
    alternativeRoute: NavigationRoute,
) = suspendCancellableCoroutine<Expected<RoutesSetError, RoutesSetSuccess>> { continuation ->
    val callback = RoutesSetCallback { result -> continuation.resume(result) }
    switchToAlternativeRoute(alternativeRoute, callback)
}

sealed class RouteRequestResult {
    data class Success(
        val routes: List<NavigationRoute>,
        @RouterOrigin val routerOrigin: String
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

fun MapboxNavigation.versionSwitchObserver(): Flow<String?> = callbackFlow {
    val observer = object : NavigationVersionSwitchObserver {
        override fun onSwitchToFallbackVersion(tilesVersion: String?) {
            logD("Switch to fallback version: $tilesVersion")
            trySend(tilesVersion)
        }

        override fun onSwitchToTargetVersion(tilesVersion: String?) {
            logD("Switch to target version: $tilesVersion")
            trySend(tilesVersion)
        }
    }
    registerNavigationVersionSwitchObserver(observer)
    awaitClose { unregisterNavigationVersionSwitchObserver(observer) }
}
