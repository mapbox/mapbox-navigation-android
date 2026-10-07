package com.mapbox.navigation.ui.androidauto.placeslistonmap

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlaceMarkerRendererTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sut = PlaceMarkerRenderer(context)

    @Test
    fun `marker bitmap is rendered once and kept`() {
        val first = sut.renderMarker()
        val bitmap = sut.bitmap

        val second = sut.renderMarker()

        assertNotNull(bitmap)
        assertSame(bitmap, sut.bitmap)
        assertSame(first, second)
    }

    @Test
    fun `a new bitmap is used by the next marker`() {
        sut.renderMarker()
        val newBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        sut.bitmap = newBitmap

        assertSame(newBitmap, sut.renderMarker().icon!!.bitmap)
    }
}
