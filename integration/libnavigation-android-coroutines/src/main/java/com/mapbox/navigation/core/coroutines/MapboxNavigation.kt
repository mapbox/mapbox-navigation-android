package com.mapbox.navigation.core.coroutines

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.coroutines.values.RequestAlternativesError
import com.mapbox.navigation.core.coroutines.values.RequestRoutesError
import com.mapbox.navigation.core.coroutines.values.RouteAlternatives
import com.mapbox.navigation.core.coroutines.values.RoutesWithOrigin
import com.mapbox.navigation.core.routealternatives.RouteAlternativesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

suspend fun MapboxNavigation.requestRoutes(routeOptions: RouteOptions): RoutesWithOrigin =
    suspendCancellableCoroutine { continuation ->
        requestRoutes(routeOptions, object : RouterCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
                val value = RoutesWithOrigin(routes, routerOrigin)
                continuation.resumeWith(Result.success(value))
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                val error = RequestRoutesError(
                    reasons,
                    """
                        Route request failed with:
                        $reasons
                    """.trimIndent()
                )
                continuation.resumeWithException(error)
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                continuation.cancel()
            }
        })
    }

suspend fun MapboxNavigation.requestAlternativeRoutes(): RouteAlternatives =
    suspendCancellableCoroutine { continuation ->
        requestAlternativeRoutes(object : RouteAlternativesRequestCallback {
            override fun onRouteAlternativeRequestFinished(
                routeProgress: RouteProgress,
                alternatives: List<DirectionsRoute>,
                routerOrigin: RouterOrigin
            ) {
                val value = RouteAlternatives(routeProgress, alternatives, routerOrigin)
                continuation.resumeWith(Result.success(value))
            }

            override fun onRouteAlternativesAborted(message: String) {
                val error = RequestAlternativesError(message)
                continuation.resumeWithException(error)
            }
        })
    }

@ExperimentalCoroutinesApi
fun MapboxNavigation.locationFlow(): Flow<LocationMatcherResult> =
    callbackFlow {
        val callback = object : LocationObserver {
            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                trySend(locationMatcherResult)
            }

            override fun onNewRawLocation(rawLocation: Location) = Unit
        }

        registerLocationObserver(callback)
        awaitClose { unregisterLocationObserver(callback) }
    }.buffer(Channel.RENDEZVOUS)

@ExperimentalCoroutinesApi
fun MapboxNavigation.rawLocationFlow(): Flow<Location> =
    callbackFlow {
        val observer = object : LocationObserver {
            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) =
                Unit

            override fun onNewRawLocation(rawLocation: Location) {
                trySend(rawLocation)
            }
        }

        registerLocationObserver(observer)
        awaitClose {
            unregisterLocationObserver(observer)
        }
    }.buffer(Channel.RENDEZVOUS)

@ExperimentalCoroutinesApi
fun MapboxNavigation.routeProgressFlow(): Flow<RouteProgress> =
    callbackFlow {
        val observer = RouteProgressObserver { trySend(it) }
        registerRouteProgressObserver(observer)
        awaitClose {
            unregisterRouteProgressObserver(observer)
        }
    }.buffer(Channel.RENDEZVOUS)

