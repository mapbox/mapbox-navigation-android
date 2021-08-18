package com.mapbox.navigation.ui.maps.camera.view

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.maps.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.PAUSED)
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

    @Test
    fun `update style`() {
        val view = MapboxRouteOverviewButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.routeOverviewIcon)

        view.updateStyle(R.style.MapboxStyleRouteOverview)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `overview and extend`() {
        val view = MapboxRouteOverviewButton(ctx)
        val routeOverviewText = view.findViewById<AppCompatTextView>(R.id.routeOverviewText)

        view.showTextAndExtend(2000L)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(routeOverviewText.text.isEmpty())
        assertEquals(routeOverviewText.visibility, View.INVISIBLE)
    }
}
