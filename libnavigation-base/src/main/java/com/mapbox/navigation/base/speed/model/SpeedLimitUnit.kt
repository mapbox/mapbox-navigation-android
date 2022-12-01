package com.mapbox.navigation.base.speed.model

/**
 * Defines Speed Limit Unit
 */
@Deprecated(
    message = "SpeedLimitUnit is incapable of specifying current speed unit.",
    replaceWith = ReplaceWith("SpeedUnit", imports = arrayOf("SpeedUnit"))
)
enum class SpeedLimitUnit {
    /**
     * Speed limit in kilometers per hour
     */
    KILOMETRES_PER_HOUR,

    /**
     * Speed limit in miles per hour
     */
    MILES_PER_HOUR
}
