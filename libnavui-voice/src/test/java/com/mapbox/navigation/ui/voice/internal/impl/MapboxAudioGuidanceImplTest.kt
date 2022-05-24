package com.mapbox.navigation.ui.voice.internal.impl

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.voice.TestCarAppDataStoreOwner
import com.mapbox.navigation.ui.voice.TestMapboxAudioGuidanceServices
import com.mapbox.navigation.ui.voice.TestMapboxAudioGuidanceServices.Companion.SPEECH_ANNOUNCEMENT_DELAY_MS
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructions
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val deviceLanguage = "en"

@ExperimentalCoroutinesApi
@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioGuidanceImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val testMapboxAudioGuidanceServices = TestMapboxAudioGuidanceServices()
    private val testCarAppDataStoreOwner = TestCarAppDataStoreOwner()
    private val carAppConfigOwner: NavigationConfigOwner = mockk {
        every { language() } returns flowOf(deviceLanguage)
    }

    private val carAppAudioGuidance = MapboxAudioGuidanceImpl(
        testMapboxAudioGuidanceServices.mapboxAudioGuidanceServices,
        carAppConfigOwner,
        coroutineRule.testDispatcher,
    ).apply {
        dataStoreOwner = testCarAppDataStoreOwner.carAppDataStoreOwner
    }

    @Test
    fun `empty state flow by default`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mockk())

        val initialState = carAppAudioGuidance.stateFlow().first()

        assertEquals(false, initialState.isPlayable)
        assertEquals(false, initialState.isMuted)
        assertNull(initialState.speechAnnouncement)
        carAppAudioGuidance.onDetached(mockk())
    }

    @Test
    fun `completes full lifecycle`() = coroutineRule.runBlockingTest {
        val states = mutableListOf<MapboxAudioGuidance.State>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        carAppAudioGuidance.onAttached(mockk())
        carAppAudioGuidance.onDetached(mockk())

        assertEquals(1, states.size)
        job.cancelAndJoin()
    }

    @Test
    fun `becomes playable before voice instructions arrive`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mockk())
        val states = mutableListOf<MapboxAudioGuidance.State>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        val voiceInstruction = mockk<MapboxVoiceInstructions.State> {
            every { isPlayable } returns true
            every { voiceInstructions } returns null
        }
        testMapboxAudioGuidanceServices.emitVoiceInstruction(voiceInstruction)

        assertEquals(2, states.size)
        assertFalse(states[1].isMuted)
        assertTrue(states[1].isPlayable)
        assertNull(states[1].speechAnnouncement)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mockk())
    }

    @Test
    fun `plays voice instructions`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mockk())
        val states = mutableListOf<MapboxAudioGuidance.State>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        val voiceInstruction = mockk<MapboxVoiceInstructions.State> {
            every { isPlayable } returns true
            every { voiceInstructions } returns mockk(relaxed = true) {
                every { announcement() } returns "You have arrived at your destination"
            }
        }
        testMapboxAudioGuidanceServices.emitVoiceInstruction(voiceInstruction)
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

        assertEquals(3, states.size)
        assertFalse(states[2].isMuted)
        assertTrue(states[2].isPlayable)
        assertEquals(
            "You have arrived at your destination",
            states[2].speechAnnouncement?.announcement
        )
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mockk())
    }

    @Test
    fun `does not play when muted but provides announcement`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mockk())
        val states = mutableListOf<MapboxAudioGuidance.State>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        carAppAudioGuidance.mute()
        val voiceInstruction = mockk<MapboxVoiceInstructions.State> {
            every { isPlayable } returns true
            every { voiceInstructions } returns mockk(relaxed = true) {
                every { announcement() } returns "You have arrived at your destination"
            }
        }
        testMapboxAudioGuidanceServices.emitVoiceInstruction(voiceInstruction)
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

        assertEquals(3, states.size)
        assertTrue(states[2].isMuted)
        assertTrue(states[2].isPlayable)
        assertEquals(
            "You have arrived at your destination",
            states[2].voiceInstructions?.announcement()
        )
        assertNull(states[2].speechAnnouncement)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mockk())
    }

    @Test
    fun `plays voice instructions without canceling previous`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mockk())
        val states = mutableListOf<Pair<MapboxAudioGuidance.State, Long>>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect {
                states.add(it to currentTime)
            }
        }

        // Emit two announcements without waiting for one to complete.
        val firstVoiceInstruction = mockk<MapboxVoiceInstructions.State> {
            every { isPlayable } returns true
            every { voiceInstructions } returns mockk(relaxed = true) {
                every { announcement() } returns "Turn right on Jefferson Street"
            }
        }
        testMapboxAudioGuidanceServices.emitVoiceInstruction(firstVoiceInstruction)
        val secondVoiceInstruction = mockk<MapboxVoiceInstructions.State> {
            every { isPlayable } returns true
            every { voiceInstructions } returns mockk(relaxed = true) {
                every { announcement() } returns "You have arrived at your destination"
            }
        }
        testMapboxAudioGuidanceServices.emitVoiceInstruction(secondVoiceInstruction)
        // Wait for the announcements. Note that this is blocking a test scheduler
        // so it should not delay actual time.
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 3)

        // Verify the time the speech announcements were completed.
        assertEquals(5, states.size)
        val firstAnnouncement = states[2].first.speechAnnouncement?.announcement
        val secondAnnouncement = states[4].first.speechAnnouncement?.announcement
        assertEquals("Turn right on Jefferson Street", firstAnnouncement)
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS, states[2].second)
        assertEquals("You have arrived at your destination", secondAnnouncement)
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS * 2, states[4].second)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mockk())
    }

    @Test
    fun `voice language from route is preferred to device language`() =
        coroutineRule.runBlockingTest {
            carAppAudioGuidance.onAttached(mockk())

            val voiceLanguage = "ru"
            testMapboxAudioGuidanceServices.emitVoiceLanguage(voiceLanguage)
            delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

            val mapboxAudioGuidanceServices =
                testMapboxAudioGuidanceServices.mapboxAudioGuidanceServices
            excludeRecords {
                mapboxAudioGuidanceServices.mapboxVoiceInstructions()
            }
            verifySequence {
                mapboxAudioGuidanceServices.mapboxAudioGuidanceVoice(any(), deviceLanguage)
                mapboxAudioGuidanceServices.mapboxAudioGuidanceVoice(any(), voiceLanguage)
            }
            carAppAudioGuidance.onDetached(mockk())
        }
}
