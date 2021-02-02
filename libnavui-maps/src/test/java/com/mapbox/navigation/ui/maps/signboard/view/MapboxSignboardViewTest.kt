package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
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
    fun `render signboard null when state empty`() {
        val view = MapboxSignboardView(ctx)
        val signboardState = SignboardState.Signboard.Empty
        val expected = null

        view.render(signboardState)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard null when state error`() {
        val view = MapboxSignboardView(ctx)
        val signboardState = SignboardState.Signboard.Error("")
        val expected = null

        view.render(signboardState)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard visibility hide`() {
        val view = MapboxSignboardView(ctx)
        val signboardState = SignboardState.Hide
        val expected = GONE

        view.render(signboardState)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render signboard visibility show`() {
        val view = MapboxSignboardView(ctx)
        val signboardState = SignboardState.Show
        val expected = VISIBLE

        view.render(signboardState)

        assertEquals(expected, view.visibility)
    }
}
