package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.internal.extensions.inferDeviceLanguage
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioGuidanceApiTest {

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should use the routes language to speak voice instructions`() = runBlockingTest {
        val voiceInstructionsObserver = slot<VoiceInstructionsObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(any()) } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult("de"))
            }
            every {
                registerVoiceInstructionsObserver(capture(voiceInstructionsObserver))
            } answers {
                val voiceInstructions = mockk<VoiceInstructions> {
                    every { announcement() } returns "Left on Broadway"
                    every { ssmlAnnouncement() } returns null
                }
                firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(voiceInstructions)
            }
        }

        val voiceInstructionsSlot = mutableListOf<VoiceInstructions>()
        val languageSlot = mutableListOf<String>()
        val audioGuidanceApi = AudioGuidanceApi.create(
            mapboxNavigation,
            mockAudioGuidanceServices(mapboxNavigation, languageSlot, voiceInstructionsSlot)
        )

        val speechAnnouncement = audioGuidanceApi.speakVoiceInstructions().first()

        assertEquals("de", languageSlot[0])
        assertEquals("Left on Broadway", speechAnnouncement?.announcement)
    }

    @Test
    fun `should use device language if route is not available`() = runBlockingTest {
        val voiceInstructionsObserver = slot<VoiceInstructionsObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(any()) } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult(null))
            }
            every {
                registerVoiceInstructionsObserver(capture(voiceInstructionsObserver))
            } answers {
                val voiceInstructions = mockk<VoiceInstructions> {
                    every { announcement() } returns "四谷四丁目を左折です"
                    every { ssmlAnnouncement() } returns null
                }
                firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(voiceInstructions)
            }
            every { navigationOptions } returns mockk {
                every { applicationContext.inferDeviceLanguage() } returns "ja"
            }
        }

        val voiceInstructionsSlot = mutableListOf<VoiceInstructions>()
        val languageSlot = mutableListOf<String>()
        val audioGuidanceApi = AudioGuidanceApi.create(
            mapboxNavigation,
            mockAudioGuidanceServices(mapboxNavigation, languageSlot, voiceInstructionsSlot)
        )

        val speechAnnouncement = audioGuidanceApi.speakVoiceInstructions().first()

        assertEquals("ja", languageSlot[0])
        assertEquals("四谷四丁目を左折です", speechAnnouncement?.announcement)
    }

    @Test
    fun `plays voice instructions without canceling previous`() = runBlockingTest {
        val voiceInstructionsObserver = slot<VoiceInstructionsObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(any()) } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult("en"))
            }
            every {
                registerVoiceInstructionsObserver(capture(voiceInstructionsObserver))
            } answers {
                firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(
                    mockk(relaxed = true) {
                        every { announcement() } returns "Turn right on Jefferson Street"
                    }
                )
                firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(
                    mockk(relaxed = true) {
                        every { announcement() } returns "You have arrived at your destination"
                    }
                )
            }
        }

        val voiceInstructionsSlot = mutableListOf<VoiceInstructions>()
        val languageSlot = mutableListOf<String>()
        val audioGuidanceApi = AudioGuidanceApi.create(
            mapboxNavigation,
            mockAudioGuidanceServices(mapboxNavigation, languageSlot, voiceInstructionsSlot)
        )
        val emitTime = mutableListOf<Long>()
        audioGuidanceApi.speakVoiceInstructions()
            .take(2)
            .collect {
                emitTime.add(currentTime)
            }

        // Wait for the announcements. Note that this is blocking a test scheduler
        // so it should not delay actual time.
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 3)

        // Verify the time the speech announcements were completed.
        assertEquals(
            "Turn right on Jefferson Street",
            voiceInstructionsSlot[0].announcement()
        )
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS, emitTime[0])
        assertEquals(
            "You have arrived at your destination",
            voiceInstructionsSlot[1].announcement()
        )
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS * 2, emitTime[1])
    }

    @Test
    fun `should not repeat instructions when resubscribing`() = runBlockingTest {
        val voiceInstructionsObserver = slot<VoiceInstructionsObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(any()) } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult("de"))
            }
            every {
                registerVoiceInstructionsObserver(capture(voiceInstructionsObserver))
            } answers {
                val voiceInstructions = mockk<VoiceInstructions> {
                    every { announcement() } returns "Left on Broadway"
                    every { ssmlAnnouncement() } returns null
                }
                firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(voiceInstructions)
            }
        }

        val voiceInstructionsSlot = mutableListOf<VoiceInstructions>()
        val languageSlot = mutableListOf<String>()
        val audioGuidanceApi = AudioGuidanceApi.create(
            mapboxNavigation,
            mockAudioGuidanceServices(mapboxNavigation, languageSlot, voiceInstructionsSlot)
        )

        val speechAnnouncementFirst = audioGuidanceApi.speakVoiceInstructions().first()
        val speechAnnouncementNext = audioGuidanceApi.speakVoiceInstructions().first()

        assertNotNull(speechAnnouncementFirst)
        assertNull(speechAnnouncementNext)
    }

    private fun mockRoutesUpdatedResult(language: String?): RoutesUpdatedResult =
        mockk {
            every { navigationRoutes } returns listOf(
                mockk {
                    every { directionsRoute } returns mockk {
                        every { voiceLanguage() } returns language
                    }
                },
                mockk()
            )
        }

    private fun mockAudioGuidanceServices(
        mapboxNavigation: MapboxNavigation,
        languageSlot: MutableList<String>,
        voiceInstructionsSlot: MutableList<VoiceInstructions>
    ): AudioGuidanceServices = mockk {
        every {
            audioGuidanceVoice(mapboxNavigation, capture(languageSlot))
        } returns mockk {
            every { speak(capture(voiceInstructionsSlot)) } answers {
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
    }

    private companion object {
        const val SPEECH_ANNOUNCEMENT_DELAY_MS = 2000L
    }
}
