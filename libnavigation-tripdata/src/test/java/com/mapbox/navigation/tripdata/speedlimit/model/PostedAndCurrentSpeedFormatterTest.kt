package com.mapbox.navigation.tripdata.speedlimit.model

import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class PostedAndCurrentSpeedFormatterTest {

    @Test
    fun `format current and posted speed based on METRIC`() {
        val formatter = PostedAndCurrentSpeedFormatter()

        val currentSpeed = formatter.format(
            SpeedData(
                12.0,
                SpeedUnit.METERS_PER_SECOND,
                UnitType.METRIC,
            ),
        )

        val postedSpeedMetric = formatter.format(
            SpeedData(
                64.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
                UnitType.METRIC,
            ),
        )

        val postedSpeedImperial = formatter.format(
            SpeedData(
                40.0,
                SpeedUnit.MILES_PER_HOUR,
                UnitType.METRIC,
            ),
        )

        assertEquals(43, currentSpeed)
        assertEquals(64, postedSpeedMetric)
        assertEquals(65, postedSpeedImperial)
    }

    @Test
    fun `format current and posted speed based on IMPERIAL`() {
        val formatter = PostedAndCurrentSpeedFormatter()

        val currentSpeed = formatter.format(
            SpeedData(
                12.0,
                SpeedUnit.METERS_PER_SECOND,
                UnitType.IMPERIAL,
            ),
        )

        val postedSpeedMetric = formatter.format(
            SpeedData(
                64.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
                UnitType.IMPERIAL,
            ),
        )

        val postedSpeedImperial = formatter.format(
            SpeedData(
                40.0,
                SpeedUnit.MILES_PER_HOUR,
                UnitType.IMPERIAL,
            ),
        )

        assertEquals(27, currentSpeed)
        assertEquals(40, postedSpeedMetric)
        assertEquals(40, postedSpeedImperial)
    }
}
