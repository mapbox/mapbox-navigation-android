package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class EstimatedTimeToArrivalFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun format_when_timeFormat_NONE_SPECIFIED() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns false
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).toLowerCase()

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            NONE_SPECIFIED,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_TWELVE_HOURS() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns false
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).toLowerCase()

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            TWELVE_HOURS,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_TWENTY_FOUR_HOURS() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns false
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            TWENTY_FOUR_HOURS,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_NONE_SPECIFIED_is24HourFormat() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns true
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            NONE_SPECIFIED,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_TWELVE_HOURS_is24HourFormat() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns true
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).toLowerCase()

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            TWELVE_HOURS,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_NONE_SPECIFIED_is24HourFormat_is_false() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns false
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).toLowerCase()

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            NONE_SPECIFIED,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun format_when_timeFormat_TWENTY_FOUR_HOURS_is24HourFormat_is_false() {
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(ctx) } returns false
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)

        val result = EstimatedTimeToArrivalFormatter(
            ctx,
            TWENTY_FOUR_HOURS,
        ).format(now.time)

        assertEquals(expected, result.toString())
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun usesApplicationContext() {
        val inputContext = mockk<Context> {
            every { applicationContext } returns ctx
        }

        EstimatedTimeToArrivalFormatter(
            inputContext,
            NONE_SPECIFIED,
        )

        verify { inputContext.applicationContext }
    }
}
