package com.mapbox.navigation.ui.voice.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest

/**
 * This class converts [MapboxNavigation] callback streams into [Flow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapboxVoiceInstructions {

    private val voiceInstructionsFlow =
        MutableStateFlow(MapboxVoiceInstructionsState(true, null))
    private val routesFlow = MutableStateFlow<List<NavigationRoute>>(emptyList())
    private val tripSessionStateFlow = MutableStateFlow(TripSessionState.STOPPED)

    private val voiceInstructionsObserver = VoiceInstructionsObserver {
        voiceInstructionsFlow.value = MapboxVoiceInstructionsState(true, it)
    }
    private val routesObserver = RoutesObserver {
        routesFlow.value = it.navigationRoutes
        if (it.navigationRoutes.isEmpty()) {
            voiceInstructionsFlow.value = MapboxVoiceInstructionsState()
        }
    }
    private val tripSessionStateObserver = TripSessionStateObserver {
        tripSessionStateFlow.value = it
        if (it == TripSessionState.STOPPED) {
            voiceInstructionsFlow.value = MapboxVoiceInstructionsState()
        }
    }

    fun registerObservers(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    fun unregisterObservers(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)

        resetFlows()
    }

    fun voiceInstructions(): Flow<State> = voiceInstructionsFlow

    fun voiceLanguage(): Flow<String?> {
        return routesFlow
            .mapLatest { it.firstOrNull()?.directionsRoute?.voiceLanguage() }
    }

    private fun resetFlows() {
        voiceInstructionsFlow.value = MapboxVoiceInstructionsState(true, null)
        routesFlow.value = emptyList()
        tripSessionStateFlow.value = TripSessionState.STOPPED
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
