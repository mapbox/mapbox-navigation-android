package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.View.GONE
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.signboard.model.SignboardValue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MapboxSignboardViewTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render signboard null when expected failure`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Failure(SignboardError("whatever", null))
        val expected = null

        view.render(signboard)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard visibility hide when expected failure`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Failure(SignboardError("whatever", null))
        val expected = GONE

        view.render(signboard)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render signboard visibility hide when expected success svg invalid`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Success(SignboardValue(byteArrayOf(12, 23, 12)))
        val expected = GONE

        view.render(signboard)

        assertEquals(expected, view.visibility)
    }
}
