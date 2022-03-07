@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioGuidanceVoiceTest {

    private val speechApi = mockk<MapboxSpeechApi>(relaxUnitFun = true)
    private val voiceInstructionsPlayer = mockk<MapboxVoiceInstructionsPlayer>(relaxUnitFun = true)
    private val carAppAudioGuidanceVoice = AudioGuidanceVoice(
        speechApi,
        voiceInstructionsPlayer
    )

    @Test
    fun `voice instruction should be played as SpeechAnnouncement`() = runBlockingTest {
        mockSuccessfulSpeechApi()
        mockSuccessfulVoiceInstructionsPlayer()

        val voiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "Turn right on Market"
        }
        carAppAudioGuidanceVoice.speak(voiceInstructions).collect { speechAnnouncement ->
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }
    }

    @Test
    fun `the apis are cleaned up after completing announcements`() = runBlockingTest {
        mockSuccessfulSpeechApi()
        mockSuccessfulVoiceInstructionsPlayer()
        val voiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "Turn right on Market"
        }
        carAppAudioGuidanceVoice.speak(voiceInstructions).collect { speechAnnouncement ->
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }

        verify { speechApi.clean(any()) }
        verify { speechApi.cancel() }
        verify { voiceInstructionsPlayer.clear() }
    }

    @Test
    fun `should play fallback when speech api fails`() = runBlockingTest {
        every { speechApi.generate(any(), any()) } answers {
            val consumer = secondArg<MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>>()
            val error = mockk<SpeechError> {
                every { fallback } returns mockk {
                    every { announcement } returns "Turn right on Market"
                }
            }
            consumer.accept(ExpectedFactory.createError(error))
        }
        mockSuccessfulVoiceInstructionsPlayer()

        val voiceInstructions = mockk<VoiceInstructions> {
            every { announcement() } returns "This message fails"
        }
        carAppAudioGuidanceVoice.speak(voiceInstructions).collect { speechAnnouncement ->
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }
    }

    private fun mockSuccessfulSpeechApi() {
        every { speechApi.generate(any(), any()) } answers {
            val announcementArg = firstArg<VoiceInstructions>().announcement()
            val speechValue = mockk<SpeechValue> {
                every { announcement } returns mockk {
                    every { announcement } returns announcementArg!!
                }
            }
            val consumer = secondArg<MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>>()
            consumer.accept(ExpectedFactory.createValue(speechValue))
        }
    }

    private fun mockSuccessfulVoiceInstructionsPlayer() {
        every { voiceInstructionsPlayer.play(any(), any()) } answers {
            val speechAnnouncement = firstArg<SpeechAnnouncement>()
            secondArg<MapboxNavigationConsumer<SpeechAnnouncement>>().accept(speechAnnouncement)
        }
    }
}
