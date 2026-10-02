package com.mapbox.navigation.ui.androidauto.notification

import androidx.car.app.CarAppService

/**
 * Contains options for the Mapbox Car Trip Notification.
 *
 * @param startAppService [CarAppService]
 */
class MapboxCarNotificationOptions private constructor(
    val startAppService: Class<out CarAppService>?,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .startAppService(startAppService)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxCarNotificationOptions

        if (startAppService != other.startAppService) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return startAppService.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CarNotificationOptions(startAppService=$startAppService)"
    }

    /**
     * Build your [MapboxCarNotificationOptions].
     */
    class Builder {
        private var startAppService: Class<out CarAppService>? = null

        /**
         * [CarAppService] that is launched when the user taps on the notification.
         */
        fun startAppService(startAppService: Class<out CarAppService>?) = apply {
            this.startAppService = startAppService
        }

        /**
         * Build the object.
         */
        fun build(): MapboxCarNotificationOptions {
            return MapboxCarNotificationOptions(startAppService)
        }
    }
}
