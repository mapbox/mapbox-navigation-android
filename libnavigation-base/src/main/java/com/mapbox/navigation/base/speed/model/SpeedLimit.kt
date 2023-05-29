package com.mapbox.navigation.base.speed.model

/**
 * @deprecated Use [SpeedLimitInfo].
 *
 * Object that holds speed limit properties
 * @property speedKmph speed limit in kilometers per hour (optional)
 * @property speedLimitUnit [SpeedLimitUnit]
 * @property speedLimitSign [SpeedLimitSign]
 */
@Deprecated("Use SpeedLimitInfo", replaceWith = ReplaceWith("SpeedLimitInfo"))
data class SpeedLimit(
    val speedKmph: Int?,
    val speedLimitUnit: SpeedLimitUnit,
    val speedLimitSign: SpeedLimitSign
)
