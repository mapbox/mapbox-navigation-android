package com.mapbox.navigation.base.speed.model

/**
 * Object that holds speed limit properties
 * @property speedKmph speed limit in kilometers per hour (optional)
 * @property speedLimitUnit [SpeedLimitUnit]
 * @property speedLimitSign [SpeedLimitSign]
 */
data class SpeedLimit(
    val speedKmph: Int?,
    val speedLimitUnit: SpeedLimitUnit,
    val speedLimitSign: SpeedLimitSign
)
