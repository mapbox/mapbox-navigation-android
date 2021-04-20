package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class PreOreoAudioFocusDelegateTest {

    @Test
    fun `pre oreo audio focus delegate request focus`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        preOreoAudioFocusDelegate.requestFocus()

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns true when audio focus is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        Assert.assertEquals(
            true,
            preOreoAudioFocusDelegate.requestFocus(),
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns false when audio focus is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        Assert.assertEquals(
            preOreoAudioFocusDelegate.requestFocus(),
            false
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns false when audio focus is delayed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedVoiceInstructionsPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_DELAYED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        Assert.assertEquals(
            preOreoAudioFocusDelegate.requestFocus(),
            false
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    @Test
    fun `pre oreo audio focus delegate abandon focus`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(mockedAudioManager, mockk())

        preOreoAudioFocusDelegate.abandonFocus()

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocus(null)
        }
    }
}
