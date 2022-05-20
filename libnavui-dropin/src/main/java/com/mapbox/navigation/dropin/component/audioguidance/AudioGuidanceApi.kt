package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.internal.extensions.inferDeviceLanguage
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.internal.extensions.flowVoiceInstructions
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

/**
 * Internal implementation classes for audio guidance.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class AudioGuidanceApi private constructor(
    private val mapboxNavigation: MapboxNavigation,
    private val audioGuidanceServices: AudioGuidanceServices
) {
    // When changing audio state, this ensures the audio is only spoken once.
    private var lastInstruction: VoiceInstructions? = null

    /**
     * While attached to this flowable, speech speech announcements will be triggered.
     */
    fun speakVoiceInstructions(): Flow<SpeechAnnouncement?> {
        return mapboxNavigation.flowDirectionsLanguage()
            .map { language ->
                audioGuidanceServices.audioGuidanceVoice(mapboxNavigation, language)
            }
            .flatMapLatest { audioGuidance ->
                // The flatMapConcat below is subtle but important. It ensures voice instructions
                // will not cancel each other.
                mapboxNavigation.flowVoiceInstructions().flatMapConcat { voiceInstructions ->
                    if (lastInstruction?.announcement() != voiceInstructions.announcement() ||
                        lastInstruction?.ssmlAnnouncement() != voiceInstructions.ssmlAnnouncement()
                    ) {
                        lastInstruction = voiceInstructions
                        audioGuidance.speak(voiceInstructions)
                    } else {
                        flowOf(null)
                    }
                }
            }
    }

    private fun MapboxNavigation.flowDirectionsLanguage(): Flow<String> {
        return flowRoutesUpdated().mapLatest {
            it.navigationRoutes.firstOrNull()?.directionsRoute?.voiceLanguage()
                ?: navigationOptions.applicationContext.inferDeviceLanguage()
        }
    }

    companion object {
        fun create(
            mapboxNavigation: MapboxNavigation,
            audioGuidanceServices: AudioGuidanceServices
        ) = AudioGuidanceApi(mapboxNavigation, audioGuidanceServices)
    }
}
