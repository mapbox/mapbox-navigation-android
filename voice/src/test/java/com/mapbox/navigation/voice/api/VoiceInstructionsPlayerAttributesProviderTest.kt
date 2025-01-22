package com.mapbox.navigation.voice.api

import android.os.Build
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class VoiceInstructionsPlayerAttributesProviderTest {

    @Test
    fun `retrieve oreo and later player attributes if oreo and above`() {
        val actual = VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(
            VoiceInstructionsPlayerOptions.Builder().build(),
        )

        assertTrue(actual is VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes)
    }

    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    @Test
    fun `retrieve pre oreo player attributes if below oreo`() {
        val actual = VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(
            VoiceInstructionsPlayerOptions.Builder().build(),
        )

        assertTrue(actual is VoiceInstructionsPlayerAttributes.PreOreoAttributes)
    }
}
