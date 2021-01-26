package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimit

internal sealed class SpeedLimitAction {
    data class CalculateSpeedLimitUpdate(val speedLimit: SpeedLimit) : SpeedLimitAction()
}
