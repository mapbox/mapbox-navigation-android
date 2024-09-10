@file:JvmName("MapboxNavigationExtensions")

package com.mapbox.navigation.core.internal.extensions

import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesSetStartedParams
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.directions.session.SetNavigationRoutesStartedObserver
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
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
 * Register [HistoryRecordingStateChangeObserver]. Use this method to receive notifications
 * regarding history recording: when to start, stop or cancel recording
 * to have each trip session (Free Drive and Active Guidance) recorded independently.
 * NOTE: if there is a session running when the observer is being registered,
 * it will be notified via [HistoryRecordingStateChangeObserver.onShouldStartRecording]
 * with the current session as an argument.
 *
 * @param observer callback to receive notifications.
 */
@UiThread
fun MapboxNavigation.registerHistoryRecordingStateChangeObserver(
    observer: HistoryRecordingStateChangeObserver,
) {
    historyRecordingStateHandler.registerStateChangeObserver(observer)
}

/**
 * Unregister [HistoryRecordingStateChangeObserver].
 * See [MapboxNavigation.registerHistoryRecordingStateChangeObserver] for more info.
 *
 * @param observer callback to stop receiving notifications.
 */
@UiThread
fun MapboxNavigation.unregisterHistoryRecordingStateChangeObserver(
    observer: HistoryRecordingStateChangeObserver,
) {
    historyRecordingStateHandler.unregisterStateChangeObserver(observer)
}

fun MapboxNavigation.retrieveCopilotHistoryRecorder(): MapboxHistoryRecorder =
    copilotHistoryRecorder

fun MapboxNavigation.retrieveCompositeHistoryRecorder(): MapboxHistoryRecorder =
    compositeRecorder

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
fun MapboxNavigation.flowVoiceInstructions(): Flow<VoiceInstructions> = channelFlow {
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

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowOnWaypointArrival(): Flow<RouteProgress> = callbackFlow {
    val observer = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) {
            trySend(routeProgress)
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) = Unit
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) = Unit
    }
    registerArrivalObserver(observer)
    awaitClose { unregisterArrivalObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowOnNextRouteLegStart(): Flow<RouteLegProgress> = callbackFlow {
    val observer = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) = Unit
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            trySend(routeLegProgress)
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) = Unit
    }
    registerArrivalObserver(observer)
    awaitClose { unregisterArrivalObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowNavigationSessionState(): Flow<NavigationSessionState> = callbackFlow {
    val observer = NavigationSessionStateObserver { trySend(it) }
    registerNavigationSessionStateObserver(observer)
    awaitClose { unregisterNavigationSessionStateObserver(observer) }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun MapboxNavigation.flowSetNavigationRoutesStarted(): Flow<RoutesSetStartedParams> =
    callbackFlow {
        val observer = SetNavigationRoutesStartedObserver { trySend(it) }
        registerOnRoutesSetStartedObserver(observer)
        awaitClose { unregisterOnRoutesSetStartedObserver(observer) }
    }

interface HistoryRecordingEnabledObserver {

    fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder)

    fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder)
}

fun MapboxNavigation.registerHistoryRecordingEnabledObserver(
    observer: HistoryRecordingEnabledObserver,
) {
    registerHistoryRecordingEnabledObserver(observer)
}

fun MapboxNavigation.unregisterHistoryRecordingEnabledObserver(
    observer: HistoryRecordingEnabledObserver,
) {
    unregisterHistoryRecordingEnabledObserver(observer)
}

fun MapboxNavigation.flowRerouteState(
    onSubscription: () -> Unit = { },
): Flow<RerouteState>? {
    val rerouteController = getRerouteController() ?: return null
    return callbackFlow {
        val observer = RerouteController.RerouteStateObserver { trySend(it) }
        rerouteController.registerRerouteStateObserver(observer)
        onSubscription()
        awaitClose { rerouteController.unregisterRerouteStateObserver(observer) }
    }
}
