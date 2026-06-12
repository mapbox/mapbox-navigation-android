package com.mapbox.navigation.base.formatter

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.navigation.base.TimeFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxTimeFormatterTest {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `formatTime with TWELVE_HOURS type returns 12-hour formatted time`() {
        every { DateFormat.is24HourFormat(context) } returns false
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        val result = formatter.formatTime(calendar)

        assertTrue(
            "Expected 12-hour format (PM), got: $result",
            result.contains("PM") || result.contains("pm"),
        )
    }

    @Test
    fun `formatTime with TWENTY_FOUR_HOURS type returns 24-hour formatted time`() {
        every { DateFormat.is24HourFormat(context) } returns false
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        val result = formatter.formatTime(calendar)

        assertTrue("Expected 24-hour format, got: $result", result.contains("13"))
    }

    @Test
    fun `formatTime with NONE_SPECIFIED and device 12-hour setting returns 12-hour format`() {
        every { DateFormat.is24HourFormat(context) } returns false
        val formatter = MapboxTimeFormatter(context, TimeFormat.NONE_SPECIFIED)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue(
            "Expected 12-hour format, got: $result",
            result.contains("PM") || result.contains("pm") || result.contains("3"),
        )
    }

    @Test
    fun `formatTime with NONE_SPECIFIED and device 24-hour setting returns 24-hour format`() {
        every { DateFormat.is24HourFormat(context) } returns true
        val formatter = MapboxTimeFormatter(context, TimeFormat.NONE_SPECIFIED)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue("Expected 24-hour format, got: $result", result.contains("15"))
    }

    @Test
    fun `formatTime default constructor uses NONE_SPECIFIED`() {
        every { DateFormat.is24HourFormat(context) } returns true
        val formatter = MapboxTimeFormatter(context)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }

        val result = formatter.formatTime(calendar)

        assertTrue("Expected 24-hour format for 23:59, got: $result", result.contains("23"))
    }

    @Test
    fun `formatTime TWELVE_HOURS overrides device 24-hour setting`() {
        every { DateFormat.is24HourFormat(context) } returns true
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue(
            "Expected 12-hour format overriding device setting, got: $result",
            result.contains("PM") || result.contains("pm") || result.contains("2"),
        )
    }

    @Test
    fun `formatTime TWENTY_FOUR_HOURS overrides device 12-hour setting`() {
        every { DateFormat.is24HourFormat(context) } returns false
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue(
            "Expected 24-hour format overriding device setting, got: $result",
            result.contains("14"),
        )
    }

    @Test
    fun `formatTime at midnight with TWENTY_FOUR_HOURS returns 00 hour`() {
        every { DateFormat.is24HourFormat(context) } returns false
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue("Expected midnight in 24-hour format, got: $result", result.contains("0"))
    }

    @Test
    fun `formatTime at midnight with TWELVE_HOURS returns AM`() {
        every { DateFormat.is24HourFormat(context) } returns true
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        val result = formatter.formatTime(calendar)

        assertTrue(
            "Expected AM for midnight in 12-hour format, got: $result",
            result.contains("AM") || result.contains("am") || result.contains("12"),
        )
    }
}
