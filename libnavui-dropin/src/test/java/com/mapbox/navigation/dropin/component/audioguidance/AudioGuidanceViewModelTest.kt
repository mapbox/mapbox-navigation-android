package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class AudioGuidanceViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val navigationStateViewModel = mockk<NavigationStateViewModel>()
    private val mapboxAudioApi = mockk<AudioGuidanceApi>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(AudioGuidanceApi)
        mockkObject(MapboxNavigationApp)
        every { AudioGuidanceApi.create(any(), any()) } returns mapboxAudioApi
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `default state is not muted`() = runBlockingTest {
        val mapboxAudioViewModel = AudioGuidanceViewModel(navigationStateViewModel)
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        val guidanceState: AudioGuidanceState = mapboxAudioViewModel.state.first()

        assertFalse(guidanceState.isMuted)
    }

    @Test
    fun `onAttach will collect voice instructions for ActiveNavigation`() = runBlockingTest {
        val mapboxAudioViewModel =
            AudioGuidanceViewModel(navigationStateViewModel, AudioGuidanceState(isMuted = false))
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        mapboxAudioViewModel.onAttached(mockMapboxNavigation())

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `onAttach will not collect voice instructions for RoutePreview`() = runBlockingTest {
        val mapboxAudioViewModel =
            AudioGuidanceViewModel(navigationStateViewModel, AudioGuidanceState(isMuted = false))
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.RoutePreview
        )

        mapboxAudioViewModel.onAttached(mockMapboxNavigation())

        verify(exactly = 0) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `detached will not collect voice instructions`() = runBlockingTest {
        val mapboxAudioViewModel =
            AudioGuidanceViewModel(navigationStateViewModel, AudioGuidanceState(isMuted = true))
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        val mapboxNavigation = mockMapboxNavigation()
        mapboxAudioViewModel.onAttached(mapboxNavigation)
        mapboxAudioViewModel.onDetached(mapboxNavigation)
        mapboxAudioViewModel.invoke(AudioAction.Unmute)

        verify(exactly = 0) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `Toggle will reverse muted state`() = runBlockingTest {
        val mapboxAudioViewModel =
            AudioGuidanceViewModel(navigationStateViewModel, AudioGuidanceState(isMuted = false))
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        val mapboxNavigation = mockMapboxNavigation()
        mapboxAudioViewModel.onAttached(mapboxNavigation)
        mapboxAudioViewModel.invoke(AudioAction.Toggle)
        mapboxAudioViewModel.onDetached(mapboxNavigation)

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
        assertTrue(mapboxAudioViewModel.state.value.isMuted)
    }

    @Test
    fun `speakVoiceInstructions is collected`() = runBlockingTest {
        val mapboxAudioViewModel =
            AudioGuidanceViewModel(navigationStateViewModel, AudioGuidanceState(isMuted = false))
        every { navigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )
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
