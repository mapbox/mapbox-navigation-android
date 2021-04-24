package com.mapbox.navigation.ui.voice.api

import android.media.AudioAttributes
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
        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

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
    fun `oreo and later audio focus delegate request focus with voice assistance attribute`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

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
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            slotAudioFocusRequest.captured.audioAttributes.usage
        )
    }

    @Test
    fun `oreo and later delegate requestFocus returns false when requestAudioFocus is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_GRANTED

        assertEquals(
            true,
            oreoAndLaterAudioFocusDelegate.requestFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later delegate requestFocus returns false when requestAudioFocus is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_FAILED

        assertEquals(
            false,
            oreoAndLaterAudioFocusDelegate.requestFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later delegate requestFocus returns true when requestAudioFocus is delayed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_DELAYED

        assertEquals(
            true,
            oreoAndLaterAudioFocusDelegate.requestFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later abandon focus returns true when abandonAudioFocusRequest is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.abandonAudioFocusRequest(any())
        } returns AUDIOFOCUS_REQUEST_GRANTED

        assertEquals(
            true,
            oreoAndLaterAudioFocusDelegate.abandonFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocusRequest(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later abandon focus returns false when abandonAudioFocusRequest is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedVoiceInstructionsPlayerOptions.audioAttributes
        } returns AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.abandonAudioFocusRequest(any())
        } returns AUDIOFOCUS_REQUEST_FAILED

        assertEquals(
            false,
            oreoAndLaterAudioFocusDelegate.abandonFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocusRequest(capture(slotAudioFocusRequest))
        }
    }
}
