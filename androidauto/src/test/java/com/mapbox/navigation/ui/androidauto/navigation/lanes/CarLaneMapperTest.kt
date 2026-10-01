package com.mapbox.navigation.ui.androidauto.navigation.lanes

import androidx.car.app.navigation.model.LaneDirection
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CarLaneMapperTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val carLaneMapper = CarLaneMapper()

    @Test
    fun `empty values should return empty list`() {
        val laneGuidance = mockk<Lane> {
            every { allLanes } returns emptyList()
        }
        val lanes = carLaneMapper.mapLanes(laneGuidance)

        assertTrue(lanes.isEmpty())
    }

    @Test
    fun `map a lane that is valid but not active`() {
        val laneGuidance = mockk<Lane> {
            every { allLanes } returns listOf(
                mockk {
                    every { isActive } returns false
                    every { directions } returns listOf("straight")
                },
            )
        }
        val lanes = carLaneMapper.mapLanes(laneGuidance)

        assertEquals(1, lanes.size)
        val lane = lanes[0]
        assertEquals(1, lane.directions.size)
        assertEquals(LaneDirection.SHAPE_STRAIGHT, lane.directions[0].shape)
        assertFalse(lane.directions[0].isRecommended)
    }

    @Test
    fun `map a lane with multiple indications`() {
        val laneGuidance = mockk<Lane> {
            every { allLanes } returns listOf(
                mockk {
                    every { isActive } returns true
                    every { directions } returns listOf("straight", "right")
                },
            )
        }
        val lanes = carLaneMapper.mapLanes(laneGuidance)

        assertEquals(1, lanes.size)
        val lane = lanes[0]
        assertEquals(2, lane.directions.size)
        assertEquals(LaneDirection.SHAPE_STRAIGHT, lane.directions[0].shape)
        assertTrue(lane.directions[0].isRecommended)
        assertEquals(LaneDirection.SHAPE_NORMAL_RIGHT, lane.directions[1].shape)
        assertTrue(lane.directions[1].isRecommended)
    }

    @Test
    fun `map a lane without valid indication`() {
        val laneGuidance = mockk<Lane> {
            every { allLanes } returns listOf(
                mockk {
                    every { isActive } returns false
                    every { directions } returns listOf("left")
                },
            )
        }
        val lanes = carLaneMapper.mapLanes(laneGuidance)

        assertEquals(1, lanes.size)
        val lane = lanes[0]
        assertEquals(1, lane.directions.size)
        assertEquals(LaneDirection.SHAPE_NORMAL_LEFT, lane.directions[0].shape)
        assertFalse(lane.directions[0].isRecommended)
    }

    @Test
    fun `map an unknown indication to an unknown shape`() {
        val laneGuidance = mockk<Lane> {
            every { allLanes } returns listOf(
                mockk {
                    every { isActive } returns true
                    every { directions } returns listOf("unknown indication", "right")
                },
            )
        }

        val lanes = carLaneMapper.mapLanes(laneGuidance)
        // Mapping the same lanes again, as on the next route progress update, doesn't throw.
        carLaneMapper.mapLanes(laneGuidance)

        val lane = lanes.single()
        assertEquals(2, lane.directions.size)
        assertEquals(LaneDirection.SHAPE_UNKNOWN, lane.directions[0].shape)
        assertEquals(LaneDirection.SHAPE_NORMAL_RIGHT, lane.directions[1].shape)
    }

    @Test
    fun `each unknown indication is logged once`() {
        mockkObject(AndroidAutoLog)
        every { AndroidAutoLog.logAndroidAutoFailure(any(), any()) } just runs
        val first = laneWith("unknown one")
        val second = laneWith("unknown two")

        carLaneMapper.mapLanes(first)
        carLaneMapper.mapLanes(first)
        carLaneMapper.mapLanes(second)

        verify(exactly = 1) {
            AndroidAutoLog.logAndroidAutoFailure(match { "unknown one" in it }, any())
        }
        verify(exactly = 1) {
            AndroidAutoLog.logAndroidAutoFailure(match { "unknown two" in it }, any())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun laneWith(indication: String) = mockk<Lane> {
        every { allLanes } returns listOf(
            mockk {
                every { isActive } returns false
                every { directions } returns listOf(indication)
            },
        )
    }
}
