package com.mapbox.navigation.base.formatter

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.navigation.base.TimeFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxTimeFormatterGreekLocaleTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("el"))
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(context) } returns false
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `GIVEN Greek locale and afternoon time WHEN TWELVE_HOURS THEN returns Greek PM time`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        assertEquals("1:30 μ.μ.", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Greek locale and midnight WHEN TWELVE_HOURS THEN returns 12 00 with Greek AM marker`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        assertEquals("12:00 π.μ.", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Greek locale and afternoon time WHEN TWENTY_FOUR_HOURS THEN returns 24-hour time`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        assertEquals("13:30", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Greek locale and midnight WHEN TWENTY_FOUR_HOURS THEN returns 0 00`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        assertEquals("0:00", formatter.formatTime(calendar))
    }
}
