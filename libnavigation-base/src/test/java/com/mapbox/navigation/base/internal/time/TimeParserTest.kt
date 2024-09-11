package com.mapbox.navigation.base.internal.time

import com.mapbox.navigation.testing.utcToLocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Month

class TimeParserTest {
    @Test
    fun `parse UTC date`() {
        val date = parseISO8601DateToLocalTimeOrNull("2022-05-22T12:11:34Z")
            ?: error("result is null")
        val expectedDate = utcToLocalTime(
            2022,
            Month.MAY,
            22,
            12,
            11,
            34,
        )
        assertEquals(expectedDate, date)
    }

    @Test
    fun `parse UTC date with time zone offset`() {
        val date = parseISO8601DateToLocalTimeOrNull("2022-02-15T12:00:00-01")
            ?: error("result is null")
        val expectedDate = utcToLocalTime(
            2022,
            Month.FEBRUARY,
            15,
            13,
            0,
            0,
        )
        assertEquals(expectedDate.time, date.time)
    }

    @Test
    fun `parse wrong UTC`() {
        val date = parseISO8601DateToLocalTimeOrNull("wrong")
        assertNull(date)
    }

    @Test
    fun `parse null`() {
        val date = parseISO8601DateToLocalTimeOrNull(null)
        assertNull(date)
    }
}
