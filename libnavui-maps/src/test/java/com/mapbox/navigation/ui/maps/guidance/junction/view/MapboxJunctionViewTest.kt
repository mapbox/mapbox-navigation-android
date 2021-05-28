package com.mapbox.navigation.ui.maps.guidance.junction.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MapboxJunctionViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render junction null when error`() {
        val view = MapboxJunctionView(ctx)
        val junction: Expected<JunctionError, JunctionValue> =
            ExpectedFactory.createError(JunctionError("whatever", null))
        val expected = null

        view.render(junction)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render junction when success`() {
        val view = MapboxJunctionView(ctx)
        val mockBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val junction: Expected<JunctionError, JunctionValue> =
            ExpectedFactory.createValue(JunctionValue(mockBitmap))

        view.render(junction)

        Assert.assertNull(Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }
}
