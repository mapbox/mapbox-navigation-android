package com.mapbox.services.android.navigation.v5.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIFTY
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_TEN
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.util.Locale
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DistanceFormatterTest {

    @MockK
    private lateinit var context: Context
    @MockK
    private lateinit var resources: Resources
    @MockK
    private lateinit var configuration: Configuration

    companion object {
        private const val LARGE_LARGE_UNIT = 18124.65
        private const val MEDIUM_LARGE_UNIT = 9812.33
        private const val SMALL_SMALL_UNIT = 13.71
        private const val LARGE_SMALL_UNIT = 109.73
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.resources } returns resources
        every { configuration.locales } returns LocaleList.getDefault()
        every { context.getString(R.string.kilometers) } returns ("km")
        every { context.getString(R.string.meters) } returns ("m")
        every { context.getString(R.string.miles) } returns ("mi")
        every { context.getString(R.string.feet) } returns ("ft")
    }

    @Test
    fun formatDistance_noLocaleCountry() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale(Locale.ENGLISH.language),
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "11 mi"
        )
    }

    @Test
    fun formatDistance_noLocale() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale("", ""),
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "11 mi"
        )
    }

    @Test
    fun formatDistance_unitTypeDifferentFromLocale() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale.US,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "18 km"
        )
    }

    @Test
    fun formatDistance_largeMiles() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale.US,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "11 mi"
        )
    }

    @Test
    fun formatDistance_largeKilometers() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "18 km"
        )
    }

    @Test
    fun formatDistance_largeKilometerNoUnitTypeButMetricLocale() {
        assertOutput(
            LARGE_LARGE_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "18 km"
        )
    }

    @Test
    fun formatDistance_mediumMiles() {
        assertOutput(
            MEDIUM_LARGE_UNIT,
            Locale.US,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "6.1 mi"
        )
    }

    @Test
    fun formatDistance_mediumKilometers() {
        assertOutput(
            MEDIUM_LARGE_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "9,8 km"
        )
    }

    @Test
    fun formatDistance_mediumKilometersUnitTypeDifferentFromLocale() {
        assertOutput(
            MEDIUM_LARGE_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "6,1 mi"
        )
    }

    @Test
    fun formatDistance_smallFeet() {
        assertOutput(
            SMALL_SMALL_UNIT,
            Locale.US,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "50 ft"
        )
    }

    @Test
    fun formatDistance_smallFeet_roundToTen() {
        assertOutput(
            SMALL_SMALL_UNIT,
            Locale.US,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_TEN,
            "40 ft"
        )
    }

    @Test
    fun formatDistance_smallMeters() {
        assertOutput(
            SMALL_SMALL_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "50 m"
        )
    }

    @Test
    fun formatDistance_smallMeters_roundToTen() {
        assertOutput(
            SMALL_SMALL_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_TEN,
            "10 m"
        )
    }

    @Test
    fun formatDistance_largeFeet() {
        assertOutput(
            LARGE_SMALL_UNIT,
            Locale.US,
            DirectionsCriteria.IMPERIAL,
            ROUNDING_INCREMENT_FIFTY,
            "350 ft"
        )
    }

    @Test
    fun formatDistance_largeMeters() {
        assertOutput(
            LARGE_SMALL_UNIT,
            Locale.FRANCE,
            DirectionsCriteria.METRIC,
            ROUNDING_INCREMENT_FIFTY,
            "100 m"
        )
    }

    private fun assertOutput(
        distance: Double,
        locale: Locale,
        unitType: String,
        roundIncrement: Int,
        output: String
    ) {
        assertEquals(
            output,
            DistanceFormatter(context, locale.language, unitType, roundIncrement).formatDistance(
                distance
            ).toString()
        )
    }
}
