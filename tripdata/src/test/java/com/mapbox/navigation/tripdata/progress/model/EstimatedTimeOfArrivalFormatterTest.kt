package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.SpannableString
import android.text.format.DateFormat
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class EstimatedTimeOfArrivalFormatterTest {

    private val ctx = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkConstructor(SpannableString::class)
    }

    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun format_when_timeFormat_TWELVE_HOURS_is24HourFormat_is_false() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns false
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).lowercase()
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, TWELVE_HOURS).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun format_when_timeFormat_TWENTY_FOUR_HOURS_is24HourFormat() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns true
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, TWENTY_FOUR_HOURS).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun format_when_timeFormat_NONE_SPECIFIED_is24HourFormat() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns true
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, NONE_SPECIFIED).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun format_when_timeFormat_TWELVE_HOURS_is24HourFormat() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns true
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).lowercase()
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, TWELVE_HOURS).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun format_when_timeFormat_NONE_SPECIFIED_is24HourFormat_is_false() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns false
        val now = Date()
        val expected = SimpleDateFormat("h:mm a").format(now).lowercase()
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, NONE_SPECIFIED).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun format_when_timeFormat_TWENTY_FOUR_HOURS_is24HourFormat_is_false() {
        every { DateFormat.is24HourFormat(ctx.applicationContext) } returns false
        val now = Date()
        val expected = SimpleDateFormat("H:mm").format(now)
        every { constructedWith<SpannableString>(EqMatcher(expected)).toString() } returns expected

        val calendar = Calendar.getInstance().apply { time = now }
        val result = EstimatedTimeOfArrivalFormatter(ctx, TWENTY_FOUR_HOURS).format(calendar)

        assertEquals(expected, result.toString())
    }

    @Test
    fun usesApplicationContext() {
        EstimatedTimeOfArrivalFormatter(ctx, NONE_SPECIFIED)

        verify { ctx.applicationContext }
    }
}
