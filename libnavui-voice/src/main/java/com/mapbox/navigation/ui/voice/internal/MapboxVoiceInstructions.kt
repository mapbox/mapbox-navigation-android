package com.mapbox.navigation.ui.voice.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
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

    private val reasonsWithoutNewInstructions = setOf(
        RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
        RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
    )

    private val voiceInstructionsFlow =
        MutableStateFlow(MapboxVoiceInstructionsState(true, false, null))
    private val routesFlow = MutableStateFlow<List<NavigationRoute>>(emptyList())

    private var hasFirstVoiceInstruction = false

    private val voiceInstructionsObserver = VoiceInstructionsObserver {
        voiceInstructionsFlow.value = MapboxVoiceInstructionsState(
            true,
            !hasFirstVoiceInstruction,
            it
        )
        hasFirstVoiceInstruction = true
    }
    private val routesObserver = RoutesObserver {
        if (it.reason !in reasonsWithoutNewInstructions) {
            hasFirstVoiceInstruction = false
        }
        routesFlow.value = it.navigationRoutes
        if (it.navigationRoutes.isEmpty()) {
            voiceInstructionsFlow.value = MapboxVoiceInstructionsState()
        }
    }
    private val tripSessionStateObserver = TripSessionStateObserver {
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
        voiceInstructionsFlow.value = MapboxVoiceInstructionsState(true, false, null)
        hasFirstVoiceInstruction = false
        routesFlow.value = emptyList()
    }

    interface State {
        val isPlayable: Boolean
        val isFirst: Boolean
        val voiceInstructions: VoiceInstructions?
    }
}

@VisibleForTesting
internal data class MapboxVoiceInstructionsState(
    override val isPlayable: Boolean = false,
    override val isFirst: Boolean = false,
    override val voiceInstructions: VoiceInstructions? = null
) : MapboxVoiceInstructions.State
