package com.mapbox.navigation.ui.voice.internal.impl

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxAudioGuidanceVoiceTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val speechApi = mockk<MapboxSpeechApi>(relaxUnitFun = true)
    private val voiceInstructionsPlayer = mockk<MapboxVoiceInstructionsPlayer>(relaxed = true)
    private val sut = MapboxAudioGuidanceVoice(
        speechApi,
        voiceInstructionsPlayer
    )

    @Test
    fun `voice instruction should be played as SpeechAnnouncement`() =
        coroutineRule.runBlockingTest {
            mockSuccessfulSpeechApi()
            mockSuccessfulVoiceInstructionsPlayer()

            val voiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Turn right on Market"
            }
            val speechAnnouncement = sut.speak(voiceInstructions)
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }

    @Test
    fun `null should clean up the api and player`() = coroutineRule.runBlockingTest {
        sut.speak(null)

        verify { speechApi.cancel() }
        verify { voiceInstructionsPlayer.clear() }
    }

    @Test
    fun `should play fallback when speech api fails`() = coroutineRule.runBlockingTest {
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
        val speechAnnouncement = sut.speak(voiceInstructions)
        assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
    }

    @Test
    fun `should wait until previous instruction finishes playback before playing next one`() =
        coroutineRule.runBlockingTest {
            mockSuccessfulSpeechApi()
            every { voiceInstructionsPlayer.play(any(), any()) } answers {
                launch {
                    val speechAnnouncement = firstArg<SpeechAnnouncement>()
                    delay(1000) // simulate 1 second announcement playback duration
                    secondArg<MapboxNavigationConsumer<SpeechAnnouncement>>()
                        .accept(speechAnnouncement)
                }
                Unit
            }

            val played = mutableListOf<SpeechAnnouncement?>()
            launch {
                listOf(
                    VoiceInstructions.builder().announcement("A").build(),
                    VoiceInstructions.builder().announcement("B").build()
                ).forEach {
                    val announcement = sut.speak(it) // suspend until playback finishes
                    played.add(announcement)
                }
            }
            advanceTimeBy(1500) // advance time to 50% of announcement B playback time

            assertEquals(1, played.size)
        }

    private fun mockSuccessfulSpeechApi() {
        every { speechApi.generate(any(), any()) } answers {
            val announcementArg = firstArg<VoiceInstructions>().announcement()
            val speechValue = mockk<SpeechValue> {
                every { announcement } returns SpeechAnnouncement.Builder(announcementArg!!).build()
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
