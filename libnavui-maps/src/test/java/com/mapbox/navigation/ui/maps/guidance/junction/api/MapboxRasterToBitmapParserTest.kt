package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.toDataRef
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapboxRasterToBitmapParserTest {

    @Before
    fun `set up`() {
        mockkStatic(BitmapFactory::class)
    }

    @After
    fun `tear down`() {
        unmockkStatic(BitmapFactory::class)
    }

    @Test
    fun `when raster empty parse fail`() {
        val mockRaster = byteArrayOf().toDataRef()
        val expected: Expected<String, Bitmap> =
            ExpectedFactory.createError("Error parsing raster to bitmap as raster is empty")

        val actual = MapboxRasterToBitmapParser.parse(mockRaster)

        assertEquals(expected.error!!, actual.error!!)
    }

    @Test
    fun `when raster invalid parse fail`() {
        val mockRaster = byteArrayOf(34, 87, 88, 45, 22, 90, 77).toDataRef()

        every { BitmapFactory.decodeStream(any()) } returns null
        val expected = ExpectedFactory.createError<String, Bitmap>("Raster is not a valid bitmap")

        val actual = MapboxRasterToBitmapParser.parse(mockRaster)

        assertEquals(expected.error!!, actual.error!!)
    }

    @Test
    fun `when raster not empty parse success`() {
        val mockRaster = byteArrayOf(34, 87, 88, 45, 22, 90, 77).toDataRef()
        val mockBitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
        val expected: Expected<String, Bitmap> = ExpectedFactory.createValue(mockBitmap)

        val actual = MapboxRasterToBitmapParser.parse(mockRaster)

        assertEquals(expected.value!!, actual.value!!)
    }
}
