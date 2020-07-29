package com.mapbox.navigation.ui.internal.utils

const val DEFAULT_BITMAP_ENCODE_WIDTH = 250
const val DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY = 20

class BitmapEncodeOptions private constructor(val width: Int, val compressQuality: Int) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder() = Builder()
        .compressQuality(compressQuality)
        .width(width)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BitmapEncodeOptions

        if (width != other.width) return false
        if (compressQuality != other.compressQuality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + compressQuality
        return result
    }

    override fun toString(): String {
        return "BitmapEncodeOptions(width=$width, compressQuality=$compressQuality)"
    }

    class Builder {
        private var width: Int = DEFAULT_BITMAP_ENCODE_WIDTH
        private var compressQuality: Int = DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY

        fun width(width: Int) = apply {
            if (width <= 0) {
                throw IllegalArgumentException("width must be > 0")
            }
            this.width = width
        }

        fun compressQuality(compressQuality: Int) = apply {
            if (compressQuality !in 0..100) {
                throw IllegalArgumentException("compressQuality must be 0..100")
            }
            this.compressQuality = compressQuality
        }

        fun build(): BitmapEncodeOptions {
            return BitmapEncodeOptions(width, compressQuality)
        }
    }
}
