package com.mapbox.navigation.ui.speedlimit.model

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Represents a speed limit update to be rendered.
 * @property speedKPH Int speed in kilometers per hour
 * @property speedUnit SpeedLimitUnit speed limit unit.
 * @property signFormat SpeedLimitSign speed limit sign
 * @property speedLimitFormatter ValueFormatter<UpdateSpeedLimitValue, String>
 * @constructor
 */
class UpdateSpeedLimitValue internal constructor(
    val speedKPH: Int,
    val speedUnit: SpeedLimitUnit,
    val signFormat: SpeedLimitSign,
    val speedLimitFormatter: ValueFormatter<UpdateSpeedLimitValue, String>
)
