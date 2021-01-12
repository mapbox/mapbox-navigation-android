package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.maneuver.LaneIconHelper
import com.mapbox.navigation.ui.maneuver.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxLaneGuidanceTest {

    private val laneIconHelper = mockk<LaneIconHelper>()
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `view image layout requested`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(listOf("left"))
            .build()
        val activeDirection = "left"
        every {
            laneIconHelper.retrieveLaneToDraw(laneIndicator, activeDirection)
        } returns R.drawable.mapbox_ic_turn_left

        view.renderLane(laneIndicator, activeDirection, wrapper)

        assertTrue(view.isLayoutRequested)
    }

    @Test
    fun `view image not null`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(listOf("left"))
            .build()
        val activeDirection = "left"
        every {
            laneIconHelper.retrieveLaneToDraw(laneIndicator, activeDirection)
        } returns R.drawable.mapbox_ic_turn_left

        view.renderLane(laneIndicator, activeDirection, wrapper)

        assertNotNull(view.drawable)
    }

    @Test
    fun `view image alpha when not active`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(listOf("left"))
            .build()
        val activeDirection = "left"
        every {
            laneIconHelper.retrieveLaneToDraw(laneIndicator, activeDirection)
        } returns R.drawable.mapbox_ic_turn_left

        view.renderLane(laneIndicator, activeDirection, wrapper)
        val actualAlpha = view.alpha

        assertEquals(0.5f, actualAlpha)
    }

    @Test
    fun `view image alpha when active`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(true)
            .directions(listOf("left"))
            .build()
        val activeDirection = "left"
        every {
            laneIconHelper.retrieveLaneToDraw(laneIndicator, activeDirection)
        } returns R.drawable.mapbox_ic_turn_left

        view.renderLane(laneIndicator, activeDirection, wrapper)
        val actualAlpha = view.alpha

        assertEquals(1f, actualAlpha)
    }

    private fun getBitmap(drawable: Drawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
