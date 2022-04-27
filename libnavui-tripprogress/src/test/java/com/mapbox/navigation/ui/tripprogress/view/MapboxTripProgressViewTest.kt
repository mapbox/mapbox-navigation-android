package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.tripprogress.R
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxTripProgressViewTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxTripProgressView(ctx)
        val timeRemainingView = view.findViewById<TextView>(R.id.timeRemainingText)

        assertNotNull(timeRemainingView.currentTextColor)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxTripProgressView(ctx, null)
        val timeRemainingView = view.findViewById<TextView>(R.id.timeRemainingText)
        timeRemainingView.setTextColor(R.color.mapbox_trip_progress_text_color)
        val expectedColor = R.color.mapbox_trip_progress_text_color

        assertEquals(expectedColor, timeRemainingView.currentTextColor)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxTripProgressView(ctx, null)
        val distanceRemainingView = view.findViewById<TextView>(R.id.distanceRemainingText)
        distanceRemainingView.setTextColor(R.color.mapbox_trip_progress_text_color)
        val expectedColor = R.color.mapbox_trip_progress_text_color

        assertEquals(expectedColor, distanceRemainingView.currentTextColor)
    }

    @Test
    fun updateOptions() {
        val expectedTextColor = ContextCompat.getColor(ctx, R.color.colorOnSurface)
        val expectedBackgroundColor = ContextCompat.getColor(
            ctx,
            R.color.mapbox_trip_progress_view_background_color
        )

        val view = MapboxTripProgressView(ctx, null, R.style.MapboxStyleTripProgressView)
        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(
                R.id.distanceRemainingText
            ).currentTextColor
        )
        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(
                R.id.estimatedTimeToArriveText
            ).currentTextColor
        )
        assertEquals(
            ContextCompat.getColor(ctx, R.color.colorSecondaryVariant),
            view.findViewById<TextView>(R.id.timeRemainingText).currentTextColor
        )
        assertEquals(
            expectedBackgroundColor,
            (view.background as ColorDrawable).color
        )
    }

    @Test
    fun render_Update() {
        val formatter = TripProgressUpdateFormatter.Builder(ctx)
            .estimatedTimeToArrivalFormatter(
                mockk {
                    every { format(1L) } returns SpannableString("11:59")
                }
            )
            .distanceRemainingFormatter(
                mockk {
                    every { format(2.0) } returns SpannableString("44 mi")
                }
            )
            .timeRemainingFormatter(
                mockk {
                    every { format(3.0) } returns SpannableString("5 min")
                }
            )
            .percentRouteTraveledFormatter(
                mockk {
                    every { format(4.0) } returns SpannableString("10%")
                }
            ).build()
        val state = TripProgressUpdateValue(
            1L,
            2.0,
            3.0,
            4.0,
            5.0,
            6,
            formatter
        )

        val view = MapboxTripProgressView(ctx).also {
            it.render(state)
        }

        assertEquals(
            "44 mi",
            view.findViewById<TextView>(R.id.distanceRemainingText).text.toString()
        )
        assertEquals(
            "11:59",
            view.findViewById<TextView>(
                R.id.estimatedTimeToArriveText
            ).text.toString()
        )
        assertEquals(
            "5 min",
            view.findViewById<TextView>(R.id.timeRemainingText).text.toString()
        )
    }
}
