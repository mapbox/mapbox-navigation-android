package com.mapbox.navigation.base.time

import com.mapbox.navigation.base.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TimeFormattingChainTest {

    private val chain = TimeFormattingChain()

    @Test
    fun `setup returns chain that uses provided locale for twelve-hour type`() {
        // GIVEN
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolver = chain.setup(isDeviceTwentyFourHourFormat = false, locale = locale)

        // WHEN
        val result = resolver.obtainTimeFormatted(TimeFormat.TWELVE_HOURS, calendar)

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
    fun `setup returns chain that uses provided locale for twenty-four-hour type`() {
        // GIVEN
        val locale = Locale.US
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolver = chain.setup(isDeviceTwentyFourHourFormat = false, locale = locale)

        // WHEN
        val result = resolver.obtainTimeFormatted(TimeFormat.TWENTY_FOUR_HOURS, calendar)

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
    fun `setup returns chain that uses provided locale for none-specified type when device is in twenty-four-hour format`() {
        // GIVEN
        val locale = Locale.US
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolver = chain.setup(isDeviceTwentyFourHourFormat = true, locale = locale)

        // WHEN
        val result = resolver.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

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
    fun `setup returns chain that uses provided locale for none-specified type when device is in twelve-hour format`() {
        // GIVEN
        val locale = Locale.JAPANESE
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolver = chain.setup(isDeviceTwentyFourHourFormat = false, locale = locale)

        // WHEN
        val result = resolver.obtainTimeFormatted(TimeFormat.NONE_SPECIFIED, calendar)

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
    fun `setup uses Locale_getDefault when no locale provided`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolver = chain.setup(isDeviceTwentyFourHourFormat = true)

        // WHEN
        val result = resolver.obtainTimeFormatted(TimeFormat.TWENTY_FOUR_HOURS, calendar)

        // THEN
        val expected = String.format(
            Locale.getDefault(),
            TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
            calendar,
            calendar,
        )
        assertEquals(expected, result)
    }

    @Test
    fun `locale propagates through chain - Japanese produces locale-specific output for twelve-hour type`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val resolverJapanese =
            chain.setup(isDeviceTwentyFourHourFormat = false, locale = Locale.JAPANESE)
        val resolverUs = chain.setup(isDeviceTwentyFourHourFormat = false, locale = Locale.US)

        // WHEN
        val resultJapanese = resolverJapanese.obtainTimeFormatted(TimeFormat.TWELVE_HOURS, calendar)
        val resultUs = resolverUs.obtainTimeFormatted(TimeFormat.TWELVE_HOURS, calendar)

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
