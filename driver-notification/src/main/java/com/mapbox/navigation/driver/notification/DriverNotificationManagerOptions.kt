package com.mapbox.navigation.driver.notification

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration options for the `DriverNotificationManager`.
 *
 * This class provides a way to configure the behavior of the `DriverNotificationManager`.
 * It can be customized using the `Builder` class to create an instance with specific settings.
 *
 * @see [DriverNotificationManager] for managing driver notification providers
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DriverNotificationManagerOptions internal constructor() {

    /**
     * Creates a builder to modify the current options.
     */
    fun toBuilder(): Builder {
        return Builder()
    }

    /**
     * Checks equality between this instance and another object.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    /**
     * Gets the hash code for this instance.
     */
    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    /**
     * Returns a string representation of this instance.
     */
    override fun toString(): String {
        return "DriverNotificationManagerOptions()"
    }

    /**
     * Builder class for creating or modifying `DriverNotificationManagerOptions`.
     */
    class Builder {

        /**
         * Builds a new instance of `DriverNotificationManagerOptions` with the current settings.
         */
        fun build(): DriverNotificationManagerOptions {
            return DriverNotificationManagerOptions()
        }
    }
}
