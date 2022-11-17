package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.TestMapboxAudioGuidanceServices
import com.mapbox.navigation.ui.voice.TestMapboxAudioGuidanceServices.Companion.SPEECH_ANNOUNCEMENT_DELAY_MS
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructionsState
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioGuidanceTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val testMapboxAudioGuidanceServices = TestMapboxAudioGuidanceServices()
    private val mapboxNavigation: MapboxNavigation = mockk {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk()
        }
    }

    private val carAppAudioGuidance = MapboxAudioGuidance(
        testMapboxAudioGuidanceServices.mapboxAudioGuidanceServices,
        coroutineRule.testDispatcher,
    )

    @Suppress("PrivatePropertyName")
    private val VOICE_INSTRUCTION1 = VoiceInstructions.builder()
        .announcement("Turn right on Jefferson Street")
        .build()

    @Suppress("PrivatePropertyName")
    private val VOICE_INSTRUCTION2 = VoiceInstructions.builder()
        .announcement("You have arrived at your destination")
        .build()

    @Test
    fun `empty state flow by default`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)

        val initialState = carAppAudioGuidance.stateFlow().first()

        assertEquals(false, initialState.isPlayable)
        assertEquals(false, initialState.isMuted)
        assertNull(initialState.speechAnnouncement)
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `completes full lifecycle`() = coroutineRule.runBlockingTest {
        val states = mutableListOf<MapboxAudioGuidanceState>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        carAppAudioGuidance.onAttached(mapboxNavigation)
        carAppAudioGuidance.onDetached(mapboxNavigation)

        assertEquals(1, states.size)
        job.cancelAndJoin()
    }

    @Test
    fun `becomes playable before voice instructions arrive`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)
        val states = mutableListOf<MapboxAudioGuidanceState>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        testMapboxAudioGuidanceServices.emitVoiceInstruction(
            MapboxVoiceInstructionsState(true, null)
        )

        assertEquals(2, states.size)
        assertFalse(states[1].isMuted)
        assertTrue(states[1].isPlayable)
        assertNull(states[1].speechAnnouncement)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `plays voice instructions`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)
        val states = mutableListOf<MapboxAudioGuidanceState>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        val voiceInstruction = MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION2)
        testMapboxAudioGuidanceServices.emitVoiceInstruction(voiceInstruction)
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

        assertEquals(3, states.size)
        assertFalse(states[2].isMuted)
        assertTrue(states[2].isPlayable)
        assertEquals(
            VOICE_INSTRUCTION2.announcement(),
            states[2].speechAnnouncement?.announcement
        )
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `does not play when muted but provides announcement`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)
        val states = mutableListOf<MapboxAudioGuidanceState>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect { states.add(it) }
        }

        carAppAudioGuidance.mute()
        testMapboxAudioGuidanceServices.emitVoiceInstruction(
            MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION1)
        )
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

        assertEquals(3, states.size)
        assertTrue(states[2].isMuted)
        assertTrue(states[2].isPlayable)
        assertEquals(
            VOICE_INSTRUCTION1.announcement(),
            states[2].voiceInstructions?.announcement()
        )
        assertNull(states[2].speechAnnouncement)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `plays voice instructions without canceling previous`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)
        val states = mutableListOf<Pair<MapboxAudioGuidanceState, Long>>()
        val job = launch {
            carAppAudioGuidance.stateFlow().collect {
                states.add(it to currentTime)
            }
        }

        // Emit two announcements without waiting for one to complete.
        val firstVoiceInstruction = MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION1)
        testMapboxAudioGuidanceServices.emitVoiceInstruction(firstVoiceInstruction)
        val secondVoiceInstruction = MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION2)
        testMapboxAudioGuidanceServices.emitVoiceInstruction(secondVoiceInstruction)
        // Wait for the announcements. Note that this is blocking a test scheduler
        // so it should not delay actual time.
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 3)

        // Verify the time the speech announcements were completed.
        assertEquals(5, states.size)
        val firstAnnouncement = states[2].first.speechAnnouncement?.announcement
        val secondAnnouncement = states[4].first.speechAnnouncement?.announcement
        assertEquals(VOICE_INSTRUCTION1.announcement(), firstAnnouncement)
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS, states[2].second)
        assertEquals(VOICE_INSTRUCTION2.announcement(), secondAnnouncement)
        assertEquals(SPEECH_ANNOUNCEMENT_DELAY_MS * 2, states[4].second)
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `plays voice each instruction only once`() = coroutineRule.runBlockingTest {
        carAppAudioGuidance.onAttached(mapboxNavigation)
        val states = mutableListOf<MapboxAudioGuidanceState>()
        val job = launch {
            carAppAudioGuidance.stateFlow().drop(1).toList(states)
        }

        // Emit two announcements without waiting for one to complete.
        testMapboxAudioGuidanceServices.emitVoiceInstruction(
            MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION1)
        )
        // Wait for the playback. Note that this is only blocking the test scheduler.
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 2)
        carAppAudioGuidance.mute()
        carAppAudioGuidance.unmute()
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 2) // Wait for the playback.
        testMapboxAudioGuidanceServices.emitVoiceInstruction(
            MapboxVoiceInstructionsState(true, VOICE_INSTRUCTION2)
        )
        delay(SPEECH_ANNOUNCEMENT_DELAY_MS * 2) // Wait for the playback.

        // expected MapboxAudioGuidanceState values
        // # IS MUTED     INSTRUCTION         HAS SPEECH ANNOUNCEMENT
        //          (first instruction)
        // 0 false      VOICE_INSTRUCTION1    false
        // 1 false      VOICE_INSTRUCTION1    true
        //          (mute)
        // 2 true       VOICE_INSTRUCTION1    false
        //          (un-mute)
        // 3 false      VOICE_INSTRUCTION1    false
        //          (second instruction)
        // 4 false      VOICE_INSTRUCTION2    false
        // 5 false      VOICE_INSTRUCTION2    true
        val expectation = mutableListOf(
            Triple(false, VOICE_INSTRUCTION1, false),
            Triple(false, VOICE_INSTRUCTION1, true),
            Triple(true, VOICE_INSTRUCTION1, false),
            Triple(false, VOICE_INSTRUCTION1, false),
            Triple(false, VOICE_INSTRUCTION2, false),
            Triple(false, VOICE_INSTRUCTION2, true),
        )
        assertEquals(expectation.size, states.size)
        expectation.forEachIndexed { i, expected ->
            val v = Triple(
                states[i].isMuted,
                states[i].voiceInstructions,
                states[i].speechAnnouncement?.announcement != null
            )
            assertEquals("$i expectation not matching", expected, v)
        }
        job.cancelAndJoin()
        carAppAudioGuidance.onDetached(mapboxNavigation)
    }

    @Test
    fun `voice language from route is preferred to device language`() =
        coroutineRule.runBlockingTest {
            carAppAudioGuidance.onAttached(mapboxNavigation)

            val voiceLanguage = "ru"
            testMapboxAudioGuidanceServices.emitVoiceLanguage(voiceLanguage)
            delay(SPEECH_ANNOUNCEMENT_DELAY_MS)

            val mapboxAudioGuidanceServices =
                testMapboxAudioGuidanceServices.mapboxAudioGuidanceServices
            excludeRecords {
                mapboxAudioGuidanceServices.mapboxVoiceInstructions()
            }
            verifySequence {
                mapboxAudioGuidanceServices.dataStoreOwner(any())
                mapboxAudioGuidanceServices.configOwner(any())
                mapboxAudioGuidanceServices.mapboxAudioGuidanceVoice(any(), "en")
                mapboxAudioGuidanceServices.mapboxAudioGuidanceVoice(any(), voiceLanguage)
            }
            carAppAudioGuidance.onDetached(mapboxNavigation)
        }

    @Test
    fun `getCurrentVoiceInstructionsPlayer returns valid player instance`() {
        val mapboxAudioGuidanceServices =
            testMapboxAudioGuidanceServices.mapboxAudioGuidanceServices

        every { mapboxAudioGuidanceServices.voiceInstructionsPlayer } returns null
        assertNull(carAppAudioGuidance.getCurrentVoiceInstructionsPlayer())

        val player: MapboxVoiceInstructionsPlayer = mockk()
        every { mapboxAudioGuidanceServices.voiceInstructionsPlayer } returns player
        assertEquals(player, carAppAudioGuidance.getCurrentVoiceInstructionsPlayer())

        every { mapboxAudioGuidanceServices.voiceInstructionsPlayer } returns null
        assertNull(carAppAudioGuidance.getCurrentVoiceInstructionsPlayer())
    }
}
