package com.mapbox.navigation.ui.base.model.tripprogress

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import io.mockk.mockk
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
        val state = mockk<TripProgressUpdate>()
        val formatter = object : ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
                return SpannableString("mapbox")
            }
        }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .estimatedTimeToArrivalFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getEstimatedTimeToArrival(state).toString()
        )
    }

    @Test
    fun distanceRemainingFormatter() {
        val state = mockk<TripProgressUpdate>()
        val formatter = object : ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
                return SpannableString("mapbox")
            }
        }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .distanceRemainingFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getDistanceRemaining(state).toString()
        )
    }

    @Test
    fun timeRemainingFormatter() {
        val state = mockk<TripProgressUpdate>()
        val formatter = object : ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
                return SpannableString("mapbox")
            }
        }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .timeRemainingFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getTimeRemaining(state).toString()
        )
    }

    @Test
    fun percentRouteTraveledFormatter() {
        val state = mockk<TripProgressUpdate>()
        val formatter = object : ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
                return SpannableString("mapbox")
            }
        }

        val progressFormatter = TripProgressUpdateFormatter.Builder(ctx)
            .percentRouteTraveledFormatter(formatter)
            .build()

        assertEquals(
            "mapbox",
            progressFormatter.getPercentRouteTraveled(state).toString()
        )
    }

    @Test
    fun toBuilder() {
        val state = mockk<TripProgressUpdate>()
        val distanceRemainingFormatter =
            object : ValueFormatter<TripProgressUpdate, SpannableString> {
                override fun format(t: TripProgressUpdate): SpannableString {
                    return SpannableString("distanceRemainingFormatter")
                }
            }
        val timeRemainingFormatter = object : ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
                return SpannableString("timeRemainingFormatter")
            }
        }
        val percentDistanceTraveledFormatter =
            object : ValueFormatter<TripProgressUpdate, SpannableString> {
                override fun format(t: TripProgressUpdate): SpannableString {
                    return SpannableString("percentDistanceTraveledFormatter")
                }
            }
        val estimatedTimeToArrivalFormatter = object :
            ValueFormatter<TripProgressUpdate, SpannableString> {
            override fun format(t: TripProgressUpdate): SpannableString {
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
            rebuiltFormatter.getDistanceRemaining(state).toString(),
            "distanceRemainingFormatter"
        )
        assertEquals(
            rebuiltFormatter.getTimeRemaining(state).toString(),
            "timeRemainingFormatter"
        )
        assertEquals(
            rebuiltFormatter.getPercentRouteTraveled(state).toString(),
            "percentDistanceTraveledFormatter"
        )
        assertEquals(
            rebuiltFormatter.getEstimatedTimeToArrival(state).toString(),
            "estimatedTimeToArrivalFormatter"
        )
    }
}
