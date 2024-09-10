package com.mapbox.navigation.tripdata.speedlimit.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.tripdata.speedlimit.model.SpeedInfoValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object SpeedInfoValueFactory {

    @JvmStatic
    fun createSpeedInfoValue(
        postedSpeed: Int?,
        currentSpeed: Int,
        postedSpeedUnit: SpeedUnit,
        speedSignConvention: SpeedLimitSign?,
    ) = SpeedInfoValue(
        postedSpeed = postedSpeed,
        currentSpeed = currentSpeed,
        postedSpeedUnit = postedSpeedUnit,
        speedSignConvention = speedSignConvention,
    )
}
