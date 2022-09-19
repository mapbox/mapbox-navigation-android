package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioAction
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioGuidanceState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.testing.TestStore
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidanceState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioGuidanceStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var audioGuidanceState: MutableStateFlow<MapboxAudioGuidanceState>
    private lateinit var mockAudioGuidance: MapboxAudioGuidance
    private lateinit var testStore: TestStore

    @Before
    fun setup() {
        mockkObject(MapboxAudioGuidance)

        audioGuidanceState = MutableStateFlow(
            mockk { every { isMuted } returns false }
        )
        mockAudioGuidance = mockk(relaxed = true) {
            every { stateFlow() } returns audioGuidanceState
        }
        every { MapboxAudioGuidance.getInstance() } returns mockAudioGuidance

        testStore = spyk(TestStore())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttach - will observe MapboxAudioGuidance state and update Store state`() =
        runBlockingTest {
            val sut = AudioGuidanceStateController(testStore)
            testStore.setState(
                State(
                    audio = AudioGuidanceState(isMuted = false)
                )
            )
            audioGuidanceState.value = mockk { every { isMuted } returns true }

            sut.onAttached(mockk())
            assertTrue(testStore.state.value.audio.isMuted)
        }

    @Test
    fun `onAttach - will observe Store audio state and call MapboxAudioGuidance`() =
        runBlockingTest {
            val sut = AudioGuidanceStateController(testStore)
            testStore.setState(
                State(
                    audio = AudioGuidanceState(isMuted = false)
                )
            )
            audioGuidanceState.value = mockk { every { isMuted } returns false }

            sut.onAttached(mockk())
            testStore.setState(
                State(
                    audio = AudioGuidanceState(isMuted = true)
                )
            )

            verify(exactly = 1) { mockAudioGuidance.mute() }
        }

    @Test
    fun `action - Toggle will reverse muted state`() = runBlockingTest {
        testStore.setState(
            State(
                navigation = NavigationState.ActiveNavigation,
                audio = AudioGuidanceState(isMuted = false)
            )
        )
        val sut = AudioGuidanceStateController(testStore)

        val mapboxNavigation: MapboxNavigation = mockk()
        sut.onAttached(mapboxNavigation)
        testStore.dispatch(AudioAction.Toggle)
        sut.onDetached(mapboxNavigation)

        assertTrue(testStore.state.value.audio.isMuted)
    }
}
