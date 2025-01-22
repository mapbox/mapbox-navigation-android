package com.mapbox.navigation.voice.api

import android.media.AudioManager
import com.mapbox.navigation.voice.model.AudioFocusOwner
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class PreOreoAudioFocusDelegateTest {

    @Test
    fun `pre oreo audio focus delegate request focus for MediaPlayer`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.requestFocus(AudioFocusOwner.MediaPlayer, mockCallback)

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    @Test
    fun `pre oreo audio focus delegate request focus for TextToSpeech`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedPlayerOptions.ttsStreamType
        } returns AudioManager.STREAM_MUSIC

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.requestFocus(AudioFocusOwner.TextToSpeech, mockCallback)

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns true when audio focus is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.requestFocus(AudioFocusOwner.MediaPlayer, mockCallback)
        assertEquals(
            true,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns false when audio focus is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.requestFocus(AudioFocusOwner.MediaPlayer, mockCallback)

        assertEquals(
            false,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    @Test
    fun `pre oreo audio delegate returns true when audio focus is delayed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        } returns AudioManager.AUDIOFOCUS_REQUEST_DELAYED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.requestFocus(AudioFocusOwner.MediaPlayer, mockCallback)

        assertEquals(
            true,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    @Test
    fun `pre oreo audio focus delegate abandon focus`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(mockedAudioManager, mockk())

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.abandonFocus(mockCallback)

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocus(null)
        }
    }

    @Test
    fun `pre oreo audio focus delegate abandon focus returns true when focus is granted`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)

        every {
            mockedAudioManager.abandonAudioFocus(null)
        } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(mockedAudioManager, mockk())

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.abandonFocus(mockCallback)

        assertEquals(
            true,
            slotResult.captured,
        )
        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocus(null)
        }
    }

    @Test
    fun `pre oreo audio focus delegate abandon focus returns false when focus is failed`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)

        every {
            mockedAudioManager.abandonAudioFocus(null)
        } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED

        val preOreoAudioFocusDelegate = PreOreoAudioFocusDelegate(mockedAudioManager, mockk())

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        preOreoAudioFocusDelegate.abandonFocus(mockCallback)

        assertEquals(
            false,
            slotResult.captured,
        )
        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocus(null)
        }
    }
}
