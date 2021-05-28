package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxSpeedLimitApiTest {

    @Test
    fun `speed limit update with valid value`() {
        val processorResult = SpeedLimitResult.SpeedLimitCalculation(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )
        val formatter = mockk<ValueFormatter<UpdateSpeedLimitValue, String>>()
        val processor = mockk<SpeedLimitProcessor> {
            every { process(any()) } returns processorResult
        }
        val api = MapboxSpeedLimitApi(formatter, processor)
        val speedLimit = SpeedLimit(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )

        val result = api.updateSpeedLimit(speedLimit)

        assertEquals(35, result.value!!.speedKPH)
        assertEquals(SpeedLimitSign.MUTCD, result.value!!.signFormat)
        assertEquals(SpeedLimitUnit.KILOMETRES_PER_HOUR, result.value!!.speedUnit)
        assertEquals(formatter, result.value!!.speedLimitFormatter)
    }

    @Test
    fun `speed limit update with null value`() {
        val processorResult = SpeedLimitResult.SpeedLimitCalculation(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )
        val formatter = mockk<ValueFormatter<UpdateSpeedLimitValue, String>>()
        val processor = mockk<SpeedLimitProcessor> {
            every { process(any()) } returns processorResult
        }
        val api = MapboxSpeedLimitApi(formatter, processor)

        val result = api.updateSpeedLimit(null)

        assertEquals(result.error!!.errorMessage, "Speed Limit data not available")
    }
}
