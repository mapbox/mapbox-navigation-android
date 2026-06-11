package com.mapbox.navigation.base.time

import com.mapbox.navigation.base.TimeFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TwentyFourHoursTimeFormatTest {

    private val chain: TimeFormatResolver = mockk()

    @Test
    fun `formats time with provided locale`() {
        // GIVEN
        val locale = Locale.US
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter = TwentyFourHoursTimeFormat(chain, locale)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.TWENTY_FOUR_HOURS, calendar)

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
    fun `delegates to chain for non-twenty-four-hour type`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter = TwentyFourHoursTimeFormat(chain, Locale.US)
        every { chain.obtainTimeFormatted(any(), any()) } returns "delegated"

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.TWELVE_HOURS, calendar)

        // THEN
        assertEquals("delegated", result)
        verify { chain.obtainTimeFormatted(TimeFormat.TWELVE_HOURS, calendar) }
    }

    @Test
    fun `uses Locale_getDefault when no locale provided`() {
        // GIVEN
        val calendar = fixedCalendar(hour = 15, minute = 45)
        val formatter = TwentyFourHoursTimeFormat(chain)

        // WHEN
        val result = formatter.obtainTimeFormatted(TimeFormat.TWENTY_FOUR_HOURS, calendar)

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
