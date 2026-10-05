package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.mapbox.navigation.ui.androidauto.R

/**
 * Modify the look of the maneuver icon.
 */
class CarManeuverIconOptions private constructor(
    val context: Context,
    @ColorInt
    val background: Int = Color.TRANSPARENT,
    @StyleRes
    val styleRes: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(context).apply {
        background(background)
        styleRes(styleRes)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CarManeuverIconOptions

        if (context != other.context) return false
        if (background != other.background) return false
        if (styleRes != other.styleRes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + background
        result = 31 * result + styleRes
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "CarManeuverIconOptions(" +
            "context=$context, " +
            "background=$background, " +
            "styleRes=$styleRes" +
            ")"
    }

    /**
     * Build a new [CarManeuverIconOptions]
     *
     * @param context optional Context for applying default themes.
     */
    class Builder(
        private val context: Context,
    ) {
        @ColorInt
        private var background: Int? = null

        @StyleRes
        private var styleRes: Int? = null

        /**
         * Theme that represents the active state for a lane
         */
        fun background(@ColorInt background: Int) = apply {
            this.background = background
        }

        /**
         * Theme that represents the lane icon
         */
        fun styleRes(@StyleRes styleRes: Int?) = apply {
            this.styleRes = styleRes
        }

        /**
         * Build the [CarManeuverIconOptions]
         */
        fun build(): CarManeuverIconOptions {
            return CarManeuverIconOptions(
                context = context,
                background = background ?: DEFAULT_BACKGROUND,
                styleRes = styleRes ?: DEFAULT_THEME,
            )
        }

        private companion object {
            private const val DEFAULT_BACKGROUND = Color.TRANSPARENT
            private val DEFAULT_THEME = R.style.CarManeuverTheme
        }
    }
}
