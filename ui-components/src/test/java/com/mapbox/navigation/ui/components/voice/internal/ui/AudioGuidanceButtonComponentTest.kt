package com.mapbox.navigation.ui.components.voice.internal.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.components.voice.view.MapboxAudioGuidanceButton
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.internal.MapboxAudioGuidanceStateFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioGuidanceButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var testContract: TestContract
    private lateinit var button: MapboxAudioGuidanceButton
    private lateinit var sut: AudioGuidanceButtonComponent

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
        ctx = ApplicationProvider.getApplicationContext()
        mapboxNavigation = mockk()
        testContract = spyk(TestContract())
        button = spyk(MapboxAudioGuidanceButton(ctx))
        sut = AudioGuidanceButtonComponent(
            audioGuidanceButton = button,
            contractProvider = { testContract },
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onAttach - should update button state`() {
        testContract.isMuted.value = false

        sut.onAttached(mapboxNavigation)
        testContract.isMuted.value = true

        verifyOrder {
            button.unmute()
            button.mute()
        }
    }

    @Test
    fun `onAttach - should update button visibility`() {
        testContract.isVisible.value = false

        sut.onAttached(mapboxNavigation)

        assertFalse(button.isVisible)
    }

    @Test
    fun `onAttach - onclick should mute voice instructions`() {
        testContract.isMuted.value = false
        sut.onAttached(mapboxNavigation)

        button.performClick()

        verify { testContract.mute() }
    }

    @Test
    fun `onAttach - onclick should unMute voice instructions`() {
        testContract.isMuted.value = true
        sut.onAttached(mapboxNavigation)

        button.performClick()

        verify { testContract.unMute() }
    }

    @Test
    fun `onAttach - default contract`() {
        val testState = MutableStateFlow(
            MapboxAudioGuidanceStateFactory.createMapboxAudioGuidanceState(isMuted = false),
        )
        val mockAudioGuidance = mockk<MapboxAudioGuidance> {
            every { stateFlow() } returns testState
            every { mute() } returns Unit
            every { unmute() } returns Unit
            every { toggle() } returns Unit
        }
        every {
            MapboxNavigationApp.getObservers(MapboxAudioGuidance::class)
        } returns listOf(mockAudioGuidance)

        val sut = AudioGuidanceButtonComponent(button)
        sut.onAttached(mapboxNavigation)
        button.performClick()

        verify { mockAudioGuidance.mute() }
    }

    @Test
    fun `onAttach - create MapboxAudioGuidance if none exists`() {
        val slotAudioGuidance = slot<MapboxAudioGuidance>()
        every {
            MapboxNavigationApp.getObservers(MapboxAudioGuidance::class)
        } returns emptyList()
        every {
            MapboxNavigationApp.registerObserver(capture(slotAudioGuidance))
        } returns MapboxNavigationApp

        val sut = AudioGuidanceButtonComponent(button)
        sut.onAttached(mapboxNavigation)
        button.performClick()

        assertTrue(slotAudioGuidance.isCaptured)
    }

    @Test
    fun `onDetached - should not handle button on click events`() {
        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        button.performClick()

        verify(exactly = 0) { testContract.mute() }
        verify(exactly = 0) { testContract.unMute() }
    }

    private open class TestContract : AudioComponentContract {
        override val isMuted = MutableStateFlow(false)
        override val isVisible = MutableStateFlow(false)

        override fun mute() {
            isMuted.value = true
        }

        override fun unMute() {
            isMuted.value = false
        }
    }
}
