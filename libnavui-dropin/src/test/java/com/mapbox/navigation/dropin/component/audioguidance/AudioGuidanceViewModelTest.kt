package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class AudioGuidanceViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mapboxAudioApi = mockk<AudioGuidanceApi>(relaxed = true)
    private lateinit var testStore: TestStore

    @Before
    fun setup() {
        mockkObject(AudioGuidanceApi)
        mockkObject(MapboxNavigationApp)
        every { AudioGuidanceApi.create(any(), any()) } returns mapboxAudioApi
        testStore = spyk(TestStore())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttach will collect voice instructions for ActiveNavigation`() = runBlockingTest {
        val mapboxAudioViewModel = AudioGuidanceViewModel(testStore)
        testStore.setState(
            State(
                navigation = NavigationState.ActiveNavigation,
                audio = AudioGuidanceState(isMuted = false)
            )
        )

        mapboxAudioViewModel.onAttached(mockMapboxNavigation())

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `onAttach will not collect voice instructions for RoutePreview`() = runBlockingTest {
        testStore.setState(
            State(
                navigation = NavigationState.RoutePreview,
                audio = AudioGuidanceState(isMuted = false)
            )
        )
        val mapboxAudioViewModel = AudioGuidanceViewModel(testStore)

        mapboxAudioViewModel.onAttached(mockMapboxNavigation())

        verify(exactly = 0) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `detached will not collect voice instructions`() = runBlockingTest {
        testStore.setState(
            State(
                navigation = NavigationState.ActiveNavigation,
                audio = AudioGuidanceState(isMuted = true)
            )
        )
        val mapboxAudioViewModel = AudioGuidanceViewModel(testStore)

        val mapboxNavigation = mockMapboxNavigation()
        mapboxAudioViewModel.onAttached(mapboxNavigation)
        mapboxAudioViewModel.onDetached(mapboxNavigation)
        testStore.dispatch(AudioAction.Unmute)

        verify(exactly = 0) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `Toggle will reverse muted state`() = runBlockingTest {
        testStore.setState(
            State(
                navigation = NavigationState.ActiveNavigation,
                audio = AudioGuidanceState(isMuted = false)
            )
        )
        val mapboxAudioViewModel = AudioGuidanceViewModel(testStore)

        val mapboxNavigation = mockMapboxNavigation()
        mapboxAudioViewModel.onAttached(mapboxNavigation)
        testStore.dispatch(AudioAction.Toggle)
        mapboxAudioViewModel.onDetached(mapboxNavigation)

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
        assertTrue(testStore.state.value.audio.isMuted)
    }

    @Test
    fun `speakVoiceInstructions is collected`() = runBlockingTest {
        testStore.setState(
            State(
                navigation = NavigationState.ActiveNavigation,
                audio = AudioGuidanceState(isMuted = false)
            )
        )
        val mapboxAudioViewModel = AudioGuidanceViewModel(testStore)

        val captureSpeechAnnouncement = mutableListOf<SpeechAnnouncement?>()
        every { mapboxAudioApi.speakVoiceInstructions() } answers {
            flowOf<SpeechAnnouncement?>(mockk(), mockk()).onEach {
                captureSpeechAnnouncement.add(it)
            }
        }

        mapboxAudioViewModel.onAttached(mockk())

        assertEquals(2, captureSpeechAnnouncement.size)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
