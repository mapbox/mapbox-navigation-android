package com.mapbox.navigation.dropin.component.marker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapMarkerFactoryTest {

    private lateinit var ctx: Context
    private lateinit var sut: MapMarkerFactory

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = MapMarkerFactory(ctx, BitmapMemoryCache(10000))
    }

    @Test
    fun `createPin - should create point annotation`() {
        val point = Point.fromLngLat(10.0, 20.0)

        val annotation = sut.createPin(point)

        assertNotNull(annotation)
        assertEquals(point, annotation.getPoint())
    }
}
