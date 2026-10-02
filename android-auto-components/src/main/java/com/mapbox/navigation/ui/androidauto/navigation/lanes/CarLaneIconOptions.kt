package com.mapbox.navigation.ui.androidauto.navigation.lanes

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.mapbox.navigation.ui.androidauto.R

/**
 * Modify the look and feel of the car lanes.
 */
class CarLaneIconOptions private constructor(
    val activeTheme: Resources.Theme,
    val notActiveTheme: Resources.Theme,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        activeTheme(activeTheme)
        notActiveTheme(notActiveTheme)
    }

    /**
     * Build a new [CarLaneIconOptions]
     */
    class Builder {
        private var activeTheme: Resources.Theme? = null
        private var notActiveTheme: Resources.Theme? = null

        /**
         * Theme that represents the active state for a lane
         */
        fun activeTheme(activeTheme: Resources.Theme) = apply {
            this.activeTheme = activeTheme
        }

        /**
         * Theme that represents the inactive state for a lane
         */
        fun notActiveTheme(notActiveTheme: Resources.Theme) = apply {
            this.notActiveTheme = notActiveTheme
        }

        /**
         * Build the [CarLaneIconOptions]
         *
         * @param context optional Context for applying default themes.
         * @throws NullPointerException when a [context] is null and the themes are not set
         */
        fun build(context: Context? = null): CarLaneIconOptions {
            return CarLaneIconOptions(
                activeTheme = activeTheme
                    ?: defaultTheme(context, DEFAULT_ACTIVE_THEME),
                notActiveTheme = notActiveTheme
                    ?: defaultTheme(context, DEFAULT_NOT_ACTIVE_THEME),
            )
        }

        private companion object {
            private val DEFAULT_ACTIVE_THEME = R.style.CarLaneActiveTheme
            private val DEFAULT_NOT_ACTIVE_THEME = R.style.CarLaneNotActiveTheme

            private fun defaultTheme(context: Context?, @StyleRes style: Int) =
                ContextThemeWrapper(context!!.applicationContext, style).theme
        }
    }
}
