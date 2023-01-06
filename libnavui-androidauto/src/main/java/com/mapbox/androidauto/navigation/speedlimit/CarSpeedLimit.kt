package com.mapbox.androidauto.navigation.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * Object with all the state needed to render the car speed limit.
 *
 * @see CarSpeedLimitRenderer
 * @see SpeedLimitWidget
 */
internal data class CarSpeedLimit(
    val isVisible: Boolean,
    val speedLimit: Int?,
    val speed: Int?,
    val signFormat: SpeedLimitSign?,
    val threshold: Int
)
