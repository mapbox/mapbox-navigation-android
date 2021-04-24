package com.mapbox.navigation.ui.voice.api

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build.VERSION_CODES.LOLLIPOP
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
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

        val actual = AudioFocusDelegateProvider.retrieveAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        assertTrue(actual is OreoAndLaterAudioFocusDelegate)
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun `retrieve pre oreo audio focus delegate if below oreo`() {
        val mockedAudioManager: AudioManager = mockk(relaxed = true)
        val mockedVoiceInstructionsPlayerOptions: VoiceInstructionsPlayerOptions = mockk()

        val actual = AudioFocusDelegateProvider.retrieveAudioFocusDelegate(
            mockedAudioManager,
            mockedVoiceInstructionsPlayerOptions
        )

        assertTrue(actual is PreOreoAudioFocusDelegate)
    }
}
