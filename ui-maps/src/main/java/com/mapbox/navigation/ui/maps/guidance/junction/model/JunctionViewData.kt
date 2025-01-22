package com.mapbox.navigation.ui.maps.guidance.junction.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.utils.internal.logE
import java.io.ByteArrayInputStream

/**
 * A class that encapsulates the junction view data and its associated format type.
 *
 * This class is primarily used to store and manage visual representations of road junctions,
 * including the raw image data (as a byte array) and the format of the image (e.g., PNG, SVG).
 *
 * @property data A byte array that contains the raw junction view image data.
 * @property responseFormat The format of the image (e.g., "png", "svg").
 */
@ExperimentalPreviewMapboxNavigationAPI
class JunctionViewData internal constructor(
    val data: ByteArray,
    @ResponseFormat val responseFormat: String,
) {

    /**
     * Checks if the junction view data is in PNG format.
     * @return `true` if the data format is PNG, `false` otherwise.
     */
    val isPngData: Boolean
        get() = responseFormat == ResponseFormat.PNG

    /**
     * Checks if the junction view data is in SVG format.
     *
     * @return `true` if the data format is SVG, `false` otherwise.
     */
    val isSvgData: Boolean
        get() = responseFormat == ResponseFormat.SVG

    /**
     * Retrieves the junction view data as a [PngData] object if the data is in PNG format.
     * @return A [PngData] object containing the PNG image data, or `null` if the data is not in PNG format.
     */
    fun getPngData(): PngData? {
        return if (isPngData) {
            PngData(data)
        } else {
            null
        }
    }

    /**
     * Retrieves the junction view data as an [SvgData] object if the data is in SVG format.
     * @return An [SvgData] object containing the SVG image data, or `null` if the data is not in SVG format.
     */
    fun getSvgData(): SvgData? {
        return if (isSvgData) {
            SvgData(data)
        } else {
            null
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JunctionViewData

        if (!data.contentEquals(other.data)) return false
        return responseFormat == other.responseFormat
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + responseFormat.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "JunctionViewData(data=${data.contentToString()}, responseFormat='$responseFormat')"
    }

    /**
     * Annotation class representing the format of the received junction view.
     * Available values are:
     * - [ResponseFormat.PNG]
     * - [ResponseFormat.SVG]
     * - [ResponseFormat.UNKNOWN]
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        ResponseFormat.PNG,
        ResponseFormat.SVG,
        ResponseFormat.UNKNOWN,
    )
    annotation class ResponseFormat {

        companion object {

            /**
             * Received junction view is in PNG format.
             */
            const val PNG = "png"

            /**
             * Received junction view is in SVG format.
             */
            const val SVG = "svg"

            /**
             * Received junction view has unknown or unsupported image format.
             */
            const val UNKNOWN = "unknown"

            @JvmSynthetic
            @ResponseFormat
            internal fun createFromContentType(contentType: String): String {
                return when (contentType.lowercase()) {
                    "image/svg+xml" -> SVG
                    "image/png" -> PNG
                    else -> UNKNOWN
                }
            }
        }
    }

    /**
     * A class that represents PNG image data.
     * @property data The raw PNG image data as a byte array.
     */
    class PngData(val data: ByteArray) {

        /**
         * Decodes the PNG data into a [Bitmap] object using the specified decoding options.
         *
         * By default, the bitmap is decoded with the [Bitmap.Config.RGB_565] configuration to optimize memory usage.
         * Custom options can be provided via the `options` parameter to control the decoding behavior.
         *
         * @param options A [BitmapFactory.Options] object to customize the decoding process.
         * Defaults to [Bitmap.Config.RGB_565] configuration.
         *
         * @return A [Bitmap] object if decoding is successful, or `null` if the data cannot be decoded.
         */
        fun getAsBitmap(options: BitmapFactory.Options = DEFAULT_DECODE_OPTIONS): Bitmap? {
            return BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PngData

            return data.contentEquals(other.data)
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "PngData(data=${data.contentToString()})"
        }

        private companion object {
            val DEFAULT_DECODE_OPTIONS = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        }
    }

    /**
     * A class that represents SVG image data.
     * @property data The raw SVG image data as a byte array.
     */
    class SvgData(val data: ByteArray) {

        /**
         * Renders the SVG data into a [Bitmap] object with specified width and height.
         *
         * @param width The width of the bitmap.
         * @param height The height of the bitmap.
         * @return A [Bitmap] object containing the rendered SVG, or `null` if the SVG parsing or rendering fails.
         */
        fun getAsBitmap(width: Int, height: Int): Bitmap? {
            return try {
                return SvgUtil.renderAsBitmapWith(ByteArrayInputStream(data), width, height)
            } catch (e: Exception) {
                logE {
                    "Unable to convert SVG to Bitmap: $e"
                }
                null
            }
        }

        /**
         * Renders the SVG data into a [Bitmap] object with specified width.
         * The height will be calculated based on SVG aspect ratio.
         *
         * @param width The width of the bitmap.
         * @return A [Bitmap] object containing the rendered SVG, or `null` if the SVG parsing or rendering fails.
         */
        fun getAsBitmap(width: Int): Bitmap? {
            return try {
                SvgUtil.renderAsBitmapWithWidth(ByteArrayInputStream(data), width)
            } catch (e: Exception) {
                logE {
                    "Unable to render SVG: $e"
                }
                null
            }
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SvgData

            return data.contentEquals(other.data)
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "SvgData(data=${data.contentToString()})"
        }
    }
}
