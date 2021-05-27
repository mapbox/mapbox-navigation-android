package com.mapbox.navigation.ui.voice.options

import android.media.AudioAttributes
import android.media.AudioManager
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(NavSDKRobolectricTestRunner::class)
class VoiceInstructionsPlayerOptionsTest :
    BuilderTest<VoiceInstructionsPlayerOptions, VoiceInstructionsPlayerOptions.Builder>() {

    override fun getImplementationClass(): KClass<VoiceInstructionsPlayerOptions> =
        VoiceInstructionsPlayerOptions::class

    override fun getFilledUpBuilder(): VoiceInstructionsPlayerOptions.Builder =
        VoiceInstructionsPlayerOptions.Builder()
            .focusGain(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .streamType(AudioManager.STREAM_RING)
            .usage(AudioAttributes.USAGE_MEDIA)
            .contentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .useLegacyApi(true)

    @Test
    override fun trigger() {
        // read doc
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid focus gain`() {
        VoiceInstructionsPlayerOptions.Builder().focusGain(AudioManager.AUDIOFOCUS_NONE)
    }
}
