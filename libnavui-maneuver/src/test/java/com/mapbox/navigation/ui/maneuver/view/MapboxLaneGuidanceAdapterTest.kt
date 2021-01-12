package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxLaneGuidanceAdapterTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `add lanes empty query item count`() {
        val laneAdapter = MapboxLaneGuidanceAdapter(ctx)

        laneAdapter.addLanes(listOf(), null)

        assertEquals(0, laneAdapter.itemCount)
    }

    @Test
    fun `add lanes query item count`() {
        val laneIndicatorList = listOf<LaneIndicator>(
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("left"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(true)
                .directions(listOf("straight"))
                .build()
        )
        val laneAdapter = MapboxLaneGuidanceAdapter(ctx)
        val expected = laneIndicatorList.size

        laneAdapter.addLanes(laneIndicatorList, null)
        val actual = laneAdapter.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `remove lanes size of lanes`() {
        val laneIndicatorList = listOf(
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("left"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(true)
                .directions(listOf("straight"))
                .build()
        )
        val laneAdapter = MapboxLaneGuidanceAdapter(ctx)
        val expected = 0

        laneAdapter.addLanes(laneIndicatorList, null)
        laneAdapter.removeLanes()
        val actual = laneAdapter.itemCount

        assertEquals(expected, actual)
    }
}
