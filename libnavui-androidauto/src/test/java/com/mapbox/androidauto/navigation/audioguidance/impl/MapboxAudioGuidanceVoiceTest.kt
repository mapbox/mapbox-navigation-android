@file:Suppress("NoMockkVerifyImport")

package com.mapbox.androidauto.navigation.audioguidance.impl

import com.mapbox.androidauto.testing.CarAppTestRule
import com.mapbox.androidauto.testing.MainCoroutineRule
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxAudioGuidanceVoiceTest {

    @get:Rule
    val carAppTest = CarAppTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val speechApi = mockk<MapboxSpeechApi>(relaxUnitFun = true)
    private val voiceInstructionsPlayer = mockk<MapboxVoiceInstructionsPlayer>(relaxUnitFun = true)
    private val carAppAudioGuidanceVoice = MapboxAudioGuidanceVoice(
        speechApi,
        voiceInstructionsPlayer
    )

    @Test
    fun `voice instruction should be played as SpeechAnnouncement`() = coroutineRule.runTest {
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
    fun `null should clean up the api and player`() = coroutineRule.runTest {
        carAppAudioGuidanceVoice.speak(null).collect()

        verify { speechApi.cancel() }
        verify { voiceInstructionsPlayer.clear() }
    }

    @Test
    fun `should play fallback when speech api fails`() = coroutineRule.runTest {
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
