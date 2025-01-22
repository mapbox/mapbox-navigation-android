package com.mapbox.navigation.tripdata.speedlimit

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.tripdata.speedlimit.model.PostedAndCurrentSpeedFormatter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedLimitProcessorTest {

    @Test
    fun `process posted speed and current speed update`() {
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns 12.0
                every { speedLimitInfo } returns mockk {
                    every { speed } returns 64
                    every { unit } returns SpeedUnit.MILES_PER_HOUR
                    every { sign } returns SpeedLimitSign.MUTCD
                }
            }
        }
        val formatter = PostedAndCurrentSpeedFormatter()
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }

        val result =
            SpeedLimitProcessor().process(
                SpeedLimitAction.FindPostedAndCurrentSpeed(
                    formatter,
                    locationMatcherResult,
                    distanceFormatterOptions,
                ),
            ) as SpeedLimitResult.PostedAndCurrentSpeed

        assertEquals(result.currentSpeed, 27)
        assertEquals(result.postedSpeed, 64)
        assertEquals(result.speedSignConvention, SpeedLimitSign.MUTCD)
        assertEquals(result.postedSpeedUnit, SpeedUnit.MILES_PER_HOUR)
    }

    @Test
    fun `process posted speed and current speed update different unit`() {
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns 12.0
                every { speedLimitInfo } returns mockk {
                    every { speed } returns 64
                    every { unit } returns SpeedUnit.KILOMETERS_PER_HOUR
                    every { sign } returns SpeedLimitSign.MUTCD
                }
            }
        }
        val formatter = PostedAndCurrentSpeedFormatter()
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }

        val result =
            SpeedLimitProcessor().process(
                SpeedLimitAction.FindPostedAndCurrentSpeed(
                    formatter,
                    locationMatcherResult,
                    distanceFormatterOptions,
                ),
            ) as SpeedLimitResult.PostedAndCurrentSpeed

        assertEquals(result.currentSpeed, 27)
        assertEquals(result.postedSpeed, 40)
        assertEquals(result.speedSignConvention, SpeedLimitSign.MUTCD)
        assertEquals(result.postedSpeedUnit, SpeedUnit.MILES_PER_HOUR)
    }

    @Test
    fun `process posted speed and current speed update when posted speed is null`() {
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns 12.0
                every { speedLimitInfo } returns mockk {
                    every { speed } returns null
                    every { unit } returns SpeedUnit.MILES_PER_HOUR
                    every { sign } returns SpeedLimitSign.MUTCD
                }
            }
        }
        val formatter = PostedAndCurrentSpeedFormatter()
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }

        val result =
            SpeedLimitProcessor().process(
                SpeedLimitAction.FindPostedAndCurrentSpeed(
                    formatter,
                    locationMatcherResult,
                    distanceFormatterOptions,
                ),
            ) as SpeedLimitResult.PostedAndCurrentSpeed

        assertNull(result.postedSpeed)
        assertEquals(result.currentSpeed, 27)
        assertEquals(result.speedSignConvention, SpeedLimitSign.MUTCD)
        assertEquals(result.postedSpeedUnit, SpeedUnit.MILES_PER_HOUR)
    }

    @Test
    fun `process posted speed and current speed update when current speed is null`() {
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns mockk {
                every { speed } returns null
                every { speedLimitInfo } returns mockk {
                    every { speed } returns 12
                    every { unit } returns SpeedUnit.MILES_PER_HOUR
                    every { sign } returns SpeedLimitSign.MUTCD
                }
            }
        }
        val formatter = PostedAndCurrentSpeedFormatter()
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { unitType } returns UnitType.IMPERIAL
        }

        val result =
            SpeedLimitProcessor().process(
                SpeedLimitAction.FindPostedAndCurrentSpeed(
                    formatter,
                    locationMatcherResult,
                    distanceFormatterOptions,
                ),
            ) as SpeedLimitResult.PostedAndCurrentSpeed

        assertNull(result.currentSpeed)
        assertEquals(result.postedSpeed, 12)
        assertEquals(result.speedSignConvention, SpeedLimitSign.MUTCD)
        assertEquals(result.postedSpeedUnit, SpeedUnit.MILES_PER_HOUR)
    }
}
