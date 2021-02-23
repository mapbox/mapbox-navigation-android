package com.mapbox.navigation.ui.voice.options

import android.media.AudioManager
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class VoiceInstructionsPlayerOptionsTest :
    BuilderTest<VoiceInstructionsPlayerOptions, VoiceInstructionsPlayerOptions.Builder>() {

    override fun getImplementationClass(): KClass<VoiceInstructionsPlayerOptions> =
        VoiceInstructionsPlayerOptions::class

    override fun getFilledUpBuilder(): VoiceInstructionsPlayerOptions.Builder =
        VoiceInstructionsPlayerOptions.Builder().focusGain(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)

    @Test
    override fun trigger() {
        // read doc
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid focus gain`() {
        VoiceInstructionsPlayerOptions.Builder().focusGain(AudioManager.AUDIOFOCUS_NONE)
    }
}
