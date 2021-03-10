package com.mapbox.navigation.ui.maps.camera.view

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.maps.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxRouteOverviewButtonTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxRouteOverviewButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.routeOverviewIcon)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxRouteOverviewButton(ctx, null)
        val expectedDrawable = view.findViewById<ImageView>(R.id.routeOverviewIcon)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxRouteOverviewButton(ctx, null, 0)
        val expectedDrawable = view.findViewById<ImageView>(R.id.routeOverviewIcon)

        assertNotNull(expectedDrawable.drawable)
    }
}
