package com.mapbox.navigation.voice.api

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_REQUEST_DELAYED
import android.media.AudioManager.AUDIOFOCUS_REQUEST_FAILED
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import com.mapbox.navigation.voice.model.AudioFocusOwner
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
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
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        val mockOwner: AudioFocusOwner = mockk()
        oreoAndLaterAudioFocusDelegate.requestFocus(mockOwner, mockCallback)

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            slotAudioFocusRequest.captured.focusGain,
        )
    }

    @Test
    fun `oreo and later audio focus delegate request focus with voice assistance attribute`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.usage
        } returns AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        every {
            mockedPlayerOptions.useLegacyApi
        } returns false
        every {
            mockedPlayerOptions.contentType
        } returns AudioAttributes.CONTENT_TYPE_MUSIC

        val playerAttributes =
            VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(mockedPlayerOptions)

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            playerAttributes,
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        val mockOwner: AudioFocusOwner = mockk()
        oreoAndLaterAudioFocusDelegate.requestFocus(mockOwner, mockCallback)

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
        assertEquals(
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            slotAudioFocusRequest.captured.audioAttributes.usage,
        )
    }

    @Test
    fun `oreo and later delegate requestFocus returns false when requestAudioFocus is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_GRANTED

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        val mockOwner: AudioFocusOwner = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        oreoAndLaterAudioFocusDelegate.requestFocus(mockOwner, mockCallback)

        assertEquals(
            true,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later delegate requestFocus returns false when requestAudioFocus is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_FAILED

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        val mockOwner: AudioFocusOwner = mockk()
        oreoAndLaterAudioFocusDelegate.requestFocus(mockOwner, mockCallback)

        assertEquals(
            false,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later delegate requestFocus returns true when requestAudioFocus is delayed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )
        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.requestAudioFocus(any())
        } returns AUDIOFOCUS_REQUEST_DELAYED

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        val mockOwner: AudioFocusOwner = mockk()
        oreoAndLaterAudioFocusDelegate.requestFocus(mockOwner, mockCallback)

        assertEquals(
            true,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.requestAudioFocus(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later abandon focus returns true when abandonAudioFocusRequest is granted`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.abandonAudioFocusRequest(any())
        } returns AUDIOFOCUS_REQUEST_GRANTED

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        oreoAndLaterAudioFocusDelegate.abandonFocus(mockCallback)

        assertEquals(
            true,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocusRequest(capture(slotAudioFocusRequest))
        }
    }

    @Test
    fun `oreo and later abandon focus returns false when abandonAudioFocusRequest is failed`() {
        val mockedAudioManager = mockk<AudioManager>(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        every {
            mockedPlayerAttributes.applyOn(any(), any<AudioFocusRequest.Builder>())
        } returns Unit

        val oreoAndLaterAudioFocusDelegate = OreoAndLaterAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        val slotAudioFocusRequest = slot<AudioFocusRequest>()

        every {
            mockedAudioManager.abandonAudioFocusRequest(any())
        } returns AUDIOFOCUS_REQUEST_FAILED

        val slotResult = slot<Boolean>()
        val mockCallback: AudioFocusRequestCallback = mockk()
        every { mockCallback.invoke(capture(slotResult)) } returns Unit
        oreoAndLaterAudioFocusDelegate.abandonFocus(mockCallback)

        assertEquals(
            false,
            slotResult.captured,
        )

        verify(exactly = 1) {
            mockedAudioManager.abandonAudioFocusRequest(capture(slotAudioFocusRequest))
        }
    }
}
