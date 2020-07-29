package com.mapbox.navigation.base.options

import java.lang.annotation.Inherited

/**
 * The navigation SDK has algorithms optimized for the type of data
 * being provided. The device profile selects the optimization.
 *
 * @param customConfig Json custom configuration used by the navigator
 * @param deviceType The type of device providing data to the navigator.
 */
class DeviceProfile private constructor(
    val customConfig: String,
    val deviceType: DeviceType
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder() = Builder().customConfig(customConfig).deviceType(deviceType)

    /**
     * Build a new [DeviceProfile]
     */
    class Builder {
        private var customConfig = ""
        private var deviceType = DeviceType.HANDHELD

        /**
         * Json custom configuration used by the navigator
         */
        fun customConfig(customConfig: String) = apply { this.customConfig = customConfig }

        /**
         * Change the [DeviceType]
         */
        fun deviceType(deviceType: DeviceType) = apply { this.deviceType = deviceType }

        /**
         * Build the [DeviceType]
         */
        fun build() = DeviceProfile(
            customConfig = customConfig,
            deviceType = deviceType
        )
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceProfile

        if (customConfig != other.customConfig) return false
        if (deviceType != other.deviceType) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = customConfig.hashCode()
        result = 31 * result + deviceType.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "DeviceProfile(customConfig='$customConfig', deviceType=$deviceType)"
    }
}

/**
 * The type of device providing data to the navigator.
 */
enum class DeviceType {
    /**
     * Any typical Android smart phone with GPS
     */
    HANDHELD,

    /**
     * Automobiles that provide data directly from the vehicle
     */
    AUTOMOBILE
}
