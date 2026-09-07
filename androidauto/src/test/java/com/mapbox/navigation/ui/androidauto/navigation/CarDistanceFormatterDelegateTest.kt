package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Distance
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class CarDistanceFormatterDelegateTest {

    @Test
    fun `IMPERIAL should convert NaN distance to 0 UNIT_FEET`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY)

        val distance = mapper.carDistance(Double.NaN)

        assertEquals(0.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_FEET, distance.displayUnit)
    }

    @Test
    fun `IMPERIAL should convert small distance to UNIT_FEET`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY)

        val distance = mapper.carDistance(
            CarDistanceFormatterDelegate.smallDistanceMeters - 1,
        )

        // This is also testing that feet distance is always a whole number
        assertEquals(1300.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_FEET, distance.displayUnit)
    }

    @Test
    fun `IMPERIAL should convert medium distance to UNIT_MILES_P1`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY)

        val smallDistance = mapper.carDistance(
            CarDistanceFormatterDelegate.smallDistanceMeters + 1,
        )
        val mediumDistance = mapper.carDistance(
            CarDistanceFormatterDelegate.mediumDistanceMeters - 1,
        )

        assertEquals(0.249169771, smallDistance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_MILES_P1, smallDistance.displayUnit)
        assertEquals(6.2130, mediumDistance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_MILES_P1, mediumDistance.displayUnit)
    }

    @Test
    fun `IMPERIAL should convert large distance to UNIT_MILES`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIVE)

        val distance = mapper.carDistance(
            CarDistanceFormatterDelegate.mediumDistanceMeters + 1,
        )

        assertEquals(6.2143, distance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_MILES, distance.displayUnit)
    }

    @Test
    fun `IMPERIAL should convert small distance with rounding increment to UNIT_FEET`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIVE)

        val distance = mapper.carDistance(10.0)

        assertEquals(30.0000, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_FEET, distance.displayUnit)
    }

    @Test
    fun `METRIC should convert NaN distance to 0 UNIT_METERS`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY)

        val distance = mapper.carDistance(Double.NaN)

        assertEquals(0.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_METERS, distance.displayUnit)
    }

    @Test
    fun `METRIC should convert small distance to UNIT_METERS`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY)

        val distance = mapper.carDistance(199.0)

        assertEquals(150.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_METERS, distance.displayUnit)
    }

    @Test
    fun `METRIC should convert medium distance to UNIT_KILOMETERS_P1`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY)

        val smallDistance = mapper.carDistance(
            CarDistanceFormatterDelegate.smallDistanceMeters + 1,
        )
        val mediumDistance = mapper.carDistance(
            CarDistanceFormatterDelegate.mediumDistanceMeters - 1,
        )

        assertEquals(0.401, smallDistance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_KILOMETERS_P1, smallDistance.displayUnit)
        assertEquals(9.999, mediumDistance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_KILOMETERS_P1, mediumDistance.displayUnit)
    }

    @Test
    fun `METRIC should convert large distance to UNIT_KILOMETERS`() {
        val mapper = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY)

        val distance = mapper.carDistance(
            CarDistanceFormatterDelegate.mediumDistanceMeters + 1,
        )

        assertEquals(10.001, distance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_KILOMETERS, distance.displayUnit)
    }
}
