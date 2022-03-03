package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class MapboxAudioViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mapboxAudioApi = mockk<MapboxAudioApi>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(MapboxAudioApi)
        every { MapboxAudioApi.create(any()) } returns mapboxAudioApi
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `default state is not muted`() = runBlockingTest {
        val mapboxAudioViewModel = MapboxAudioViewModel()

        val state: MapboxAudioState = mapboxAudioViewModel.state.first()

        assertFalse(state.isMuted)
    }

    @Test
    fun `onAttach will collect voice instructions`() = runBlockingTest {
        val mapboxAudioViewModel = MapboxAudioViewModel(MapboxAudioState(isMuted = false))

        mapboxAudioViewModel.onAttached(mockk())

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `detached will not collect voice instructions`() = runBlockingTest {
        val mapboxAudioViewModel = MapboxAudioViewModel(MapboxAudioState(isMuted = true))
        mapboxAudioViewModel.onAttached(mockk())
        mapboxAudioViewModel.onDetached(mockk())
        mapboxAudioViewModel.invoke(AudioAction.Unmute)

        verify(exactly = 0) { mapboxAudioApi.speakVoiceInstructions() }
    }

    @Test
    fun `Toggle will reverse muted state`() = runBlockingTest {
        val mapboxAudioViewModel = MapboxAudioViewModel(MapboxAudioState(isMuted = false))

        mapboxAudioViewModel.onAttached(mockk())
        mapboxAudioViewModel.invoke(AudioAction.Toggle)
        mapboxAudioViewModel.onDetached(mockk())

        verify(exactly = 1) { mapboxAudioApi.speakVoiceInstructions() }
        assertTrue(mapboxAudioViewModel.state.value.isMuted)
    }
}
