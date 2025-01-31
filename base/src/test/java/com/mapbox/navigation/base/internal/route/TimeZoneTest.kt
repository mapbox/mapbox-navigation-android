package com.mapbox.navigation.base.internal.route

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeZoneTest {

    @Test
    fun `convert to java time zone with with valid identifier`() {
        val timeZone = TimeZone("-08:00", "America/Los_Angeles", "PST")

        val javaTimeZone = timeZone.toJavaTimeZone()

        assertEquals("America/Los_Angeles", javaTimeZone.id)
        assertEquals(-28800000, javaTimeZone.rawOffset)
    }

    @Test
    fun `convert to java time zone with invalid identifier`() {
        val timeZone = TimeZone("-06:45", "America/Unknown", "")

        val javaTimeZone = timeZone.toJavaTimeZone()

        assertEquals("GMT-06:45", javaTimeZone.id)
        assertEquals(-24300000, javaTimeZone.rawOffset)
    }
}
