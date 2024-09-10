package com.mapbox.navigation.ui.components.status.model

import androidx.annotation.DrawableRes

/**
 * This class stores information to be displayed by the [MapboxStatusView].
 */
class Status internal constructor(
    /**
     * The text for this status.
     */
    val message: String,

    /**
     * The duration in milliseconds after which this status should hide.
     * If this value is set to `0` or [Long.MAX_VALUE], this status will be displayed indefinitely.
     */
    val duration: Long,

    /**
     * Indicates if this status should animate when showing and hiding.
     */
    val animated: Boolean,

    /**
     * Indicates if an indeterminate ProgressBar should be displayed when showing this status.
     */
    val spinner: Boolean,

    /**
     * A Resource ID of the square Icon Drawable that should be displayed with this status.
     */
    @DrawableRes
    val icon: Int,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        // IMPORTANT! This method must be regenerated on any class field change.
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Status

        if (message != other.message) return false
        if (duration != other.duration) return false
        if (animated != other.animated) return false
        if (spinner != other.spinner) return false
        if (icon != other.icon) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        // IMPORTANT! This method must be regenerated on any class field change.
        var result = message.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + animated.hashCode()
        result = 31 * result + spinner.hashCode()
        result = 31 * result + icon
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        // IMPORTANT! This method must be regenerated on any class field change.
        return "Status(message='$message', duration=$duration, animated=$animated, spinner=$spinner, icon=$icon)" // ktlint-disable
    }
}
