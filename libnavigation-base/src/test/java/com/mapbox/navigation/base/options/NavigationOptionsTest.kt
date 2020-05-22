package com.mapbox.navigation.base.options

import android.text.SpannableString
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.formatter.DistanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationOptionsTest {

    @Test
    fun whenBuilderBuildWithNoValuesCalledThenDefaultValuesUsed() {
        val options = NavigationOptions.Builder().build()

        assertEquals(options.timeFormatType, NONE_SPECIFIED)
        assertEquals(options.navigatorPredictionMillis, DEFAULT_NAVIGATOR_PREDICTION_MILLIS)
        assertEquals(options.distanceFormatter, null)
        assertEquals(options.onboardRouterOptions, null)
    }

    @Test
    fun whenBuilderBuildCalledThenProperNavigationOptionsCreated() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        val options = NavigationOptions.Builder()
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
            .build()

        assertEquals(options.timeFormatType, timeFormat)
        assertEquals(options.navigatorPredictionMillis, navigatorPredictionMillis)
        assertEquals(options.distanceFormatter, distanceFormatter)
    }

    @Test
    fun whenOptionsValuesChangedThenAllOtherValuesSaved() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        var options = NavigationOptions.Builder()
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
            .build()

        val builder = options.toBuilder()
        val newTimeFormat = TWENTY_FOUR_HOURS
        val newNavigatorPredictionMillis = 900L
        options = builder
            .timeFormatType(newTimeFormat)
            .navigatorPredictionMillis(newNavigatorPredictionMillis)
            .build()

        assertEquals(options.timeFormatType, newTimeFormat)
        assertEquals(options.navigatorPredictionMillis, newNavigatorPredictionMillis)
        assertEquals(options.distanceFormatter, distanceFormatter)
    }
}
