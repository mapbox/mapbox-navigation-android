package com.mapbox.navigation.ui.voice.internal.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.R
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
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
            contractProvider = { testContract }
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onAttach - should update button style`() {
        sut.onAttached(mapboxNavigation)

        verify { button.updateStyle(R.style.MapboxStyleAudioGuidanceButton) }
    }

    @Test
    fun `onAttach - should update button state`() {
        testContract.isMuted.value = false

        sut.onAttached(mapboxNavigation)
        testContract.isMuted.value = true

        verifyOrder {
            button.unMute()
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
        val testState = MutableStateFlow<MapboxAudioGuidance.State>(
            TestMapboxAudioGuidanceState(isMuted = false)
        )
        val mockAudioGuidance = mockk<MapboxAudioGuidance> {
            every { stateFlow() } returns testState
            every { mute() } returns Unit
            every { unmute() } returns Unit
            every { toggle() } returns Unit
        }
        every {
            MapboxNavigationApp.getObserver(MapboxAudioGuidance::class)
        } returns mockAudioGuidance

        val sut = AudioGuidanceButtonComponent(button)
        sut.onAttached(mapboxNavigation)
        button.performClick()

        verify { mockAudioGuidance.mute() }
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

    private data class TestMapboxAudioGuidanceState(
        override val isPlayable: Boolean = false,
        override val isMuted: Boolean = false,
        override val voiceInstructions: VoiceInstructions? = null,
        override val speechAnnouncement: SpeechAnnouncement? = null,
    ) : MapboxAudioGuidance.State
}
