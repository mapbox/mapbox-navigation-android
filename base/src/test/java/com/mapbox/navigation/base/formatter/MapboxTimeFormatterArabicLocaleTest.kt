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
class MapboxTimeFormatterArabicLocaleTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar"))
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(context) } returns false
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `GIVEN Arabic locale and afternoon time WHEN TWELVE_HOURS THEN returns Arabic PM time`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        assertEquals("١:٣٠ م", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Arabic locale and midnight WHEN TWELVE_HOURS THEN returns Arabic 12 00 AM`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWELVE_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        assertEquals("١٢:٠٠ ص", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Arabic locale and afternoon time WHEN TWENTY_FOUR_HOURS THEN returns Arabic 24-hour time`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 30)
        }

        assertEquals("١٣:٣٠", formatter.formatTime(calendar))
    }

    @Test
    fun `GIVEN Arabic locale and midnight WHEN TWENTY_FOUR_HOURS THEN returns Arabic 0 00`() {
        val formatter = MapboxTimeFormatter(context, TimeFormat.TWENTY_FOUR_HOURS)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        assertEquals("٠:٠٠", formatter.formatTime(calendar))
    }
}
