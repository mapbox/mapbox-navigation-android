package com.mapbox.androidauto.navigation.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * This class helps with unit testing.
 */
internal class CarSpeedLimitServices {
    fun speedLimitWidget(signFormat: SpeedLimitSign): SpeedLimitWidget =
        SpeedLimitWidget(signFormat)
}
