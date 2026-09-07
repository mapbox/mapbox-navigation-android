package com.mapbox.navigation.voice.api

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.utils.internal.datastore.booleanDataStoreKey
import com.mapbox.navigation.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.voice.internal.impl.MapboxAudioGuidanceServices
import com.mapbox.navigation.voice.options.MapboxSpeechApiOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    options: MapboxSpeechApiOptions,
) : MapboxNavigationObserver {

    private var dataStoreOwner: NavigationDataStoreOwner? = null
    private var configOwner: NavigationConfigOwner? = null

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private var trigger: VoiceInstructionsPrefetcher? = null
    private var mutedStateFlow = MutableStateFlow(false)
    private val internalStateFlow = MutableStateFlow(MapboxAudioGuidanceState())
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mapboxVoiceInstructions = audioGuidanceServices.mapboxVoiceInstructions()
    private val optionsFlow = MutableStateFlow(options)

    private var job: Job? = null

    /**
     * Current instance of a [VoiceInstructionsPlayer].
     */
    fun getCurrentVoiceInstructionsPlayer() = audioGuidanceServices.voiceInstructionsPlayer

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
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxVoiceInstructions.unregisterObservers(mapboxNavigation)
        trigger?.onDetached(mapboxNavigation)
        job?.cancel()
        job = null
    }

    /**
     * This flow gives you access to the state of mapbox audio guidance without effecting state.
     *
     * In order to enable voice guidance, you must call [MapboxNavigation.startTripSession]
     * and set a route for active guidance through [MapboxNavigation.setNavigationRoutes].
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
     * Updates [MapboxSpeechApiOptions] that was provided during [MapboxAudioGuidance] creation.
     *
     * @param options New [MapboxSpeechApiOptions]
     *
     * @see [create]
     */
    fun updateSpeechApiOptions(options: MapboxSpeechApiOptions) {
        optionsFlow.value = options
    }

    /**
     * Top level flow that will switch based on the language and muted state.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun audioGuidanceFlow(
        mapboxNavigation: MapboxNavigation,
    ): Flow<MapboxAudioGuidanceState> {
        return mapboxNavigation.audioGuidanceVoice().flatMapLatest { audioGuidance ->
            var lastPlayedInstructions: VoiceInstructions? = null
            mutedStateFlow.flatMapLatest { isMuted ->
                val voiceInstructions = mapboxVoiceInstructions.voiceInstructions()
                    .map { state ->
                        internalStateFlow.updateAndGet {
                            MapboxAudioGuidanceState(
                                isMuted = isMuted,
                                isFirst = state.isFirst,
                                isPlayable = state.isPlayable,
                                voiceInstructions = state.voiceInstructions,
                            )
                        }
                    }

                if (isMuted) {
                    voiceInstructions
                } else {
                    voiceInstructions
                        .filter { it.voiceInstructions != lastPlayedInstructions }
                        .map {
                            lastPlayedInstructions = it.voiceInstructions
                            val announcement = if (it.isFirst) {
                                audioGuidance.speak(it.voiceInstructions)
                            } else {
                                audioGuidance.speakPredownloaded(it.voiceInstructions)
                            }
                            internalStateFlow.updateAndGet { state ->
                                MapboxAudioGuidanceState(
                                    isPlayable = state.isPlayable,
                                    isMuted = state.isMuted,
                                    isFirst = state.isFirst,
                                    voiceInstructions = state.voiceInstructions,
                                    speechAnnouncement = announcement,
                                )
                            }
                        }
                }
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun MapboxNavigation.audioGuidanceVoice(): Flow<MapboxAudioGuidanceVoice> {
        return combine(
            mapboxVoiceInstructions.voiceLanguage(),
            configOwner!!.language(),
            optionsFlow,
        ) { voiceLanguage, deviceLanguage, options -> (voiceLanguage ?: deviceLanguage) to options }
            .distinctUntilChanged()
            .map { (language, options) ->
                trigger?.onDetached(this)
                audioGuidanceServices.mapboxAudioGuidanceVoice(
                    this,
                    language,
                    options,
                ).also {
                    trigger = VoiceInstructionsPrefetcher(it.mapboxSpeechApi).also { trigger ->
                        trigger.onAttached(this)
                    }
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
         *
         * @param options Optional [MapboxSpeechApiOptions]
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            options: MapboxSpeechApiOptions = MapboxSpeechApiOptions.Builder().build(),
        ) = MapboxAudioGuidance(
            MapboxAudioGuidanceServices(),
            Dispatchers.Main.immediate,
            options,
        )

        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): MapboxAudioGuidance = MapboxNavigationApp
            .getObservers(MapboxAudioGuidance::class)
            .firstOrNull() ?: create().also { MapboxNavigationApp.registerObserver(it) }
    }
}
