package com.mapbox.androidauto.navigation.audioguidance.impl

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

/**
 * This class converts [MapboxNavigation] callback streams into [Flow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapboxVoiceInstructions(
    val mapboxNavigation: MapboxNavigation
) {
    fun voiceInstructions(): Flow<State> {
        return tripSessionStateFlow()
            .flatMapLatest { tripSessionState ->
                if (tripSessionState == TripSessionState.STARTED) {
                    routesUpdatedResultToVoiceInstructions()
                } else {
                    flowOf(MapboxVoiceInstructionsState(false, null))
                }
            }
    }

    fun voiceLanguage(): Flow<String?> {
        return routesFlow()
            .mapLatest { it.firstOrNull()?.directionsRoute?.voiceLanguage() }
            .onStart { emit(value = null) }
    }

    private fun routesUpdatedResultToVoiceInstructions(): Flow<State> {
        return routesFlow()
            .distinctUntilChangedBy { it.isEmpty() }
            .flatMapLatest { routes ->
                if (routes.isNotEmpty()) {
                    voiceInstructionsFlow().distinctUntilChanged()
                } else {
                    flowOf(MapboxVoiceInstructionsState(false, null))
                }
            }
    }

    private fun tripSessionStateFlow() = channelFlow {
        val tripSessionStateObserver = TripSessionStateObserver { tripSessionState ->
            trySend(tripSessionState)
        }
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
        awaitClose {
            mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        }
    }

    private fun routesFlow() = channelFlow {
        val routesObserver = RoutesObserver { routesUpdatedResult ->
            trySend(routesUpdatedResult.navigationRoutes)
        }
        mapboxNavigation.registerRoutesObserver(routesObserver)
        awaitClose {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
        }
    }.onStart { emit(emptyList()) }

    private fun voiceInstructionsFlow() = channelFlow {
        val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
            trySend(MapboxVoiceInstructionsState(true, voiceInstructions))
        }
        trySend(MapboxVoiceInstructionsState(true, null))
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        awaitClose {
            mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            trySend(MapboxVoiceInstructionsState(false, null))
        }
    }

    interface State {
        val isPlayable: Boolean
        val voiceInstructions: VoiceInstructions?
    }
}

@VisibleForTesting
internal data class MapboxVoiceInstructionsState(
    override val isPlayable: Boolean = false,
    override val voiceInstructions: VoiceInstructions? = null
) : MapboxVoiceInstructions.State
