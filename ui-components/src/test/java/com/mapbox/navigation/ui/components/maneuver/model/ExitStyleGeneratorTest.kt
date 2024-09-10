package com.mapbox.navigation.ui.components.maneuver.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.utils.internal.extensions.drawableWithHeight
import com.mapbox.navigation.ui.utils.internal.extensions.getAsBitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExitStyleGeneratorTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when exit number and desired height then return image span`() {
        val desiredHeight = 50
        val resources = ctx.resources
        mockkStatic("com.mapbox.navigation.ui.utils.internal.extensions.BitmapEx")
        mockkStatic("com.mapbox.navigation.ui.utils.internal.extensions.TextViewEx")
        val mockExitText = "23"
        val mockDrawable = mockk<Drawable>()
        val mockBitmap = mockk<Bitmap> {
            every { drawableWithHeight(desiredHeight, resources) } returns mockDrawable
        }
        val mockTextView = mockk<TextView> {
            every { getAsBitmap() } returns mockBitmap
        }

        val spannable = ExitStyleGenerator.styleAndGetExit(
            mockExitText,
            mockTextView,
            desiredHeight,
            resources,
        )
        val imageSpan = spannable.getSpans(0, spannable.length, ImageSpan::class.java)
        val drawableFromImageSpan = imageSpan[0].drawable

        assertTrue(spannable.isNotEmpty())
        assertTrue(imageSpan.size == 1)
        assertEquals(mockDrawable, drawableFromImageSpan)
    }
}
