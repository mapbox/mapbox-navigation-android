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
class MapboxRecenterButtonTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxRecenterButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.recenterIcon)

        assertNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxRecenterButton(ctx, null)
        val expectedDrawable = view.findViewById<ImageView>(R.id.recenterIcon)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxRecenterButton(ctx, null, 0)
        val expectedDrawable = view.findViewById<ImageView>(R.id.recenterIcon)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `update style`() {
        val view = MapboxRecenterButton(ctx)
        val expectedDrawable = view.findViewById<ImageView>(R.id.recenterIcon)

        view.updateStyle(R.style.MapboxStyleRecenter)

        assertNotNull(expectedDrawable.drawable)
    }

    @Test
    fun `recenter and extend`() {
        val view = MapboxRecenterButton(ctx)
        val recenterText = view.findViewById<AppCompatTextView>(R.id.recenterText)

        view.showTextAndExtend(0)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(recenterText.text.isEmpty())
        assertEquals(recenterText.visibility, View.INVISIBLE)
    }
}
