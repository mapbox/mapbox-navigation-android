package com.mapbox.navigation.ui.androidauto.feedback.core

import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MapboxCarOptionsFeedbackTest {

    @Test
    fun `default carFeedbackOptions uses the default builder values`() {
        val options = MapboxCarOptions()

        assertEquals(CarFeedbackOptions.Builder().build(), options.carFeedbackOptions)
    }

    @Test
    fun `customization replaces carFeedbackOptions`() {
        val options = MapboxCarOptions()
        val feedbackOptions = CarFeedbackOptions.Builder()
            .bitmapEncodeOptions(BitmapEncodeOptions.Builder().width(400).build())
            .build()

        options.applyCustomization(
            MapboxCarOptions.Customization().apply { carFeedbackOptions = feedbackOptions },
        )

        assertSame(feedbackOptions, options.carFeedbackOptions)
    }

    @Test
    fun `customization without carFeedbackOptions keeps the previous value`() {
        val options = MapboxCarOptions()
        val feedbackOptions = CarFeedbackOptions.Builder()
            .bitmapEncodeOptions(BitmapEncodeOptions.Builder().width(400).build())
            .build()
        options.applyCustomization(
            MapboxCarOptions.Customization().apply { carFeedbackOptions = feedbackOptions },
        )

        options.applyCustomization(MapboxCarOptions.Customization())

        assertSame(feedbackOptions, options.carFeedbackOptions)
    }

    @Test
    fun `customization replaces feedbackPollProvider`() {
        val options = MapboxCarOptions()
        val provider = object : CarFeedbackPollProvider() {}

        options.applyCustomization(
            MapboxCarOptions.Customization().apply { feedbackPollProvider = provider },
        )

        assertSame(provider, options.feedbackPollProvider)
    }
}
