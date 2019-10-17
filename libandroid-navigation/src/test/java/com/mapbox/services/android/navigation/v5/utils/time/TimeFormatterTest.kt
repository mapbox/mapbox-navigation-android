package com.mapbox.services.android.navigation.v5.utils.time

import com.mapbox.services.android.navigation.v5.navigation.NONE_SPECIFIED
import com.mapbox.services.android.navigation.v5.navigation.TWELVE_HOURS
import com.mapbox.services.android.navigation.v5.navigation.TWENTY_FOUR_HOURS
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun checksTwelveHoursTimeFormat() {
        val time = Calendar.getInstance()
        val anyYear = 2018
        val anyMonth = 3
        val anyDay = 26
        val sixPm = 18
        val eighteenMinutes = 18
        val zeroSeconds = 0
        time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds)
        val elevenMinutes = 663.7
        val indifferentDeviceTwentyFourHourFormat = true

        val formattedTime = TimeFormatter.formatTime(
            time,
            elevenMinutes,
            TWELVE_HOURS,
            indifferentDeviceTwentyFourHourFormat
        )

        assertEquals("6:29 pm", formattedTime)
    }

    @Test
    fun checksTwentyFourHoursTimeFormat() {
        val time = Calendar.getInstance()
        val anyYear = 2018
        val anyMonth = 3
        val anyDay = 26
        val sixPm = 18
        val eighteenMinutes = 18
        val zeroSeconds = 0
        time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds)
        val elevenMinutes = 663.7
        val indifferentDeviceTwentyFourHourFormat = false

        val formattedTime = TimeFormatter.formatTime(
            time,
            elevenMinutes,
            TWENTY_FOUR_HOURS,
            indifferentDeviceTwentyFourHourFormat
        )

        assertEquals("18:29", formattedTime)
    }

    @Test
    fun checksDefaultTwelveHoursTimeFormat() {
        val time = Calendar.getInstance()
        val anyYear = 2018
        val anyMonth = 3
        val anyDay = 26
        val sixPm = 18
        val eighteenMinutes = 18
        val zeroSeconds = 0
        time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds)
        val elevenMinutes = 663.7
        val deviceTwelveHourFormat = false

        val formattedTime = TimeFormatter.formatTime(
            time,
            elevenMinutes,
            NONE_SPECIFIED,
            deviceTwelveHourFormat
        )

        assertEquals("6:29 pm", formattedTime)
    }

    @Test
    fun checksDefaultTwentyFourHoursTimeFormat() {
        val time = Calendar.getInstance()
        val anyYear = 2018
        val anyMonth = 3
        val anyDay = 26
        val sixPm = 18
        val eighteenMinutes = 18
        val zeroSeconds = 0
        time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds)
        val elevenMinutes = 663.7
        val deviceTwentyFourHourFormat = true

        val formattedTime = TimeFormatter.formatTime(
            time,
            elevenMinutes,
            NONE_SPECIFIED,
            deviceTwentyFourHourFormat
        )

        assertEquals("18:29", formattedTime)
    }
}
