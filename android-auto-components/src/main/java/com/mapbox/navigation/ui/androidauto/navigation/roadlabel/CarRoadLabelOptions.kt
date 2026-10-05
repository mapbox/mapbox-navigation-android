package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Modify the look and feel of the road label.
 */
class CarRoadLabelOptions private constructor(
    @ColorInt
    val backgroundColor: Int,
    @ColorInt
    val roundedLabelColor: Int,
    @ColorInt
    val shadowColor: Int?,
    @ColorInt
    val textColor: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        backgroundColor(backgroundColor)
        roundedLabelColor(roundedLabelColor)
        shadowColor(shadowColor)
        textColor(textColor)
    }

    /**
     * Build a new [CarRoadLabelOptions]
     */
    class Builder {
        private var backgroundColor: Int = DEFAULT_BACKGROUND_COLOR
        private var roundedLabelColor: Int = DEFAULT_ROUNDED_LABEL_COLOR
        private var shadowColor: Int? = DEFAULT_SHADOW_COLOR
        private var textColor: Int = DEFAULT_TEXT_COLOR

        /**
         * The rectangular background color.
         * default is [Color.TRANSPARENT]
         */
        fun backgroundColor(@ColorInt backgroundColor: Int) = apply {
            this.backgroundColor = backgroundColor
        }

        /**
         * The color of the shadow. The shadow will not show if when `null`.
         */
        fun shadowColor(@ColorInt shadowColor: Int?) = apply {
            this.shadowColor = shadowColor
        }

        /**
         * The color of the rounded label on top of the background.
         */
        fun roundedLabelColor(@ColorInt roundedLabelColor: Int) = apply {
            this.roundedLabelColor = roundedLabelColor
        }

        /**
         * The color of the text in the label.
         */
        fun textColor(@ColorInt textColor: Int) = apply {
            this.textColor = textColor
        }

        /**
         * Build the [CarRoadLabelOptions]
         */
        fun build(): CarRoadLabelOptions {
            return CarRoadLabelOptions(
                backgroundColor = backgroundColor,
                roundedLabelColor = roundedLabelColor,
                shadowColor = shadowColor,
                textColor = textColor,
            )
        }
    }

    companion object {
        val default = Builder().build()

        private const val DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT
        private const val DEFAULT_SHADOW_COLOR = 0xAB000000.toInt()
        private const val DEFAULT_ROUNDED_LABEL_COLOR = 0xFFFFFFFF.toInt()
        private const val DEFAULT_TEXT_COLOR = 0xFF000000.toInt()
    }
}
