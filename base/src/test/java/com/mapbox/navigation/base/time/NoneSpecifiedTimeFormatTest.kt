package com.mapbox.navigation.base.time

import com.mapbox.navigation.base.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NoneSpecifiedTimeFormatTest {

    @Test
    fun `formats in twenty-four-hour format when isDeviceTwentyFourHourFormat is true`() {
        // GIVEN
        val locale = Locale.US
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter =
            NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = true, locale = locale)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

        // THEN
        val expected = String.format(
            locale,
            TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `formats in twelve-hour format when isDeviceTwentyFourHourFormat is false`() {
        // GIVEN
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter =
            NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = false, locale = locale)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

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
    fun `uses provided locale in twenty-four-hour mode`() {
        // GIVEN - Japanese locale produces locale-specific output for %tp; 24h format uses digits
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter =
            NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = true, locale = locale)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

        // THEN
        val expected = String.format(
            locale,
            TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `uses provided locale in twelve-hour mode`() {
        // GIVEN - Japanese locale uses 午後 (gogo) instead of pm
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatterJapanese =
            NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = false, locale = locale)
        val formatterUs =
            NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = false, locale = Locale.US)

        // WHEN
        val resultJapanese =
            formatterJapanese.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)
        val resultUs = formatterUs.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

        // THEN
        assertNotEquals(resultUs, resultJapanese)
    }

    @Test
    fun `uses Locale_getDefault when no locale provided`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter = NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat = true)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

        // THEN
        val expected = String.format(
            Locale.getDefault(),
            TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    private companion object {
        fun fixedCalendar(hour: Int, minute: Int): Calendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(2024, Calendar.MARCH, 15, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
    }
}
