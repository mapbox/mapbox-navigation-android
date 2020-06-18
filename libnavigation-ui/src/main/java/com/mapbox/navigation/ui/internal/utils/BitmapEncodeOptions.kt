package com.mapbox.navigation.ui.internal.utils

const val DEFAULT_BITMAP_ENCODE_WIDTH = 250
const val DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY = 20

data class BitmapEncodeOptions(val width: Int, val compressQuality: Int) {

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
