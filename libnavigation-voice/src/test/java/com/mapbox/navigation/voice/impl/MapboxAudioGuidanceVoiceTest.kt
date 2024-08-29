package com.mapbox.navigation.voice.impl

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@ExperimentalCoroutinesApi
class MapboxAudioGuidanceVoiceTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val speechApi = mockk<MapboxSpeechApi>(relaxUnitFun = true)
    private val voiceInstructionsPlayer = mockk<MapboxVoiceInstructionsPlayer>(relaxed = true)
    private val sut = MapboxAudioGuidanceVoice(
        speechApi,
        voiceInstructionsPlayer,
    )

    @Test
    fun `voice instruction should be played as SpeechAnnouncement`() =
        coroutineRule.runBlockingTest {
            mockSuccessfulLoadOnDemandSpeechApi()
            mockSuccessfulVoiceInstructionsPlayer()

            val voiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Turn right on Market"
            }
            val speechAnnouncement = sut.speak(voiceInstructions)
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }

    @Test
    fun `predownloaded voice instruction should be played as SpeechAnnouncement`() =
        coroutineRule.runBlockingTest {
            mockSuccessfulPredownloadedSpeechApi()
            mockSuccessfulVoiceInstructionsPlayer()

            val voiceInstructions = mockk<VoiceInstructions> {
                every { announcement() } returns "Turn right on Market"
            }
            val speechAnnouncement = sut.speakPredownloaded(voiceInstructions)
            assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
        }

    @Test
    fun `null should clean up the api and player`() = coroutineRule.runBlockingTest {
        sut.speak(null)

        verify { speechApi.cancel() }
        verify { voiceInstructionsPlayer.clear() }
    }

    @Test
    fun `predownloaded null should clean up the api and player`() = coroutineRule.runBlockingTest {
        sut.speakPredownloaded(null)

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
    fun `should play fallback when prewodnloaded speech api fails`() = coroutineRule.runBlockingTest {
        every { speechApi.generatePredownloaded(any(), any()) } answers {
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
        val speechAnnouncement = sut.speakPredownloaded(voiceInstructions)
        assertEquals("Turn right on Market", speechAnnouncement!!.announcement)
    }

    @Test
    fun `should wait until previous instruction finishes playback before playing next one`() =
        coroutineRule.runBlockingTest {
            mockSuccessfulPredownloadedSpeechApi()
            mockSuccessfulLoadOnDemandSpeechApi()
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
                val instruction1 = VoiceInstructions.builder().announcement("A").build()
                val announcement1 = sut.speak(instruction1) // suspend until playback finishes
                played.add(announcement1)

                val instruction2 = VoiceInstructions.builder().announcement("B").build()
                val announcement2 = sut.speak(instruction2) // suspend until playback finishes
                played.add(announcement2)

                val instruction3 = VoiceInstructions.builder().announcement("C").build()
                // suspend until playback finishes
                val announcement3 = sut.speakPredownloaded(instruction3)
                played.add(announcement3)

                val instruction4 = VoiceInstructions.builder().announcement("D").build()
                // suspend until playback finishes
                val announcement4 = sut.speakPredownloaded(instruction4)
                played.add(announcement4)

                val instruction5 = VoiceInstructions.builder().announcement("E").build()
                // suspend until playback finishes
                val announcement5 = sut.speakPredownloaded(instruction5)
                played.add(announcement5)
            }
            advanceTimeBy(1500) // advance time to 50% of announcement B playback time

            assertEquals(1, played.size)

            advanceTimeBy(1000) // advance time to 50% of announcement C playback time

            assertEquals(2, played.size)

            advanceTimeBy(1000) // advance time to 50% of announcement D playback time

            assertEquals(3, played.size)

            advanceTimeBy(1000) // advance time to 50% of announcement E playback time

            assertEquals(4, played.size)

            advanceTimeBy(500) // advance time to 100% of announcement E playback time

            assertEquals(5, played.size)
        }

    private fun mockSuccessfulPredownloadedSpeechApi() {
        every { speechApi.generatePredownloaded(any(), any()) } answers {
            val announcementArg = firstArg<VoiceInstructions>().announcement()
            val speechValue = mockk<SpeechValue> {
                every { announcement } returns SpeechAnnouncement.Builder(
                    announcementArg!!,
                ).build()
            }
            val consumer = secondArg<MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>>()
            consumer.accept(ExpectedFactory.createValue(speechValue))
        }
    }

    private fun mockSuccessfulLoadOnDemandSpeechApi() {
        every { speechApi.generate(any(), any()) } answers {
            val announcementArg = firstArg<VoiceInstructions>().announcement()
            val speechValue = mockk<SpeechValue> {
                every { announcement } returns SpeechAnnouncement.Builder(
                    announcementArg!!,
                ).build()
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
