@file:JvmName("MapboxNavigationEx")

package com.mapbox.navigation.dropin.extensions

import android.location.Location
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow

/**
 * TODO note that each of these creates a new subscription. A concern may be that we want to have
 *   a single sharable state for these, rather than creating many subscriptions on the sdk.
 */

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowTripSessionState(): Flow<TripSessionState> = callbackFlow {
    val tripSessionStateObserver = TripSessionStateObserver { trySend(it) }
    registerTripSessionStateObserver(tripSessionStateObserver)
    awaitClose { unregisterTripSessionStateObserver(tripSessionStateObserver) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowRoutesUpdated(): Flow<RoutesUpdatedResult> = callbackFlow {
    val observer = RoutesObserver { trySend(it) }
    registerRoutesObserver(observer)
    awaitClose { unregisterRoutesObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowRouteProgress(): Flow<RouteProgress> = callbackFlow {
    val observer = RouteProgressObserver { trySend(it) }
    registerRouteProgressObserver(observer)
    awaitClose { unregisterRouteProgressObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowNewRawLocation(): Flow<Location> = callbackFlow {
    val observer = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            trySend(rawLocation)
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            // use the flowLocationMatcherResult
        }
    }
    registerLocationObserver(observer)
    awaitClose { unregisterLocationObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowLocationMatcherResult(): Flow<LocationMatcherResult> = callbackFlow {
    val observer = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // use the flowNewRawLocation
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            trySend(locationMatcherResult)
        }
    }
    registerLocationObserver(observer)
    awaitClose { unregisterLocationObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowVoiceInstructions() = channelFlow {
    val voiceInstructionsObserver = VoiceInstructionsObserver { trySend(it) }
    registerVoiceInstructionsObserver(voiceInstructionsObserver)
    awaitClose { unregisterVoiceInstructionsObserver(voiceInstructionsObserver) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowOnFinalDestinationArrival(): Flow<RouteProgress> = callbackFlow {
    val observer = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) = Unit
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) = Unit
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            trySend(routeProgress)
        }
    }
    registerArrivalObserver(observer)
    awaitClose { unregisterArrivalObserver(observer) }
}
