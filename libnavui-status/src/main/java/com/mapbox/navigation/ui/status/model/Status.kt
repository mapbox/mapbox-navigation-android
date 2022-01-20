package com.mapbox.navigation.ui.status.model

import androidx.annotation.DrawableRes

/**
 * This class stores information to be displayed by the [MapboxStatusView].
 */
data class Status(
    /**
     * The text that will appear on the [MapboxStatusView]
     */
    val message: String,

    /**
     * The duration in milliseconds after which the [MapboxStatusView] should hide.
     * Setting this value to `0` or [Long.MAX_VALUE] will display the [MapboxStatusView] indefinitely.
     *
     * Defaults to `0`.
     */
    val duration: Long = 0,

    /**
     * Animate showing and hiding of the [MapboxStatusView].
     *
     * Defaults to `true`.
     */
    val animated: Boolean = true,

    /**
     * Show indeterminate ProgressBar on the [MapboxStatusView].
     * Defaults to `false`
     */
    val spinner: Boolean = false,

    /**
     * Resource ID of the square Icon Drawable to show on the [MapboxStatusView].
     * Defaults to `0`
     */
    @DrawableRes
    val icon: Int = 0
)
