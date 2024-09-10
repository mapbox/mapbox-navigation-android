package com.mapbox.navigation.voice.options

import android.media.AudioAttributes
import android.media.AudioManager
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class VoiceInstructionsPlayerOptionsTest :
    BuilderTest<VoiceInstructionsPlayerOptions, VoiceInstructionsPlayerOptions.Builder>() {

    override fun getImplementationClass(): KClass<VoiceInstructionsPlayerOptions> =
        VoiceInstructionsPlayerOptions::class

    override fun getFilledUpBuilder(): VoiceInstructionsPlayerOptions.Builder =
        VoiceInstructionsPlayerOptions.Builder()
            .focusGain(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .streamType(AudioManager.STREAM_RING)
            .ttsStreamType(AudioManager.STREAM_DTMF)
            .usage(AudioAttributes.USAGE_MEDIA)
            .contentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .useLegacyApi(true)
            .checkIsLanguageAvailable(false)
            .abandonFocusDelay(2000L)

    @Test
    override fun trigger() {
        // read doc
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid focus gain`() {
        VoiceInstructionsPlayerOptions.Builder().focusGain(AudioManager.AUDIOFOCUS_NONE)
    }
}
