package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.tripdata.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.tripdata.maneuver.internal.LaneIconFactory
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.components.R
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

    private val laneIconHelper = mockk<MapboxLaneIconsApi>()
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
            .activeDirection("right")
            .drivingSide("right")
            .directions(listOf("right"))
            .build()
        every {
            laneIconHelper.getTurnLane(any())
        } returns(LaneIconFactory.createLaneIcon(R.drawable.mapbox_lane_turn, false))
        val laneIcon = laneIconHelper.getTurnLane(laneIndicator)

        view.renderLane(laneIcon, wrapper)

        assertTrue(view.isLayoutRequested)
    }

    @Test
    fun `view image not null`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .activeDirection("right")
            .drivingSide("right")
            .directions(listOf("right"))
            .build()
        every {
            laneIconHelper.getTurnLane(any())
        } returns(LaneIconFactory.createLaneIcon(R.drawable.mapbox_lane_turn, false))
        val laneIcon = laneIconHelper.getTurnLane(laneIndicator)

        view.renderLane(laneIcon, wrapper)

        assertNotNull(view.drawable)
    }

    @Test
    fun `view rotate when flipped`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .activeDirection("right")
            .drivingSide("right")
            .directions(listOf("left"))
            .build()
        every {
            laneIconHelper.getTurnLane(any())
        } returns(LaneIconFactory.createLaneIcon(R.drawable.mapbox_lane_turn, true))
        val laneIcon = laneIconHelper.getTurnLane(laneIndicator)

        view.renderLane(laneIcon, wrapper)

        assertEquals(180f, view.rotationY)
    }

    @Test
    fun `view not rotate when not flipped`() {
        val wrapper = ContextThemeWrapper(ctx, R.style.MapboxStyleTurnIconManeuver)
        val view = MapboxLaneGuidance(ctx)
        val laneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .activeDirection("right")
            .drivingSide("right")
            .directions(listOf("right"))
            .build()
        every {
            laneIconHelper.getTurnLane(any())
        } returns(LaneIconFactory.createLaneIcon(R.drawable.mapbox_lane_turn, false))
        val laneIcon = laneIconHelper.getTurnLane(laneIndicator)

        view.renderLane(laneIcon, wrapper)

        assertEquals(0f, view.rotationY)
    }
}
