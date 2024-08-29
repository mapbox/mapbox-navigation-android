package com.mapbox.navigation.ui.components.status.model

import androidx.annotation.DrawableRes
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * An object that creates new [Status] instances.
 */
@ExperimentalMapboxNavigationAPI
object StatusFactory {

    /**
     * Instantiate a new status with a given field values.
     *
     * @param message The message field value.
     * @param duration The duration field value in milliseconds. The default value is `0`.
     * @param animated The animate field value. The default value is `true`.
     * @param spinner The spinner field value. The default value is `false`.
     * @param icon The icon field value. The default value is `0`.
     */
    @JvmStatic
    @JvmOverloads
    fun buildStatus(
        message: String,
        duration: Long = 0,
        animated: Boolean = true,
        spinner: Boolean = false,
        @DrawableRes icon: Int = 0,
    ) = Status(message, duration, animated, spinner, icon)
}
