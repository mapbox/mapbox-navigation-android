package com.mapbox.navigation.ui.voice.api

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.utils.internal.datastore.booleanDataStoreKey
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceServices
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Implementation of [MapboxAudioGuidance]. See interface for details.
 */
class MapboxAudioGuidance
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val audioGuidanceServices: MapboxAudioGuidanceServices,
    dispatcher: CoroutineDispatcher,
) : MapboxNavigationObserver {

    private var dataStoreOwner: NavigationDataStoreOwner? = null
    private var configOwner: NavigationConfigOwner? = null
    private var mutedStateFlow = MutableStateFlow(false)
    private val internalStateFlow = MutableStateFlow(MapboxAudioGuidanceState())
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mapboxVoiceInstructions = audioGuidanceServices.mapboxVoiceInstructions()

    private var job: Job? = null

    /**
     * @see [MapboxNavigationApp]
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val context = mapboxNavigation.navigationOptions.applicationContext
        dataStoreOwner = audioGuidanceServices.dataStoreOwner(context)
        configOwner = audioGuidanceServices.configOwner(context)
        mapboxVoiceInstructions.registerObservers(mapboxNavigation)
        job = scope.launch {
            restoreMutedState()
            audioGuidanceFlow(mapboxNavigation).collect()
        }
    }

    /**
     * @see [MapboxNavigationApp]
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxVoiceInstructions.unregisterObservers(mapboxNavigation)
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
    fun stateFlow(): StateFlow<MapboxAudioGuidanceState> = internalStateFlow

    /**
     * Explicit call to mute the audio guidance state.
     */
    fun mute() {
        scope.launch {
            setMutedState(true)
        }
    }

    /**
     * Explicit call to unmute the audio guidance state.
     */
    fun unmute() {
        scope.launch {
            setMutedState(false)
        }
    }

    /**
     * Toggle the muted state. E.g., if audio is muted, make it unmuted.
     */
    fun toggle() {
        scope.launch {
            if (mutedStateFlow.value) {
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
    ): Flow<MapboxAudioGuidanceState> {
        return combine(
            mapboxVoiceInstructions.voiceLanguage(),
            configOwner!!.language(),
        ) { voiceLanguage, deviceLanguage -> voiceLanguage ?: deviceLanguage }
            .distinctUntilChanged()
            .flatMapLatest { language ->
                val audioGuidance =
                    audioGuidanceServices.mapboxAudioGuidanceVoice(mapboxNavigation, language)
                mutedStateFlow.flatMapLatest { isMuted ->
                    if (isMuted) {
                        silentFlow()
                    } else {
                        speechFlow(audioGuidance)
                    }
                }
            }
    }

    /**
     * This flow will monitor navigation state to determine if audio is available.
     */
    private fun silentFlow(): Flow<MapboxAudioGuidanceState> {
        return mapboxVoiceInstructions.voiceInstructions()
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
        audioGuidance: MapboxAudioGuidanceVoice
    ): Flow<MapboxAudioGuidanceState> {
        return mapboxVoiceInstructions.voiceInstructions()
            .flatMapConcat { voice ->
                internalStateFlow.update {
                    MapboxAudioGuidanceState(
                        isMuted = false,
                        isPlayable = voice.isPlayable,
                        voiceInstructions = voice.voiceInstructions
                    )
                }
                audioGuidance.speak(voice.voiceInstructions)
            }
            .map { speechAnnouncement ->
                internalStateFlow.updateAndGet {
                    MapboxAudioGuidanceState(
                        isPlayable = it.isPlayable,
                        isMuted = it.isMuted,
                        voiceInstructions = it.voiceInstructions,
                        speechAnnouncement = speechAnnouncement
                    )
                }
            }
    }

    private suspend fun restoreMutedState() {
        dataStoreOwner?.apply {
            mutedStateFlow.value = read(STORE_AUDIO_GUIDANCE_MUTED).first()
        }
    }

    private suspend fun setMutedState(muted: Boolean) {
        mutedStateFlow.value = muted
        dataStoreOwner?.write(STORE_AUDIO_GUIDANCE_MUTED, muted)
    }

    companion object {
        private val STORE_AUDIO_GUIDANCE_MUTED =
            booleanDataStoreKey("audio_guidance_muted", false)

        /**
         * Construct an instance without registering to [MapboxNavigationApp].
         */
        @JvmStatic
        fun create() = MapboxAudioGuidance(MapboxAudioGuidanceServices(), Dispatchers.Main)

        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): MapboxAudioGuidance = MapboxNavigationApp
            .getObservers(MapboxAudioGuidance::class)
            .firstOrNull() ?: create().also { MapboxNavigationApp.registerObserver(it) }
    }
}
