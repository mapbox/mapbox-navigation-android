package com.mapbox.navigation.dropin.speedlimit

import com.mapbox.navigation.dropin.ClickBehavior
import com.mapbox.navigation.ui.speedlimit.internal.SpeedInfoComponentContract
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue

internal class SpeedInfoComponentContractImpl(
    private val speedInfoBehavior: ClickBehavior<SpeedInfoValue?>
) : SpeedInfoComponentContract {

    override fun onSpeedInfoClicked(speedInfo: SpeedInfoValue?) {
        speedInfoBehavior.onClicked(speedInfo)
    }
}
