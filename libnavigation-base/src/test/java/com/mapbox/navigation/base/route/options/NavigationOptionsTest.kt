package com.mapbox.navigation.base.route.options

import android.text.SpannableString
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.options.DEFAULT_NAVIGATOR_POLLING_DELAY
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_TEN
import com.mapbox.navigation.base.typedef.TWELVE_HOURS
import com.mapbox.navigation.base.typedef.TWENTY_FOUR_HOURS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationOptionsTest {

    @Test
    fun whenBuilderBuildWithNoValuesCalledThenDefaultValuesUsed() {
        val options = NavigationOptions.Builder().build()

        assertEquals(options.roundingIncrement, ROUNDING_INCREMENT_FIFTY)
        assertEquals(options.timeFormatType, NONE_SPECIFIED)
        assertEquals(options.navigatorPollingDelay, DEFAULT_NAVIGATOR_POLLING_DELAY)
        assertEquals(options.distanceFormatter, null)
        assertEquals(options.onboardRouterConfig, null)
    }

    @Test
    fun whenBuilderBuildCalledThenProperNavigationOptionsCreated() {
        val timeFormat = TWELVE_HOURS
        val roundingIncrement = ROUNDING_INCREMENT_TEN
        val pollingDelay = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }
        val routerConfig = MapboxOnboardRouterConfig("someTilePath")

        val options = NavigationOptions.Builder()
            .timeFormatType(timeFormat)
            .roundingIncrement(roundingIncrement)
            .navigatorPollingDelay(pollingDelay)
            .distanceFormatter(distanceFormatter)
            .onboardRouterConfig(routerConfig)
            .build()

        assertEquals(options.roundingIncrement, roundingIncrement)
        assertEquals(options.timeFormatType, timeFormat)
        assertEquals(options.navigatorPollingDelay, pollingDelay)
        assertEquals(options.distanceFormatter, distanceFormatter)
        assertEquals(options.onboardRouterConfig, routerConfig)
    }

    @Test
    fun whenOptionsValuesChangedThenAllOtherValuesSaved() {
        val timeFormat = TWELVE_HOURS
        val roundingIncrement = ROUNDING_INCREMENT_TEN
        val pollingDelay = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }
        val routerConfig = MapboxOnboardRouterConfig("someTilePath")

        var options = NavigationOptions.Builder()
            .timeFormatType(timeFormat)
            .roundingIncrement(roundingIncrement)
            .navigatorPollingDelay(pollingDelay)
            .distanceFormatter(distanceFormatter)
            .onboardRouterConfig(routerConfig)
            .build()

        val builder = options.toBuilder()
        val newTimeFormat = TWENTY_FOUR_HOURS
        val newPollingDelay = 900L
        options = builder
            .timeFormatType(newTimeFormat)
            .navigatorPollingDelay(newPollingDelay)
            .build()

        assertEquals(options.roundingIncrement, roundingIncrement)
        assertEquals(options.timeFormatType, newTimeFormat)
        assertEquals(options.navigatorPollingDelay, newPollingDelay)
        assertEquals(options.distanceFormatter, distanceFormatter)
        assertEquals(options.onboardRouterConfig, routerConfig)
    }

    @Test
    fun fromBuilderToBuilderShouldBeEqual() {
        val options = NavigationOptions.Builder()
            .isFromNavigationUi(false)
            .navigatorPollingDelay(123435)
            .timeFormatType(TWENTY_FOUR_HOURS)
            .isFromNavigationUi(true)
            .build()

        val other = options.toBuilder().build()

        assertEquals(options, other)
    }

    @Test
    fun shouldBuildEqualOptions() {
        val optionsLhs = NavigationOptions.Builder()
            .isFromNavigationUi(false)
            .navigatorPollingDelay(123435)
            .timeFormatType(TWENTY_FOUR_HOURS)
            .isFromNavigationUi(true)
            .build()

        val optionsRhs = NavigationOptions.Builder()
            .isFromNavigationUi(false)
            .navigatorPollingDelay(123435)
            .timeFormatType(TWENTY_FOUR_HOURS)
            .isFromNavigationUi(true)
            .build()

        assertTrue(optionsLhs == optionsRhs)
        assertEquals(optionsLhs, optionsRhs)
    }

    @Test
    fun shouldBuildNonEqualOptions() {
        val optionsLhs = NavigationOptions.Builder()
            .navigatorPollingDelay(123435)
            .timeFormatType(TWENTY_FOUR_HOURS)
            .isFromNavigationUi(true)
            .build()

        val optionsRhs = NavigationOptions.Builder()
            .navigatorPollingDelay(123435)
            .timeFormatType(TWENTY_FOUR_HOURS)
            .isFromNavigationUi(false)
            .build()

        assertTrue(optionsLhs != optionsRhs)
        assertNotEquals(optionsLhs, optionsRhs)
    }

    @Test
    fun pepe() {
        val pepe = NavigationOptions.Builder()
            .navigatorPollingDelay(123435)
        val juan = NavigationOptions.Builder()
            .navigatorPollingDelay(123435)

        assertEquals(pepe, juan)
    }
}
