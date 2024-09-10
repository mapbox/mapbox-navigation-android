package com.mapbox.navigation.ui.components.maps.camera.view

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.components.test.R
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

private const val customText = "custom text"

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
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxRouteOverviewButton(ctx, null)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxRouteOverviewButton(ctx, null, 0)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `update style`() {
        val view = MapboxRouteOverviewButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.buttonIcon)

        view.updateStyle(R.style.MapboxStyleRouteOverview)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `overview and extend`() {
        val view = MapboxRouteOverviewButton(ctx)
        val routeOverviewText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        view.showTextAndExtend(2000L)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(routeOverviewText.text.isEmpty())
        assertEquals(routeOverviewText.visibility, View.INVISIBLE)
    }

    @Test
    fun `overview and extend with text`() {
        val view = MapboxRouteOverviewButton(ctx)
        val routeOverviewText = view.findViewById<AppCompatTextView>(R.id.buttonText)

        view.showTextAndExtend(2000L, customText)

        assertEquals(routeOverviewText.text, customText)
        assertEquals(routeOverviewText.visibility, View.VISIBLE)
    }
}
