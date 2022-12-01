package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult
import com.mapbox.navigation.ui.speedlimit.model.SpeedData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxSpeedInfoApiTest {

    @Test
    fun `posted and current speed update with valid value`() {
        val processorResult = SpeedLimitResult.PostedAndCurrentSpeed(
            40,
            currentSpeed = 35,
            SpeedUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
        )
        val formatter = mockk<ValueFormatter<SpeedData, Int>>()
        val processor = mockk<SpeedLimitProcessor> {
            every { process(any()) } returns processorResult
        }
        val api = MapboxSpeedInfoApi(processor)
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns 1200f
            }
        }

        val result = api.updatePostedAndCurrentSpeed(
            locationMatcherResult,
            distanceFormatterOptions,
            formatter
        )

        assertEquals(35, result.currentSpeed)
        assertEquals(40, result.postedSpeed)
        assertEquals(SpeedLimitSign.MUTCD, result.speedSignConvention)
        assertEquals(SpeedUnit.MILES_PER_HOUR, result.postedSpeedUnit)
    }

    @Test
    fun `posted and current speed do not update`() {
        val processorResult = SpeedLimitResult.PostedAndCurrentSpeed(
            null,
            currentSpeed = 35,
            SpeedUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
        )
        val formatter = mockk<ValueFormatter<SpeedData, Int>>()
        val processor = mockk<SpeedLimitProcessor> {
            every { process(any()) } returns processorResult
        }
        val api = MapboxSpeedInfoApi(processor)
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns 1200f
            }
        }

        val result = api.updatePostedAndCurrentSpeed(
            locationMatcherResult,
            distanceFormatterOptions,
            formatter
        )

        assertEquals(35, result.currentSpeed)
        assertEquals(null, result.postedSpeed)
        assertEquals(SpeedLimitSign.MUTCD, result.speedSignConvention)
        assertEquals(SpeedUnit.MILES_PER_HOUR, result.postedSpeedUnit)
    }
}
