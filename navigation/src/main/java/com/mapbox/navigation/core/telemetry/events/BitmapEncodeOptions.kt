package com.mapbox.navigation.core.telemetry.events

import androidx.annotation.IntRange

private const val DEFAULT_BITMAP_ENCODE_WIDTH = 250
private const val DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY = 20

/**
 * Contains options for encoding screenshots.
 *
 * @param width maximum width of an encoded screenshot, default is 250.
 * @param compressQuality quality of an encoded screenshot, ranging from 0 to 100, default is 20.
 */
class BitmapEncodeOptions private constructor(val width: Int, val compressQuality: Int) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .compressQuality(compressQuality)
            .width(width)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BitmapEncodeOptions

        if (width != other.width) return false
        if (compressQuality != other.compressQuality) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + compressQuality
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "BitmapEncodeOptions(width=$width, compressQuality=$compressQuality)"
    }

    /**
     * Build your [BitmapEncodeOptions].
     */
    class Builder {

        private var width = DEFAULT_BITMAP_ENCODE_WIDTH
        private var compressQuality = DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY

        /**
         * Updates maximum width of an encoded screenshot, default is 250.
         *
         * @throws IllegalStateException if [width] is less than 1.
         */
        fun width(@IntRange(from = 1) width: Int): Builder = apply {
            require(width >= 1) { "width must be >= 1" }
            this.width = width
        }

        /**
         * Updates quality of an encoded screenshot, default is 20.
         *
         * @throws IllegalStateException if [width] is outside [0, 100].
         */
        fun compressQuality(@IntRange(from = 0, to = 100) compressQuality: Int): Builder = apply {
            require(compressQuality in 0..100) { "compressQuality must be in 0..100" }
            this.compressQuality = compressQuality
        }

        /**
         * Build the object.
         */
        fun build(): BitmapEncodeOptions {
            return BitmapEncodeOptions(width, compressQuality)
        }
    }
}
