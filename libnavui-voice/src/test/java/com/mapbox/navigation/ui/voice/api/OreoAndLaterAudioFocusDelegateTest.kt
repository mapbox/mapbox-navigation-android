package com.mapbox.navigation.ui.voice.api

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_REQUEST_DELAYED
import android.media.AudioManager.AUDIOFOCUS_REQUEST_FAILED
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OreoAndLaterAudioFocusDelegateTest {

    @Test
    fun `oreo and later audio focus delegate request focus with gain transient may duck`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        oreoAndLaterAudioFocusDelegate.requestFocus()

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            slotAudioFocusRequest.captured.focusGain
        )
    }

    @Test
    fun `oreo and later audio delegates return true when audio focus is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_GRANTED

        assertEquals(
            oreoAndLaterAudioFocusDelegate.requestFocus(),
            true
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later audio delegates return false when audio focus is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_FAILED

        assertEquals(
            oreoAndLaterAudioFocusDelegate.requestFocus(),
            false
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later audio delegates return false when audio focus is delayed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_DELAYED

        assertEquals(
            oreoAndLaterAudioFocusDelegate.requestFocus(),
            false
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later audio focus delegate abandon focus`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val requestSlotAudioFocusRequest = slot<AudioFocusRequest>()
        every {
            mockedAudioManager.requestAudioFocus(capture(requestSlotAudioFocusRequest))
        } returns AUDIOFOCUS_REQUEST_GRANTED
        oreoAndLaterAudioFocusDelegate.requestFocus()

        oreoAndLaterAudioFocusDelegate.abandonFocus()

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocusRequest(requestSlotAudioFocusRequest.captured)
        }
    }
}
