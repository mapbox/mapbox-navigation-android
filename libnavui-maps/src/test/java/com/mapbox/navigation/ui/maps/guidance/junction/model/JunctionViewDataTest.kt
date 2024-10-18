@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.maps.guidance.junction.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionViewData.ResponseFormat
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JunctionViewDataTest {

    @Before
    fun setUp() {
        mockkStatic(BitmapFactory::class)
        mockkObject(SvgUtil)
    }

    @After
    fun tearDown() {
        unmockkStatic(BitmapFactory::class)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `test createFromContentType()`() {
        listOf(
            ResponseFormat.SVG to "image/svg+xml",
            ResponseFormat.SVG to "image/SVG+XML",

            ResponseFormat.PNG to "image/png",
            ResponseFormat.PNG to "image/PNG",

            ResponseFormat.UNKNOWN to "image/unknown",
            ResponseFormat.UNKNOWN to "test",
        ).forEach { (expectedFormat, contentType) ->
            val responseFormat = ResponseFormat.createFromContentType(contentType)

            assertEquals(
                "Expected $expectedFormat, but was $responseFormat",
                expectedFormat,
                responseFormat,
            )
        }
    }

    @Test
    fun `test svg junction view data`() {
        val svgData = JunctionViewData(TEST_DATA_BYTES, ResponseFormat.SVG)

        assertTrue(svgData.isSvgData)
        assertFalse(svgData.isPngData)

        assertEquals(JunctionViewData.SvgData(TEST_DATA_BYTES), svgData.getSvgData())
        assertNull(svgData.getPngData())
    }

    @Test
    fun `test png junction view data`() {
        val pngData = JunctionViewData(TEST_DATA_BYTES, ResponseFormat.PNG)

        assertTrue(pngData.isPngData)
        assertFalse(pngData.isSvgData)

        assertEquals(JunctionViewData.PngData(TEST_DATA_BYTES), pngData.getPngData())
        assertNull(pngData.getSvgData())
    }

    @Test
    fun `test junction view data with unknown format`() {
        val data = JunctionViewData(TEST_DATA_BYTES, ResponseFormat.UNKNOWN)

        assertFalse(data.isPngData)
        assertFalse(data.isSvgData)

        assertNull(data.getPngData())
        assertNull(data.getSvgData())
    }

    @Test
    fun `test PngData toBitmap with default options`() {
        val pngData = JunctionViewData.PngData(TEST_DATA_BYTES)

        val mockkBitmap = mockk<Bitmap>(relaxed = true)
        val decodeOptionsSlot = slot<BitmapFactory.Options>()
        every {
            BitmapFactory.decodeByteArray(any(), any(), any(), capture(decodeOptionsSlot))
        } returns mockkBitmap

        val bitmap = pngData.getAsBitmap()

        val expectedDefaultOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        verify(exactly = 1) {
            BitmapFactory.decodeByteArray(TEST_DATA_BYTES, 0, TEST_DATA_BYTES.size, any())
        }

        assertSame(mockkBitmap, bitmap)
        assertTrue(equals(expectedDefaultOptions, decodeOptionsSlot.captured))
    }

    @Test
    fun `test PngData toBitmap with custom options`() {
        val pngData = JunctionViewData.PngData(TEST_DATA_BYTES)

        val mockkBitmap = mockk<Bitmap>(relaxed = true)
        val decodeOptionsSlot = slot<BitmapFactory.Options>()
        every {
            BitmapFactory.decodeByteArray(any(), any(), any(), capture(decodeOptionsSlot))
        } returns mockkBitmap

        val bitmapOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = pngData.getAsBitmap(bitmapOptions)

        verify(exactly = 1) {
            BitmapFactory.decodeByteArray(TEST_DATA_BYTES, 0, TEST_DATA_BYTES.size, any())
        }

        assertSame(mockkBitmap, bitmap)
        assertTrue(equals(bitmapOptions, decodeOptionsSlot.captured))
    }

    @Test
    fun `test SvgData toBitmap with specified width and height`() {
        val svgData = JunctionViewData.SvgData(TEST_DATA_BYTES)

        val mockkBitmap = mockk<Bitmap>(relaxed = true)
        every { SvgUtil.renderAsBitmapWith(any(), any(), any()) } returns mockkBitmap

        val bitmap = svgData.getAsBitmap(width = 200, height = 100)
        assertSame(mockkBitmap, bitmap)

        verify(exactly = 1) {
            SvgUtil.renderAsBitmapWith(any(), 200, 100)
        }
    }

    @Test
    fun `test SvgData toBitmap with specified width`() {
        val svgData = JunctionViewData.SvgData(TEST_DATA_BYTES)

        val mockkBitmap = mockk<Bitmap>(relaxed = true)
        every { SvgUtil.renderAsBitmapWithWidth(any(), any()) } returns mockkBitmap

        val bitmap = svgData.getAsBitmap(width = 200)
        assertSame(mockkBitmap, bitmap)

        verify(exactly = 1) {
            SvgUtil.renderAsBitmapWithWidth(any(), 200)
        }
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            JunctionViewData::class.java,
            JunctionViewData.SvgData::class.java,
            JunctionViewData.PngData::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
            ToStringVerifier.forClass(it).verify()
        }
    }

    private companion object {
        val TEST_DATA_BYTES = byteArrayOf(1, 2, 3)

        fun equals(options1: BitmapFactory.Options, options2: BitmapFactory.Options): Boolean {
            return options1.inPreferredConfig == options2.inPreferredConfig &&
                options1.inScaled == options2.inScaled &&
                options1.inMutable == options2.inMutable &&
                options1.inPremultiplied == options2.inPremultiplied &&
                options1.inJustDecodeBounds == options2.inJustDecodeBounds &&
                options1.inBitmap == options2.inBitmap &&
                options1.inDensity == options2.inDensity &&
                options1.inPreferredColorSpace == options2.inPreferredColorSpace &&
                options1.inPreferredConfig == options2.inPreferredConfig &&
                options1.inSampleSize == options2.inSampleSize &&
                options1.inScreenDensity == options2.inScreenDensity &&
                options1.inTargetDensity == options2.inTargetDensity &&
                options1.outColorSpace == options2.outColorSpace &&
                options1.outWidth == options2.outWidth &&
                options1.outHeight == options2.outHeight &&
                options1.outConfig == options2.outConfig &&
                options1.outMimeType == options2.outMimeType
        }
    }
}
