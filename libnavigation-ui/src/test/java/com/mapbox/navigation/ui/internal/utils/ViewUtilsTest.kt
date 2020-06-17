package com.mapbox.navigation.ui.internal.utils

import android.graphics.Bitmap
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test

private const val LOW_DENSITY_BITMAP_WIDTH = 160
private const val HIGH_DENSITY_BITMAP_WIDTH = 1080
private const val BITMAP_HEIGHT = 240

class ViewUtilsTest {

    @Test
    fun encodeViewWithDefaultCompressQuality() {
        val (aBitmap, compressQualitySlot) = mockBitmapForCheckingCompressQuality()

        ViewUtils.encodeView(aBitmap)

        assertEquals(DEFAULT_BITMAP_ENCODE_COMPRESS_QUALITY, compressQualitySlot.captured)
    }

    @Test
    fun encodeViewWithDefaultWidthOfLowDensityBitmap() {
        val (aLowDensityBitmap, widthSlot) = mockBitmapForCheckingWidth(LOW_DENSITY_BITMAP_WIDTH)

        ViewUtils.encodeView(aLowDensityBitmap)

        assertEquals(LOW_DENSITY_BITMAP_WIDTH, widthSlot.captured)
    }

    @Test
    fun encodeViewWithDefaultWidthOfHighDensityBitmap() {
        val (aHighDensityBitmap, widthSlot) = mockBitmapForCheckingWidth()

        ViewUtils.encodeView(aHighDensityBitmap)

        assertEquals(DEFAULT_BITMAP_ENCODE_WIDTH, widthSlot.captured)
    }

    @Test
    fun encodeViewWithCompressQualityZero() {
        val (aBitmap, compressQualitySlot) = mockBitmapForCheckingCompressQuality()
        val compressQualityZero = 0
        val options = BitmapEncodeOptions.Builder().compressQuality(compressQualityZero).build()

        ViewUtils.encodeView(aBitmap, options)

        assertEquals(compressQualityZero, compressQualitySlot.captured)
    }

    @Test
    fun encodeViewWithNormalCompressQuality() {
        val (aBitmap, compressQualitySlot) = mockBitmapForCheckingCompressQuality()
        val normalCompressQuality = 50
        val options = BitmapEncodeOptions.Builder().compressQuality(normalCompressQuality).build()

        ViewUtils.encodeView(aBitmap, options)

        assertEquals(normalCompressQuality, compressQualitySlot.captured)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeViewWithNegativeCompressQuality() {
        val aBitmap: Bitmap = mockk()
        val options = BitmapEncodeOptions.Builder().compressQuality(-10).build()

        ViewUtils.encodeView(aBitmap, options)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeViewWithBigCompressQuality() {
        val aBitmap: Bitmap = mockk()
        val options = BitmapEncodeOptions.Builder().compressQuality(200).build()

        ViewUtils.encodeView(aBitmap, options)
    }

    @Test
    fun encodeViewWithSmallWidth() {
        val (aBitmap, widthSlot) = mockBitmapForCheckingWidth()
        val smallWidth = 10
        val options = BitmapEncodeOptions.Builder().width(smallWidth).build()

        ViewUtils.encodeView(aBitmap, options)

        assertEquals(smallWidth, widthSlot.captured)
    }

    @Test
    fun encodeViewWithBigWidth() {
        val (aBitmap, widthSlot) = mockBitmapForCheckingWidth()
        val bigWidth = HIGH_DENSITY_BITMAP_WIDTH + 1
        val options = BitmapEncodeOptions.Builder().width(bigWidth).build()

        ViewUtils.encodeView(aBitmap, options)

        assertEquals(HIGH_DENSITY_BITMAP_WIDTH, widthSlot.captured)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeViewWithWidthZero() {
        val aBitmap: Bitmap = mockk()
        val options = BitmapEncodeOptions.Builder().width(0).build()

        ViewUtils.encodeView(aBitmap, options)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeViewWithNegativeWidth() {
        val aBitmap: Bitmap = mockk()
        val options = BitmapEncodeOptions.Builder().width(-1).build()

        ViewUtils.encodeView(aBitmap, options)
    }

    private fun mockBitmapForCheckingWidth(width: Int = HIGH_DENSITY_BITMAP_WIDTH): BitmapWithSlot {
        val aBitmap: Bitmap = mockk()
        every { aBitmap.width }.returns(width)
        every { aBitmap.height }.returns(BITMAP_HEIGHT)
        val scaledBitmap: Bitmap = mockk()
        every { scaledBitmap.compress(any(), any(), any()) }.returns(true)
        mockkStatic(Bitmap::class)
        val widthSlot = slot<Int>()
        every {
            Bitmap.createScaledBitmap(
                any(),
                capture(widthSlot),
                any(),
                any()
            )
        } answers { scaledBitmap }

        return BitmapWithSlot(aBitmap = aBitmap, slot = widthSlot)
    }

    private fun mockBitmapForCheckingCompressQuality(): BitmapWithSlot {
        val aBitmap: Bitmap = mockk()
        every { aBitmap.width }.returns(HIGH_DENSITY_BITMAP_WIDTH)
        every { aBitmap.height }.returns(BITMAP_HEIGHT)
        val scaledBitmap: Bitmap = mockk()
        val compressQualitySlot = slot<Int>()
        every { scaledBitmap.compress(any(), capture(compressQualitySlot), any()) }.returns(true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers { scaledBitmap }

        return BitmapWithSlot(aBitmap = aBitmap, slot = compressQualitySlot)
    }

    private data class BitmapWithSlot(val aBitmap: Bitmap, val slot: CapturingSlot<Int>)
}
