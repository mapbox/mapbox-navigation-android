package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class TimeRemainingFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun usesApplicationContext() {
        val inputContext = mockk<Context> {
            every { applicationContext } returns ctx
        }

        TimeRemainingFormatter(inputContext)

        verify { inputContext.applicationContext }
    }

    @Config(qualifiers = "en")
    @Test
    fun format() {
        val result = TimeRemainingFormatter(ctx).format(1800.0)

        assertEquals("30 min", result.toString())
        assertEquals(6, result.count())
        assertEquals(
            Typeface.BOLD,
            (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style,
        )
        assertEquals(
            1.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[1] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            2.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            0,
            result.getSpanStart(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            2,
            result.getSpanEnd(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            result.getSpanFlags(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun format_lessThanOneMinute() {
        val result = TimeRemainingFormatter(ctx).format(15.0)

        assertEquals("< 1 min", result.toString())
        assertEquals(7, result.count())
        assertEquals(
            1.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[0] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            Typeface.BOLD,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[1] as StyleSpan
                ).style,
        )
        assertEquals(
            1.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            3,
            result.getSpanStart(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            7,
            result.getSpanEnd(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            result.getSpanFlags(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[2] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            2.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[3] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            0,
            result.getSpanStart(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[3] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            3,
            result.getSpanEnd(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[3] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            result.getSpanFlags(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[3] as RelativeSizeSpan,
            ),
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun format_moreThanOneHour() {
        val result = TimeRemainingFormatter(ctx).format(5460.0)

        assertEquals("1 hr 31 min", result.toString())
        assertEquals(11, result.count())
        assertEquals(
            Typeface.BOLD,
            (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style,
        )
        assertEquals(
            2.0f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[5] as RelativeSizeSpan
                ).sizeChange,
        )
        assertEquals(
            5,
            result.getSpanStart(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[5] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            7,
            result.getSpanEnd(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[5] as RelativeSizeSpan,
            ),
        )
        assertEquals(
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            result.getSpanFlags(
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[5] as RelativeSizeSpan,
            ),
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun format_withLocale() {
        val result = TimeRemainingFormatter(
            ctx,
            Locale("hu"),
        ).format(1800.0)

        assertEquals("30 perc", result.toString())
    }
}
