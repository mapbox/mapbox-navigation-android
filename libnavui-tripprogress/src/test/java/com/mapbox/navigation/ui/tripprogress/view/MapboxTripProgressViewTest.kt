package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressState
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdate
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MapboxTripProgressViewTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun initAttributes() {
        val expectedTextColor = ctx.getColor(R.color.mapbox_trip_progress_primary_text_color)
        val expectedDividerColor = ctx.getColor(R.color.mapbox_trip_progress_divider_color)
        val expectedBackgroundColor = ctx.getColor(R.color.mapbox_trip_progress_background_color)

        val view = MapboxTripProgressView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(
                R.id.txtMapboxTripProgressDistanceRemaining
            ).currentTextColor
        )
        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(
                R.id.txtMapboxTripProgressEstimatedTimeToArrive
            ).currentTextColor
        )
        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.txtMapboxTripProgressTimeRemaining).currentTextColor
        )
        assertEquals(
            expectedDividerColor,
            (
                view.findViewById<View>(R.id.mapboxTripProgressDivider).background as ColorDrawable
                ).color
        )
        assertEquals(
            expectedBackgroundColor,
            (view.background as ColorDrawable).color
        )
    }

    @Test
    @Config(qualifiers = "land")
    fun initAttributes_landscape() {
        val expectedDividerColor = ctx.getColor(R.color.mapbox_trip_progress_divider_color)

        val view = MapboxTripProgressView(ctx)

        assertEquals(
            expectedDividerColor,
            view.findViewById<TextView>(R.id.mapboxTripProgressDividerLeft).currentTextColor
        )
        assertEquals(
            expectedDividerColor,
            view.findViewById<TextView>(R.id.mapboxTripProgressDividerRight).currentTextColor
        )
    }

    @Test
    fun render_Update() {
        val update = TripProgressUpdate(
            1L,
            2.0,
            3.0,
            4.0,
            5.0,
            6
        )
        val formatter = TripProgressUpdateFormatter.Builder(ctx)
            .estimatedTimeToArrivalFormatter(
                mockk {
                    every { format(update) } returns SpannableString("11:59")
                }
            )
            .distanceRemainingFormatter(
                mockk {
                    every { format(update) } returns SpannableString("44 mi")
                }
            )
            .timeRemainingFormatter(
                mockk {
                    every { format(update) } returns SpannableString("5 min")
                }
            )
            .percentRouteTraveledFormatter(
                mockk {
                    every { format(update) } returns SpannableString("10%")
                }
            ).build()
        val state = TripProgressState.Update(update, formatter)

        val view = MapboxTripProgressView(ctx).also {
            it.render(state)
        }

        assertEquals(
            "44 mi",
            view.findViewById<TextView>(R.id.txtMapboxTripProgressDistanceRemaining).text.toString()
        )
        assertEquals(
            "11:59",
            view.findViewById<TextView>(
                R.id.txtMapboxTripProgressEstimatedTimeToArrive
            ).text.toString()
        )
        assertEquals(
            "5 min",
            view.findViewById<TextView>(R.id.txtMapboxTripProgressTimeRemaining).text.toString()
        )
    }
}
