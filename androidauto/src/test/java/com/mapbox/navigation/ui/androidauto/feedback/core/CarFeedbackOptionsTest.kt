package com.mapbox.navigation.ui.androidauto.feedback.core

import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarFeedbackOptionsTest : BuilderTest<CarFeedbackOptions, CarFeedbackOptions.Builder>() {

    override fun getImplementationClass() = CarFeedbackOptions::class

    override fun getFilledUpBuilder(): CarFeedbackOptions.Builder {
        return CarFeedbackOptions.Builder()
            .bitmapEncodeOptions(
                BitmapEncodeOptions.Builder().compressQuality(90).width(1024).build(),
            )
            .attachScreenshot(false)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test
    fun `default bitmap encode options keep the previous screenshot size and quality`() {
        val options = CarFeedbackOptions.Builder().build()

        assertEquals(50, options.bitmapEncodeOptions.compressQuality)
        assertEquals(800, options.bitmapEncodeOptions.width)
    }

    @Test
    fun `custom bitmap encode options are returned`() {
        val encodeOptions = BitmapEncodeOptions.Builder().compressQuality(10).width(300).build()

        val options = CarFeedbackOptions.Builder().bitmapEncodeOptions(encodeOptions).build()

        assertEquals(encodeOptions, options.bitmapEncodeOptions)
    }

    @Test
    fun `options with different bitmap encode options are not equal`() {
        val default = CarFeedbackOptions.Builder().build()
        val custom = getFilledUpBuilder().build()

        assertNotEquals(default, custom)
        assertNotEquals(default.hashCode(), custom.hashCode())
    }

    @Test
    fun `toBuilder keeps bitmap encode options`() {
        val original = getFilledUpBuilder().build()

        val rebuilt = original.toBuilder().build()

        assertEquals(original.bitmapEncodeOptions, rebuilt.bitmapEncodeOptions)
    }

    @Test
    fun `screenshots are attached by default`() {
        assertTrue(CarFeedbackOptions.Builder().build().attachScreenshot)
    }

    @Test
    fun `toBuilder keeps a disabled screenshot`() {
        val original = CarFeedbackOptions.Builder().attachScreenshot(false).build()

        val rebuilt = original.toBuilder().build()

        assertFalse(rebuilt.attachScreenshot)
        assertEquals(original, rebuilt)
    }

    @Test
    fun `options that differ only in attachScreenshot are not equal`() {
        val attached = CarFeedbackOptions.Builder().build()
        val detached = CarFeedbackOptions.Builder().attachScreenshot(false).build()

        assertNotEquals(attached, detached)
        assertNotEquals(attached.hashCode(), detached.hashCode())
    }
}
