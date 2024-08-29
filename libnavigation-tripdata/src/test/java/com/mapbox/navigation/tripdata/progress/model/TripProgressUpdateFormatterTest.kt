package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripProgressUpdateFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun estimatedTimeToArrivalFormatter() {
        val formatter = object : ValueFormatter<Long, SpannableString> {
            override fun format(t: Long): SpannableString {
                return SpannableString("mapbox")
            }
        }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .estimatedTimeToArrivalFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getEstimatedTimeToArrival(5).toString(),
        )
    }

    @Test
    fun distanceRemainingFormatter() {
        val formatter = object : ValueFormatter<Double, SpannableString> {
            override fun format(t: Double): SpannableString {
                return SpannableString("mapbox")
            }
        }

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
        val formatter = object : ValueFormatter<Double, SpannableString> {
            override fun format(t: Double): SpannableString {
                return SpannableString("mapbox")
            }
        }

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
        val formatter = object : ValueFormatter<Double, SpannableString> {
            override fun format(t: Double): SpannableString {
                return SpannableString("mapbox")
            }
        }

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
        val distanceRemainingFormatter =
            object : ValueFormatter<Double, SpannableString> {
                override fun format(t: Double): SpannableString {
                    return SpannableString("distanceRemainingFormatter")
                }
            }
        val timeRemainingFormatter = object : ValueFormatter<Double, SpannableString> {
            override fun format(t: Double): SpannableString {
                return SpannableString("timeRemainingFormatter")
            }
        }
        val percentDistanceTraveledFormatter =
            object : ValueFormatter<Double, SpannableString> {
                override fun format(t: Double): SpannableString {
                    return SpannableString("percentDistanceTraveledFormatter")
                }
            }
        val estimatedTimeToArrivalFormatter = object :
            ValueFormatter<Long, SpannableString> {
            override fun format(t: Long): SpannableString {
                return SpannableString("estimatedTimeToArrivalFormatter")
            }
        }
        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .distanceRemainingFormatter(distanceRemainingFormatter)
            .timeRemainingFormatter(timeRemainingFormatter)
            .percentRouteTraveledFormatter(percentDistanceTraveledFormatter)
            .estimatedTimeToArrivalFormatter(estimatedTimeToArrivalFormatter)
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
            "estimatedTimeToArrivalFormatter",
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
}
