package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripProgressUpdateFormatterTest {

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun estimatedTimeOfArrivalFormatter() {
        val formatter = ValueFormatter<Calendar, SpannableString> { SpannableString("mapbox") }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .estimatedTimeOfArrivalFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getEstimatedTimeToArrival(5).toString(),
        )
    }

    @Test
    fun distanceRemainingFormatter() {
        val formatter = ValueFormatter<Double, SpannableString> { SpannableString("mapbox") }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .distanceRemainingFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getDistanceRemaining(5.0).toString(),
        )
    }

    @Test
    fun timeRemainingFormatter() {
        val formatter = ValueFormatter<Double, SpannableString> { SpannableString("mapbox") }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .timeRemainingFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getTimeRemaining(5.0).toString(),
        )
    }

    @Test
    fun percentRouteTraveledFormatter() {
        val formatter = ValueFormatter<Double, SpannableString> { SpannableString("mapbox") }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .percentRouteTraveledFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getPercentRouteTraveled(0.3).toString(),
        )
    }

    @Test
    fun toBuilder() {
        val distanceRemainingFormatter = ValueFormatter<Double, SpannableString> {
            SpannableString("distanceRemainingFormatter")
        }
        val timeRemainingFormatter = ValueFormatter<Double, SpannableString> {
            SpannableString("timeRemainingFormatter")
        }
        val percentDistanceTraveledFormatter = ValueFormatter<Double, SpannableString> {
            SpannableString("percentDistanceTraveledFormatter")
        }
        val estimatedTimeOfArrivalFormatter = ValueFormatter<Calendar, SpannableString> {
            SpannableString("estimatedTimeOfArrivalFormatter")
        }
        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .distanceRemainingFormatter(distanceRemainingFormatter)
            .timeRemainingFormatter(timeRemainingFormatter)
            .percentRouteTraveledFormatter(percentDistanceTraveledFormatter)
            .estimatedTimeOfArrivalFormatter(estimatedTimeOfArrivalFormatter)
            .build()

        val rebuiltFormatter = progressFormatter.toBuilder(ctx).build()

        assertEquals(
            rebuiltFormatter.getDistanceRemaining(0.5).toString(),
            "distanceRemainingFormatter",
        )
        assertEquals(
            rebuiltFormatter.getTimeRemaining(0.5).toString(),
            "timeRemainingFormatter",
        )
        assertEquals(
            rebuiltFormatter.getPercentRouteTraveled(0.5).toString(),
            "percentDistanceTraveledFormatter",
        )
        assertEquals(
            rebuiltFormatter.getEstimatedTimeToArrival(1L).toString(),
            "estimatedTimeOfArrivalFormatter",
        )
    }

    @Config(qualifiers = "en-rUS")
    @Test
    fun distanceRemainingFormatterDefaultRoundingIncrementImperial() {
        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx).build()

        val result = progressFormatter.getDistanceRemaining(.5)

        assertEquals("5 ft", result.toString())
    }

    @Config(qualifiers = "pt-rPT")
    @Test
    fun distanceRemainingFormatterDefaultRoundingIncrementMetric() {
        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx).build()

        val result = progressFormatter.getDistanceRemaining(1.0)

        assertEquals("2 m", result.toString())
    }

    @Test
    fun getEstimatedTimeToArrivalDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx).build()

        val result = progressFormatter.getEstimatedTimeToArrival(1734612410000)

        assertEquals("4:46 am", result.toString())
    }

    @Test
    fun getEstimatedTimeToArrivalCustomTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx).build()

        val result = progressFormatter.getEstimatedTimeToArrival(
            1734612410000,
            TimeZone.getTimeZone("America/New_York"),
        )

        assertEquals("7:46 am", result.toString())
    }
}
