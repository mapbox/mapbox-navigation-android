package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * This class helps with unit testing.
 */
internal class CarSpeedLimitServices {
    @OptIn(MapboxExperimental::class)
    fun speedLimitWidget(signFormat: SpeedLimitSign): SpeedLimitWidget =
        SpeedLimitWidget(signFormat)
}
