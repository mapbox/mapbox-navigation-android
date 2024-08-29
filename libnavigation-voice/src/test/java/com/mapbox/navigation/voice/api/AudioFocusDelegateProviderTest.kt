package com.mapbox.navigation.voice.api

import android.media.AudioManager
import android.os.Build.VERSION_CODES.LOLLIPOP
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AudioFocusDelegateProviderTest {

    @Test
    fun `retrieve oreo and later audio focus delegate if oreo and above`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        val actual = AudioFocusDelegateProvider.defaultAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        assertTrue(actual is OreoAndLaterAudioFocusDelegate)
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun `retrieve pre oreo audio focus delegate if below oreo`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.focusGain
        } returns AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk()
        every {
            mockedPlayerAttributes.options
        } returns mockedPlayerOptions

        val actual = AudioFocusDelegateProvider.defaultAudioFocusDelegate(
            mockedAudioManager,
            mockedPlayerAttributes,
        )

        assertTrue(actual is PreOreoAudioFocusDelegate)
    }
}
