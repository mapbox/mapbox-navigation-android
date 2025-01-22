package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.content.res.AssetManager
import android.graphics.Typeface
import com.caverock.androidsvg.SVGExternalFileResolver
import com.mapbox.navigation.utils.internal.logE

/**
 * Mapbox implementation of the [SVGExternalFileResolver] to better support font resolution.
 *
 * See [resolveFont].
 */
open class MapboxExternalFileResolver(
    private val assetManager: AssetManager,
) : SVGExternalFileResolver() {

    /**
     * Called by renderer to resolve font references in &lt;text&gt; elements.
     *
     * @param fontFamily Font family as specified in a font-family attribute of css.
     * @param fontWeight Font weight as specified in a font-weight attribute of css.
     * @param fontStyle Font style as specified in a font-style attribute of css.
     *
     * As per css stylesheet associated as well as the font files supported by the provider
     * data-set, it supports the following values for font-weight and font-style
     *
     * --------------------------------------
     * font-weight | NORMAL: 400 | BOLD: 700
     * --------------------------------------
     * font-style | NORMAL | ITALIC | OBLIQUE
     * --------------------------------------
     *
     * Filename should be in the format: $font-family-$font-style$font-weight
     *
     * For example for the given input:
     *
     * ------------------------------------------------------------------
     * font-family | font-style | font-weight | fileName
     * ------------------------------------------------------------------
     *   SignText  |   Normal   |    Bold     |  SignText-Bold.ttf
     *   SignText  |   Italic   |   Normal    |  SignText-Italic.ttf
     *   SignText  |   Italic   |    Bold     |  SignText-ItalicBold.ttf
     *   SignText  |   Normal   |   Normal    |  SignText.ttf
     * ------------------------------------------------------------------
     *
     * Looking above it's understood that whenever style or weight value is normal it's
     * represented by empty string
     */
    override fun resolveFont(fontFamily: String?, fontWeight: Int, fontStyle: String?): Typeface? {
        try {
            return getTypeface(fontFamily, fontWeight, fontStyle, ".ttf")
        } catch (exception: RuntimeException) {
            logE(
                "exception: $exception",
                LOG_CATEGORY,
            )
        }

        return try {
            getTypeface(fontFamily, fontWeight, fontStyle, ".otf")
        } catch (exception: RuntimeException) {
            logE(
                "exception: $exception",
                LOG_CATEGORY,
            )
            null
        }
    }

    private fun getTypeface(
        fontFamily: String?,
        fontWeight: Int,
        fontStyle: String?,
        typefaceType: String,
    ): Typeface {
        val style = fontStyle?.let { s ->
            return@let when {
                s.contains(ITALIC) -> ITALIC
                s.contains(OBLIQUE) -> OBLIQUE
                else -> ""
            }
        } ?: ""

        val weight = when (fontWeight) {
            700 -> BOLD
            else -> ""
        }

        val fileName = if (style.isNotEmpty() && weight.isEmpty()) {
            "$fontFamily-$style"
        } else if (style.isEmpty() && weight.isNotEmpty()) {
            "$fontFamily-$weight"
        } else if (style.isNotEmpty() && weight.isNotEmpty()) {
            "$fontFamily-$style$weight"
        } else {
            "$fontFamily"
        }

        return when (typefaceType) {
            ".ttf" -> Typeface.createFromAsset(assetManager, "$fileName.ttf")
            ".otf" -> Typeface.createFromAsset(assetManager, "$fileName.otf")
            else -> Typeface.createFromAsset(assetManager, "$fileName.ttf")
        }
    }

    private companion object {

        private const val BOLD = "Bold"
        private const val ITALIC = "Italic"
        private const val OBLIQUE = "Oblique"
        private const val LOG_CATEGORY = "MapboxExternalFileResolver"
    }
}
