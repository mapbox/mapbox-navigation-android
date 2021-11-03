package com.mapbox.androidauto.testing

import com.mapbox.androidauto.navigation.audioguidance.MapboxAudioGuidanceServices
import com.mapbox.androidauto.navigation.audioguidance.impl.MapboxAudioGuidanceVoice
import com.mapbox.androidauto.navigation.audioguidance.impl.MapboxVoiceInstructions
import com.mapbox.androidauto.navigation.audioguidance.impl.MapboxVoiceInstructionsState
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

class TestMapboxAudioGuidanceServices {

    private val voiceInstructionsFlow = MutableStateFlow<MapboxVoiceInstructions.State>(
        MapboxVoiceInstructionsState()
    )

    private val voiceLanguageFlow = MutableStateFlow<String?>(value = null)

    private val mapboxVoiceInstructions = mockk<MapboxVoiceInstructions> {
        every { voiceInstructions() } returns voiceInstructionsFlow
        every { voiceLanguage() } returns voiceLanguageFlow
    }

    private val mapboxAudioGuidanceVoice = mockk<MapboxAudioGuidanceVoice> {
        every { speak(any()) } answers {
            val voiceInstructions = firstArg<VoiceInstructions?>()
            val speechAnnouncement: SpeechAnnouncement? = voiceInstructions?.let {
                mockk {
                    every { announcement } returns it.announcement()!!
                    every { ssmlAnnouncement } returns it.ssmlAnnouncement()
                }
            }
            flowOf(speechAnnouncement).onEach {
                if (it != null) {
                    // Simulate a real speech announcement by delaying the TestCoroutineScope
                    delay(SPEECH_ANNOUNCEMENT_DELAY_MS)
                }
            }
        }
    }

    val mapboxAudioGuidanceServices = mockk<MapboxAudioGuidanceServices> {
        every { mapboxVoiceInstructions(any()) } returns mapboxVoiceInstructions
        every { mapboxAudioGuidanceVoice(any(), any()) } returns mapboxAudioGuidanceVoice
    }

    fun emitVoiceInstruction(state: MapboxVoiceInstructions.State) {
        voiceInstructionsFlow.tryEmit(state)
    }

    fun emitVoiceLanguage(language: String?) {
        voiceLanguageFlow.tryEmit(language)
    }

    companion object {
        // Speech announcements take time, simulate a delay.
        // Note that delaying the TestCoroutineScope is not actual time.
        const val SPEECH_ANNOUNCEMENT_DELAY_MS = 2000L
    }
}
