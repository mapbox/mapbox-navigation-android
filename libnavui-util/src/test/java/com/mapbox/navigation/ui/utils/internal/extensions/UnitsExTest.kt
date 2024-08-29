package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnitsExTest {

    @Test
    fun dipToPixel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = 1f
        val expected = 10f
        mockkStatic(TypedValue::class)
        every {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                input,
                context.resources.displayMetrics,
            )
        } returns expected

        val actual = context.dipToPixel(input)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun spToPixel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = 1f
        val expected = 20f
        mockkStatic(TypedValue::class)
        every {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                input,
                context.resources.displayMetrics,
            )
        } returns expected

        val actual = context.spToPixel(input)

        Assert.assertEquals(expected, actual)
    }
}
