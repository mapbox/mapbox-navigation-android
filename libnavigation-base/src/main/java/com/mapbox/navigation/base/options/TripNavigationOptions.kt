package com.mapbox.navigation.base.options

import android.graphics.Color
import androidx.annotation.ColorRes
import com.mapbox.navigation.base.model.route.RouteConstants.NONE_SPECIFIED
import com.mapbox.navigation.base.model.route.RouteConstants.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

data class TripNavigationOptions internal constructor(
    private val roundingIncrement: Int,
    private val timeFormatType: Int,
    private val defaultNotificationColorId: Int
) {

    @RoundingIncrement
    fun roundingIncrement() = roundingIncrement

    @TimeFormatType
    fun timeFormatType() = timeFormatType

    /**
     * The color resource id for the default notification.  This will be ignored
     * if a [NavigationNotification] is set.
     *
     * @return color resource id for notification
     */
    @ColorRes
    fun defaultNotificationColorId() = defaultNotificationColorId

    class Builder {
        var roundingIncrement = ROUNDING_INCREMENT_FIFTY
        var timeFormatType = NONE_SPECIFIED
        var defaultNotificationColorId = Color.parseColor("#2D4E73")

        fun roundingIncrement(@RoundingIncrement roundingIncrement: Int) =
                apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(@TimeFormatType type: Int) =
                apply { this.timeFormatType = type }

        /**
         * Optionally, set the background color of the default notification.
         *
         * @param defaultNotificationColorId the color resource to be used
         * @return this builder for chaining operations together
         */
        fun defaultNotificationColorId(@ColorRes defaultNotificationColorId: Int) =
                apply { this.defaultNotificationColorId = defaultNotificationColorId }

        fun build(): TripNavigationOptions {
            return TripNavigationOptions(
                    roundingIncrement,
                    timeFormatType,
                    defaultNotificationColorId
            )
        }
    }
}
