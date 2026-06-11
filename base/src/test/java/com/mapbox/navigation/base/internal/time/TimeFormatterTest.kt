package com.mapbox.navigation.base.internal.time

import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.time.TwelveHoursTimeFormat
import com.mapbox.navigation.base.time.TwentyFourHoursTimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TimeFormatterTest {

    @Test
    fun `formatTime with locale uses provided locale`() {
        // GIVEN
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)

        // WHEN
        val result = TimeFormatter.formatTime(calendar, TimeFormat.TWELVE_HOURS, false, locale)

        // THEN
        val expected = String.format(
            locale,
            TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
            calendar,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `formatTime with locale uses Locale_getDefault when no locale provided`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)

        // WHEN
        val result = TimeFormatter.formatTime(calendar, TimeFormat.TWELVE_HOURS, false)

        // THEN
        val expected = String.format(
            Locale.getDefault(),
            TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
            calendar,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `formatTime with duration adds duration seconds to calendar before formatting`() {
        // GIVEN
        val locale = Locale.US
        val baseCalendar = fixedCalendar(hour = 10, minute = 0)
        val durationSeconds = 3600.0

        // WHEN
        val result = TimeFormatter.formatTime(
            baseCalendar,
            durationSeconds,
            TimeFormat.TWENTY_FOUR_HOURS,
            false,
            locale,
        )

        // THEN - result should represent 11:00 (10:00 + 1 hour), not the original 10:00
        val calendarAfterDuration = fixedCalendar(hour = 11, minute = 0)
        val expected = String.format(
            locale,
            TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
            calendarAfterDuration,
            calendarAfterDuration,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `formatTime with duration uses provided locale`() {
        // GIVEN
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val durationSeconds = 0.0

        // WHEN
        val result = TimeFormatter.formatTime(
            calendar,
            durationSeconds,
            TimeFormat.TWELVE_HOURS,
            false,
            locale,
        )

        // THEN
        val expected = String.format(
            locale,
            TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
            calendar,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `formatTime with duration uses Locale_getDefault when no locale provided`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val durationSeconds = 0.0

        // WHEN
        val result =
            TimeFormatter.formatTime(calendar, durationSeconds, TimeFormat.TWELVE_HOURS, false)

        // THEN
        val expected = String.format(
            Locale.getDefault(),
            TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
            calendar,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `locale produces distinct output for twelve-hour type - Japanese vs US`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)

        // WHEN
        val resultJapanese =
            TimeFormatter.formatTime(calendar, TimeFormat.TWELVE_HOURS, false, Locale.JAPANESE)
        val resultUs = TimeFormatter.formatTime(
            fixedCalendar(hour = 15, minute = 45),
            TimeFormat.TWELVE_HOURS,
            false,
            Locale.US,
        )

        // THEN
        assertNotEquals(resultUs, resultJapanese)
    }

    private companion object {
        fun fixedCalendar(hour: Int, minute: Int): Calendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(2024, Calendar.MARCH, 15, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
    }
}
