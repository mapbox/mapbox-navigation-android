package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedLimitProcessorTest {

    @Test
    fun process() {
        val speedLimit = SpeedLimit(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )

        val result =
            SpeedLimitProcessor().process(SpeedLimitAction.CalculateSpeedLimitUpdate(speedLimit))
                as SpeedLimitResult.SpeedLimitCalculation

        assertEquals(result.signFormat, SpeedLimitSign.MUTCD)
        assertEquals(result.speedUnit, SpeedLimitUnit.KILOMETRES_PER_HOUR)
        assertEquals(result.speedKPH, 35)
    }
}
