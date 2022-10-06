@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mapbox.navigation.instrumentation_tests.utils.coroutines

import android.util.Log
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.DeveloperMetadata
import com.mapbox.navigation.core.DeveloperMetadataObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.core.RoutesSetSuccess
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun MapboxNavigation.routesUpdates(): Flow<RoutesUpdatedResult> {
    val navigation = this
    return callbackFlow {
        val observer = RoutesObserver {
            trySend(it)
        }
        navigation.registerRoutesObserver(observer)
        awaitClose {
            navigation.unregisterRoutesObserver(observer)
        }
    }
}

fun MapboxNavigation.routeProgressUpdates(): Flow<RouteProgress> {
    val navigation = this
    return callbackFlow {
        val observer = RouteProgressObserver {
            trySend(it)
        }
        navigation.registerRouteProgressObserver(observer)
        awaitClose {
            navigation.unregisterRouteProgressObserver(observer)
        }
    }
}

fun MapboxNavigation.roadObjectsOnRoute(): Flow<List<UpcomingRoadObject>> {
    val navigation = this
    return callbackFlow {
        val observer = RoadObjectsOnRouteObserver {
            trySend(it)
        }
        navigation.registerRoadObjectsOnRouteObserver(observer)
        awaitClose {
            navigation.unregisterRoadObjectsOnRouteObserver(observer)
        }
    }
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
    val navigation = this
    return callbackFlow {
        val observer = BannerInstructionsObserver {
            trySend(it)
        }
        navigation.registerBannerInstructionsObserver(observer)
        awaitClose {
            navigation.unregisterBannerInstructionsObserver(observer)
        }
    }
}

suspend fun MapboxNavigation.voiceInstructions(): Flow<VoiceInstructions> {
    val navigation = this
    return callbackFlow {
        val observer = VoiceInstructionsObserver {
            trySend(it)
        }
        navigation.registerVoiceInstructionsObserver(observer)
        awaitClose {
            navigation.unregisterVoiceInstructionsObserver(observer)
        }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
fun MapboxNavigation.collectMetadata(maxItems: Int): Flow<DeveloperMetadata> {
    val navigation = this
    return callbackFlow {
        val observer = object : DeveloperMetadataObserver {
            override fun onDeveloperMetadataChanged(metadata: DeveloperMetadata) {
                Log.i("[DeveloperMetadataObserver]", "onDeveloperMetadataChanged: $metadata")
                trySend(metadata)
            }
        }
        navigation.registerDeveloperMetadataObserver(observer)
        awaitClose {
            navigation.unregisterDeveloperMetadataObserver(observer)
        }
    }.shareIn(
        MainScope(),
        replay = maxItems,
        started = SharingStarted.Eagerly
    )
}
