package com.mapbox.navigation.ui.maps.guidance.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MapboxSignboardViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render signboard null when error`() {
        val view = MapboxSignboardView(ctx)
        val signboard: Expected<SignboardError, SignboardValue> =
            ExpectedFactory.createError(SignboardError("whatever", null))
        val expected = null

        view.render(signboard)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard when success`() {
        val view = MapboxSignboardView(ctx)
        val mockBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val signboard: Expected<SignboardError, SignboardValue> =
            ExpectedFactory.createValue(SignboardValue(mockBitmap))

        view.render(signboard)

        assertNull(Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }
}
