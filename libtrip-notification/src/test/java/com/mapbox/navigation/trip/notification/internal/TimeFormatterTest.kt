package com.mapbox.navigation.trip.notification.internal

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.GregorianCalendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TimeFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleReturnsMinutes() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 428.0, null)

        assertEquals("7 min ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingNonDefaultLocaleReturnsMinutes() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 428.0, Locale("hu"))

        assertEquals("7 perc ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleReturnsHoursMin() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 4286.3, null)

        assertEquals("1 hr 11 min ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleReturnsDayHoursMin() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 93963.3, null)

        assertEquals("1 day 2 hr 6 min ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleReturnsMultiDayHoursMin() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 193963.3, null)

        assertEquals("2 days 5 hr 53 min ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleReturnsMultiDayHoursMinNonDefaultLocale() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 193963.3, Locale("hu"))

        assertEquals("2 nap 5 óra 53 perc ", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingHasCorrectNumberOfSpans() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 428.0, null)

        assertEquals(2, result.getSpans(0, result.count(), Object::class.java).size)
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingTypeFace() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 428.0, null)

        assertEquals(
            Typeface.BOLD,
            (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingRelativeSizeSpan() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 428.0, null)

        assertEquals(
            1.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java
                )[1] as RelativeSizeSpan
                ).sizeChange
        )
    }

    @Test
    fun formatTimeFormatNoneIsDeviceTwentyFourTrue() {
        val cal = GregorianCalendar().also {
            it.set(2001, 1, 1, 16, 31, 0)
        }

        val result = TimeFormatter.formatTime(
            cal,
            434.0,
            NONE_SPECIFIED,
            true
        )

        assertEquals("16:38", result)
    }

    @Test
    fun formatTimeFormatNoneIsDeviceTwentyFourFalse() {
        val cal = GregorianCalendar().also {
            it.set(2001, 1, 1, 16, 31, 0)
        }

        val result = TimeFormatter.formatTime(
            cal,
            434.0,
            NONE_SPECIFIED,
            false
        )

        assertEquals("4:38 pm", result)
    }

    @Test
    fun formatTimeFormatTwelveHour() {
        val cal = GregorianCalendar().also {
            it.set(2001, 1, 1, 16, 31, 0)
        }

        val result = TimeFormatter.formatTime(
            cal,
            434.0,
            TWELVE_HOURS,
            false
        )

        assertEquals("4:38 pm", result)
    }

    @Test
    fun formatTimeFormatTwentyFourHour() {
        val cal = GregorianCalendar().also {
            it.set(2001, 1, 1, 16, 31, 0)
        }

        val result = TimeFormatter.formatTime(
            cal,
            434.0,
            TWENTY_FOUR_HOURS,
            false
        )

        assertEquals("16:38", result)
    }

    @Config(qualifiers = "en")
    @Test
    fun formatTimeRemainingDefaultLocaleFiveHours() {
        val result = TimeFormatter.formatTimeRemaining(ctx, 17999.0, null)

        assertEquals("5 hr ", result.toString())
    }
}
