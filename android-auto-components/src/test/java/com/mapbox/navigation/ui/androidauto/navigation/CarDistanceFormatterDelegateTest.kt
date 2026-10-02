package com.mapbox.navigation.ui.androidauto.navigation

import android.content.Context
import androidx.car.app.model.Distance
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.formatter.MapboxDistanceUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class CarDistanceFormatterDelegateTest {

    private val us = Locale.US
    private val gb = Locale.UK

    @Test
    fun `US imperial uses feet below a tenth of a mile`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(160.0)

        assertEquals(500.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_FEET, distance.displayUnit)
    }

    @Test
    fun `US imperial switches to miles with one decimal from a tenth of a mile`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(161.0)

        assertEquals(0.1, distance.displayDistance, 0.001)
        assertEquals(Distance.UNIT_MILES_P1, distance.displayUnit)
    }

    @Test
    fun `imperial uses whole miles from three miles`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(4900.0)

        assertEquals(3.04, distance.displayDistance, 0.01)
        assertEquals(Distance.UNIT_MILES, distance.displayUnit)
    }

    @Test
    fun `British imperial uses yards below a tenth of a mile`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.IMPERIAL, Rounding.INCREMENT_FIFTY, gb)

        val distance = delegate.carDistance(160.0)

        assertEquals(150.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_YARDS, distance.displayUnit)
    }

    @Test
    fun `metric uses meters below a kilometer`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(999.0)

        assertEquals(950.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_METERS, distance.displayUnit)
    }

    @Test
    fun `metric uses kilometers with one decimal from one to three kilometers`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(1000.0)

        assertEquals(1.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_KILOMETERS_P1, distance.displayUnit)
    }

    @Test
    fun `metric uses whole kilometers from three kilometers`() {
        val delegate = CarDistanceFormatterDelegate(UnitType.METRIC, Rounding.INCREMENT_FIFTY, us)

        val distance = delegate.carDistance(3000.0)

        assertEquals(3.0, distance.displayDistance, 0.0)
        assertEquals(Distance.UNIT_KILOMETERS, distance.displayUnit)
    }

    @Test
    fun `car distance matches the phone value and unit at every threshold`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val distancesMeters = listOf(
            Double.NaN, -1.0, 0.0, 24.0, 25.0, 99.0, 100.0, 160.0, 161.0, 400.0, 999.0, 1000.0,
            2999.0, 3000.0, 4828.0, 4900.0, 10000.0, 100000.0,
        )
        val roundings = listOf(
            Rounding.INCREMENT_DISTANCE_DEPENDENT,
            Rounding.INCREMENT_FIVE,
            Rounding.INCREMENT_FIFTY,
        )
        val configurations = listOf(
            UnitType.METRIC to us,
            UnitType.IMPERIAL to us,
            UnitType.IMPERIAL to gb,
        )

        configurations.forEach { (unitType, locale) ->
            roundings.forEach { rounding ->
                val delegate = CarDistanceFormatterDelegate(unitType, rounding, locale)
                distancesMeters.forEach { meters ->
                    val phone = MapboxDistanceUtil.formatDistance(
                        meters,
                        rounding,
                        unitType,
                        context,
                        locale,
                    )
                    val message = "$meters m, $unitType, $locale, rounding $rounding"

                    val actual = delegate.carDistance(meters)

                    assertEquals(message, phone.distance, actual.displayDistance, 0.0)
                    assertEquals(
                        message,
                        expectedCarUnit(phone.distanceSuffix, phone.distance),
                        actual.displayUnit,
                    )
                }
            }
        }
    }

    // The phone shows one decimal for kilometers and miles below 3.
    private fun expectedCarUnit(phoneSuffix: String, distance: Double): Int = when (phoneSuffix) {
        "m" -> Distance.UNIT_METERS
        "ft" -> Distance.UNIT_FEET
        "yd" -> Distance.UNIT_YARDS
        "km" -> if (distance < 3.0) Distance.UNIT_KILOMETERS_P1 else Distance.UNIT_KILOMETERS
        "mi" -> if (distance < 3.0) Distance.UNIT_MILES_P1 else Distance.UNIT_MILES
        else -> error("Unexpected phone unit $phoneSuffix")
    }
}
