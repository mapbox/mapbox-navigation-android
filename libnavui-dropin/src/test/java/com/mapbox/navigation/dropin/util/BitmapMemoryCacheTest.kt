package com.mapbox.navigation.dropin.util

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapMemoryCacheTest {

    lateinit var sut: BitmapMemoryCache

    @Before
    fun setUp() {
        sut = BitmapMemoryCache(10000)
    }

    @Test
    fun `should returned cached value`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8)
        sut.add("my bitmap", bitmap)

        val actual = sut.get("my bitmap")

        assertNotNull(actual)
        assertEquals(bitmap, actual)
    }
}
