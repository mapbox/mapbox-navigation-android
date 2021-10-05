package com.mapbox.navigation.core.telemetry.events

import android.graphics.Bitmap
import android.util.Base64
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

private const val initialWidth = 1080
private const val initialHeight = 1920

class FeedbackHelperTest {

    private val capture = mockk<Bitmap> {
        every { width } returns initialWidth
        every { height } returns initialHeight
    }
    private val scaledBitmap = mockk<Bitmap>()
    private val decodedBitmap = "decoded bitmap".toByteArray()
    private val encodedBitmap = "encoded bitmap"

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class, Base64::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns scaledBitmap
        every { scaledBitmap.compress(any(), any(), any()) } answers {
            thirdArg<ByteArrayOutputStream>().write(decodedBitmap)
            true
        }
        every { Base64.encodeToString(decodedBitmap, Base64.DEFAULT) } returns encodedBitmap
    }

    @Test
    fun `bitmap is compressed to requested quality`() {
        val quality = 42
        val options = BitmapEncodeOptions.Builder().compressQuality(quality).build()
        FeedbackHelper.encodeScreenshot(capture, options)

        verify { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, any()) }
    }

    @Test
    fun `bitmap is not scaled up`() {
        val options = BitmapEncodeOptions.Builder().width(1200).build()
        FeedbackHelper.encodeScreenshot(capture, options)

        verify { Bitmap.createScaledBitmap(any(), initialWidth, initialHeight, any()) }
    }

    @Test
    fun `bitmap is scaled down`() {
        val width = 540
        FeedbackHelper.encodeScreenshot(capture, BitmapEncodeOptions.Builder().width(width).build())

        verify { Bitmap.createScaledBitmap(any(), width, 960, any()) }
    }

    @After
    fun tearDown() {
        unmockkStatic(Bitmap::class, Base64::class)
    }
}
