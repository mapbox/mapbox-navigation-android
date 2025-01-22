package com.mapbox.navigation.ui.maps.guidance.restarea.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapError
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MapboxRestAreaGuideMapViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render signboard null when error`() {
        val view = MapboxRestAreaGuideMapView(ctx)
        val sapaMap: Expected<RestAreaGuideMapError, RestAreaGuideMapValue> =
            ExpectedFactory.createError(
                RestAreaGuideMapError("whatever", null),
            )
        val expected = null

        view.render(sapaMap)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard when success`() {
        val view = MapboxRestAreaGuideMapView(ctx)
        val mockBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val sapaMap: Expected<RestAreaGuideMapError, RestAreaGuideMapValue> =
            ExpectedFactory.createValue(RestAreaGuideMapValue(mockBitmap))

        view.render(sapaMap)

        assertNull(Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }
}
