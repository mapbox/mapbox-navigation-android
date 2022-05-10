package com.mapbox.androidauto.navigation.audioguidance.impl

import com.mapbox.androidauto.configuration.CarAppConfigOwner
import com.mapbox.androidauto.datastore.CarAppDataStoreOwner
import com.mapbox.androidauto.datastore.StoreAudioGuidanceMuted
import com.mapbox.androidauto.navigation.audioguidance.MapboxAudioGuidance
import com.mapbox.androidauto.navigation.audioguidance.MapboxAudioGuidanceServices
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Implementation of [MapboxAudioGuidance]. See interface for details.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioGuidanceImpl(
    private val audioGuidanceServices: MapboxAudioGuidanceServices,
    private val carAppDataStore: CarAppDataStoreOwner,
    private val carAppConfigOwner: CarAppConfigOwner,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) : MapboxAudioGuidance {

    private val internalStateFlow = MutableStateFlow(MapboxAudioGuidanceState())
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var job: Job? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        job = scope.launch { audioGuidanceFlow(mapboxNavigation).collect() }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        job?.cancel()
        job = null
    }

    /**
     * This flow gives you access to the state of mapbox audio guidance without effecting state.
     *
     * In order to enable voice guidance, you must call [MapboxNavigation.startTripSession]
     * and set a route for active guidance through [MapboxNavigation.setRoutes].
     *
     * You can also control audio guidance by calling [mute], [unmute] or [toggle]
     */
    override fun stateFlow(): StateFlow<MapboxAudioGuidance.State> = internalStateFlow

    /**
     * Explicit call to mute the audio guidance state.
     */
    override fun mute() {
        carAppDataStore.launch {
            carAppDataStore.write(StoreAudioGuidanceMuted, true)
        }
    }

    /**
     * Explicit call to unmute the audio guidance state.
     */
    override fun unmute() {
        carAppDataStore.launch {
            carAppDataStore.write(StoreAudioGuidanceMuted, false)
        }
    }

    /**
     * Toggle the muted state. E.g., if audio is muted, make it unmuted.
     */
    override fun toggle() {
        carAppDataStore.launch {
            if (carAppDataStore.read(StoreAudioGuidanceMuted).first()) {
                unmute()
            } else {
                mute()
            }
        }
    }

    /**
     * Top level flow that will switch based on the language and muted state.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun audioGuidanceFlow(
        mapboxNavigation: MapboxNavigation
    ): Flow<MapboxAudioGuidance.State> {
        return combine(
            audioGuidanceServices.mapboxVoiceInstructions(mapboxNavigation).voiceLanguage(),
            carAppConfigOwner.language(),
        ) { voiceLanguage, deviceLanguage -> voiceLanguage ?: deviceLanguage }
            .distinctUntilChanged()
            .flatMapLatest { language ->
                val audioGuidance =
                    audioGuidanceServices.mapboxAudioGuidanceVoice(mapboxNavigation, language)
                carAppDataStore.read(StoreAudioGuidanceMuted).flatMapLatest { isMuted ->
                    if (isMuted) {
                        silentFlow(mapboxNavigation)
                    } else {
                        speechFlow(mapboxNavigation, audioGuidance)
                    }
                }
            }
    }

    /**
     * This flow will monitor navigation state to determine if audio is available.
     */
    private fun silentFlow(mapboxNavigation: MapboxNavigation): Flow<MapboxAudioGuidance.State> {
        return audioGuidanceServices.mapboxVoiceInstructions(mapboxNavigation).voiceInstructions()
            .map { state ->
                internalStateFlow.updateAndGet {
                    MapboxAudioGuidanceState(
                        isMuted = true,
                        isPlayable = state.isPlayable,
                        voiceInstructions = state.voiceInstructions
                    )
                }
            }
    }

    /**
     * The same as the [silentFlow] except that it will speak announcements.
     */
    @OptIn(FlowPreview::class)
    private fun speechFlow(
        mapboxNavigation: MapboxNavigation,
        audioGuidance: MapboxAudioGuidanceVoice,
    ): Flow<MapboxAudioGuidance.State> {
        return audioGuidanceServices.mapboxVoiceInstructions(mapboxNavigation).voiceInstructions()
            .flatMapConcat { voice ->
                internalStateFlow.updateAndGet {
                    MapboxAudioGuidanceState(
                        isMuted = false,
                        isPlayable = voice.isPlayable,
                        voiceInstructions = voice.voiceInstructions
                    )
                }
                audioGuidance.speak(voice.voiceInstructions)
            }
            .map { speechAnnouncement ->
                internalStateFlow.updateAndGet { it.copy(speechAnnouncement = speechAnnouncement) }
            }
    }
}

private data class MapboxAudioGuidanceState(
    override val isPlayable: Boolean = false,
    override val isMuted: Boolean = false,
    override val voiceInstructions: VoiceInstructions? = null,
    override val speechAnnouncement: SpeechAnnouncement? = null,
) : MapboxAudioGuidance.State
