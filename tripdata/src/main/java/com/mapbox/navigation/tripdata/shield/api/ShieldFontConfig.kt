package com.mapbox.navigation.tripdata.shield.api

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.svg.MapboxNavigationSVGExternalFileResolver

/**
 * Configuration for font properties used to resolve a specific typeface during SVG rendering.
 * This process relies on an `SVGExternalFileResolver` that has been registered with
 * `SVG#registerExternalFileResolver`.
 *
 * To ensure compatibility, the values you provide for your font configuration must be supported
 * by the registered `SVGExternalFileResolver`. For the Mapbox implementation, please refer to the
 * [MapboxNavigationSVGExternalFileResolver] class, which  contains detailed information on the
 * supported fonts and configuration specifics.
 *
 * @property fontFamily The font family name.
 * @property fontWeight Font weight.
 * @property fontStyle Font style.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ShieldFontConfig private constructor(
    val fontFamily: String,
    @FontWeight val fontWeight: Int,
    @FontStyle val fontStyle: String?,
) {

    /***
     * Possible font style values are defined according to the [Tiny SVG 1.2 specification](https://www.w3.org/TR/SVGTiny12/text.html#FontStyleProperty).
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(FontStyle.NORMAL, FontStyle.ITALIC, FontStyle.OBLIQUE)
    @ExperimentalPreviewMapboxNavigationAPI
    annotation class FontStyle {
        companion object {
            const val NORMAL = "normal"
            const val ITALIC = "italic"
            const val OBLIQUE = "oblique"
        }
    }

    /***
     *  Numeric font weight values defined according to the
     *  [Tiny SVG 1.2 specification](https://www.w3.org/TR/SVGTiny12/text.html#FontWeightProperty).
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        FontWeight.THIN,
        FontWeight.EXTRA_LIGHT,
        FontWeight.LIGHT,
        FontWeight.NORMAL,
        FontWeight.MEDIUM,
        FontWeight.SEMI_BOLD,
        FontWeight.BOLD,
        FontWeight.EXTRA_BOLD,
        FontWeight.BLACK,
    )
    @ExperimentalPreviewMapboxNavigationAPI
    annotation class FontWeight {
        companion object {
            const val THIN = 100
            const val EXTRA_LIGHT = 200
            const val LIGHT = 300
            const val NORMAL = 400
            const val MEDIUM = 500
            const val SEMI_BOLD = 600
            const val BOLD = 700
            const val EXTRA_BOLD = 800
            const val BLACK = 900
        }
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder(fontFamily)
        .fontWeight(fontWeight)
        .fontStyle(fontStyle)

    /**
     * Build a new [ShieldFontConfig]
     * @param fontFamily The font family name (e.g., "Arial", "CustomFont")
     */
    class Builder(
        private val fontFamily: String,
    ) {
        @FontWeight
        private var fontWeight: Int = FontWeight.NORMAL

        @FontStyle
        private var fontStyle: String? = null

        init {
            require(fontFamily.isNotBlank()) { "Font family cannot be blank" }
        }

        /**
         * Sets the font weight for the shield text.
         * @param fontWeight font weight specified in [FontWeight].
         * @return This builder for chaining.
         * @throws IllegalArgumentException if fontWeight is not one of the allowed values (100, 200, ..., 900).
         */
        fun fontWeight(@FontWeight fontWeight: Int) = apply { this.fontWeight = fontWeight }

        /**
         * Sets the font style for the shield text.
         * @param fontStyle font style specified in [FontStyle].
         */
        fun fontStyle(@FontStyle fontStyle: String?): Builder = apply {
            fontStyle?.let {
                require(
                    it == FontStyle.NORMAL ||
                        it == FontStyle.ITALIC ||
                        it == FontStyle.OBLIQUE,
                ) {
                    "Font style must be 'normal', 'italic', or 'oblique'"
                }
            }
            this.fontStyle = fontStyle
        }

        /**
         * Build the [ShieldFontConfig]
         */
        fun build(): ShieldFontConfig = ShieldFontConfig(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
        )
    }

    /**
     * Checks if this object is equal to another object.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShieldFontConfig

        if (fontFamily != other.fontFamily) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = fontFamily.hashCode()
        result = 31 * result + fontWeight
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ShieldFontConfig(" +
            "fontFamily='$fontFamily', " +
            "fontWeight='$fontWeight', " +
            "fontStyle=$fontStyle" +
            ")"
    }
}
