package com.mapbox.navigation.ui.voice.options

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PlayerAttributesProviderTest {

    @Test
    fun `retrieve oreo and later player attributes if oreo and above`() {
        val actual = PlayerAttributesProvider.retrievePlayerAttributes()
        assertTrue(actual is PlayerAttributes.OreoAndLaterAttributes)
    }

    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    @Test
    fun `retrieve pre oreo player attributes if below oreo`() {
        val actual = PlayerAttributesProvider.retrievePlayerAttributes()
        assertTrue(actual is PlayerAttributes.PreOreoAttributes)
    }
}
