package com.mapbox.navigation.ui.maps.guidance.junction.view

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MapboxJunctionViewTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render junction null when expected failure`() {
        val view = MapboxJunctionView(ctx)
        val junction = Expected.Failure(JunctionError("whatever", null))
        val expected = null

        view.render(junction)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render junction visibility hide when expected failure`() {
        val view = MapboxJunctionView(ctx)
        val junction = Expected.Failure(JunctionError("whatever", null))
        val expected = GONE

        view.render(junction)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render junction visibility hide when expected success empty data`() {
        val view = MapboxJunctionView(ctx)
        val junction = Expected.Success(JunctionValue(byteArrayOf()))
        val expected = GONE

        view.render(junction)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render junction visibility show when expected success`() {
        val view = MapboxJunctionView(ctx)
        val junction = Expected.Success(JunctionValue(byteArrayOf(12, 23, 12)))
        val expected = VISIBLE

        view.render(junction)

        assertNotNull(view.drawable)
        assertEquals(expected, view.visibility)
    }
}
