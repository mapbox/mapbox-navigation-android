package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRasterToBitmapParserTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when raster empty parse fail`() {
        val mockRaster = byteArrayOf()
        val expected: Expected<String, Bitmap> =
            ExpectedFactory.createError("Error parsing raster to bitmap as raster is empty")

        val actual = MapboxRasterToBitmapParser.parse(mockRaster)

        assertEquals(expected.error!!, actual.error!!)
    }

    @Test
    fun `when raster not empty parse success`() {
        val mockRaster = byteArrayOf(34, 87, 88, 45, 22, 90, 77)
        val mockBitmap = mockk<Bitmap>()
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(mockRaster, 0, mockRaster.size) } returns mockBitmap
        val expected: Expected<String, Bitmap> = ExpectedFactory.createValue(mockBitmap)

        val actual = MapboxRasterToBitmapParser.parse(mockRaster)

        assertEquals(expected.value!!, actual.value!!)
    }
}
