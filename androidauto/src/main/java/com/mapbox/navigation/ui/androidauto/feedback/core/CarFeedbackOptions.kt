package com.mapbox.navigation.ui.androidauto.feedback.core

import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions

/**
 * Modify the car feedback.
 */
class CarFeedbackOptions private constructor(
    val bitmapEncodeOptions: BitmapEncodeOptions,
) {
    class Builder {
        private val bitmapEncodeOptions: BitmapEncodeOptions? = null

        fun build(): CarFeedbackOptions {
            return CarFeedbackOptions(
                bitmapEncodeOptions = bitmapEncodeOptions ?: defaultBitmapEncodeOptions,
            )
        }
    }

    private companion object {
        private const val BITMAP_COMPRESS_QUALITY = 50
        private const val BITMAP_WIDTH = 800
        private val defaultBitmapEncodeOptions by lazy {
            BitmapEncodeOptions.Builder()
                .compressQuality(BITMAP_COMPRESS_QUALITY)
                .width(BITMAP_WIDTH)
                .build()
        }
    }
}
