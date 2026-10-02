package com.mapbox.navigation.ui.androidauto.feedback.core

import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions

/**
 * Modify the car feedback.
 *
 * @param bitmapEncodeOptions controls how a feedback screenshot is compressed before it's
 * attached to a submitted feedback item.
 * @param attachScreenshot whether a screenshot of the car map is taken when a feedback screen
 * opens and attached to the submitted feedback. The screenshot is sent with the navigation
 * feedback event and recorded in the history file. When `false`, no screenshot is taken and
 * feedback is submitted without one. Defaults to `true`.
 */
class CarFeedbackOptions private constructor(
    val bitmapEncodeOptions: BitmapEncodeOptions,
    val attachScreenshot: Boolean,
) {
    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        bitmapEncodeOptions(bitmapEncodeOptions)
        attachScreenshot(attachScreenshot)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CarFeedbackOptions

        if (bitmapEncodeOptions != other.bitmapEncodeOptions) return false
        if (attachScreenshot != other.attachScreenshot) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = bitmapEncodeOptions.hashCode()
        result = 31 * result + attachScreenshot.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "CarFeedbackOptions(" +
            "bitmapEncodeOptions=$bitmapEncodeOptions, " +
            "attachScreenshot=$attachScreenshot" +
            ")"
    }

    /**
     * Build a new [CarFeedbackOptions]
     */
    class Builder {
        private var bitmapEncodeOptions: BitmapEncodeOptions? = null
        private var attachScreenshot: Boolean = true

        /**
         * Override the screenshot encoding used when attaching a snapshot to feedback.
         */
        fun bitmapEncodeOptions(bitmapEncodeOptions: BitmapEncodeOptions) = apply {
            this.bitmapEncodeOptions = bitmapEncodeOptions
        }

        /**
         * Set to `false` to submit feedback without a screenshot of the car map.
         * Defaults to `true`.
         */
        fun attachScreenshot(attachScreenshot: Boolean) = apply {
            this.attachScreenshot = attachScreenshot
        }

        /**
         * Build the [CarFeedbackOptions]
         */
        fun build(): CarFeedbackOptions {
            return CarFeedbackOptions(
                bitmapEncodeOptions = bitmapEncodeOptions ?: defaultBitmapEncodeOptions,
                attachScreenshot = attachScreenshot,
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
