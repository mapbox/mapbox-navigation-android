package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.speed.model.SpeedLimitInfo
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit

object SpeedLimitInfoFactory {

    fun createSpeedLimitInfo(
        speed: Int?,
        unit: SpeedUnit,
        sign: SpeedLimitSign,
    ) = SpeedLimitInfo(speed, unit, sign)
}
