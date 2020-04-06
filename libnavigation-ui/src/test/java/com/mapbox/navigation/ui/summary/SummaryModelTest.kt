package com.mapbox.navigation.ui.summary

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.TWELVE_HOURS
import com.mapbox.navigation.core.MapboxDistanceFormatter
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter.formatTime
import com.mapbox.navigation.ui.BaseTest
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SummaryModelTest : BaseTest() {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun getDistanceRemaining() {
        val routeProgress = buildRouteProgress()
        val distanceFormatter = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()

        val result = SummaryModel(
            ctx,
            distanceFormatter,
            routeProgress,
            TWELVE_HOURS).distanceRemaining

        assertEquals("100 m", result)
    }

    @Test
    fun getTimeRemaining() {
        val routeProgress = buildRouteProgress()
        val distanceFormatter = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()

        val result = SummaryModel(
            ctx,
            distanceFormatter,
            routeProgress,
            TWELVE_HOURS).timeRemaining

        assertEquals("1 min ", result.toString())
    }

    @Test
    fun getArrivalTime() {
        val routeProgress = buildRouteProgress()
        val distanceFormatter = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
        val time = Calendar.getInstance()
        val legDurationRemaining: Double = routeProgress!!
            .currentLegProgress()!!
            .durationRemaining()
            .toDouble()
        val expectedResult =
            formatTime(
                time,
                legDurationRemaining,
                TWELVE_HOURS,
                false
            )

        val result = SummaryModel(
            ctx,
            distanceFormatter,
            routeProgress,
            TWELVE_HOURS).arrivalTime

        assertEquals(expectedResult, result)
    }

    @Throws(Exception::class)
    private fun buildRouteProgress(): RouteProgress? {
        val route = buildTestDirectionsRoute()
        return buildRouteProgress(route, 100.0, 100.0, 100.0, 0, 0)
    }
}
