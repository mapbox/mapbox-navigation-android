package com.mapbox.navigation.base.options

/**
 * The navigation SDK has algorithms optimized for the type of data
 * being provided. The device profile selects the optimization.
 */
interface DeviceProfile {
    /**
     * Custom configuration in json format, understood by the navigator.
     */
    val customConfig: String
}

/**
 * Any typical Android smart phone with GPS.
 */
class HandheldProfile @JvmOverloads constructor(
    override val customConfig: String = ""
) : DeviceProfile

/**
 * Automobiles that provide data directly from the vehicle.
 */
class AutomobileProfile @JvmOverloads constructor(
    override val customConfig: String = ""
) : DeviceProfile
