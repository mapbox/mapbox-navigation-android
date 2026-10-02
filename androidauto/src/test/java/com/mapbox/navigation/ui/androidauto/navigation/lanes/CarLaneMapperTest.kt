package com.mapbox.navigation.ui.androidauto.navigation.lanes

import androidx.car.app.navigation.model.LaneDirection
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
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
                laneIndicator(isActive = false, directions = listOf("straight")),
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
                laneIndicator(isActive = true, directions = listOf("straight", "right")),
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
                laneIndicator(isActive = false, directions = listOf("left")),
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
                laneIndicator(isActive = true, directions = listOf("unknown indication", "right")),
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

    @Test
    fun `only the active direction of an active lane is recommended`() {
        val laneGuidance = laneGuidanceOf(
            laneIndicator(
                isActive = true,
                directions = listOf("straight", "right"),
                activeDirection = "right",
            ),
        )

        val lane = carLaneMapper.mapLanes(laneGuidance).single()

        assertEquals(LaneDirection.SHAPE_STRAIGHT, lane.directions[0].shape)
        assertFalse(lane.directions[0].isRecommended)
        assertEquals(LaneDirection.SHAPE_NORMAL_RIGHT, lane.directions[1].shape)
        assertTrue(lane.directions[1].isRecommended)
    }

    @Test
    fun `no direction of an inactive lane is recommended even with an active direction`() {
        val laneGuidance = laneGuidanceOf(
            laneIndicator(
                isActive = false,
                directions = listOf("straight", "right"),
                activeDirection = "right",
            ),
        )

        val lane = carLaneMapper.mapLanes(laneGuidance).single()

        assertFalse(lane.directions[0].isRecommended)
        assertFalse(lane.directions[1].isRecommended)
    }

    @Test
    fun `u-turn lane is drawn towards the left in right-hand traffic`() {
        val lane = carLaneMapper.mapLanes(
            laneGuidanceOf(laneIndicator(true, listOf("uturn"), drivingSide = "right")),
        ).single()

        assertEquals(LaneDirection.SHAPE_U_TURN_LEFT, lane.directions.single().shape)
    }

    @Test
    fun `u-turn lane is drawn towards the right in left-hand traffic`() {
        val lane = carLaneMapper.mapLanes(
            laneGuidanceOf(laneIndicator(true, listOf("uturn"), drivingSide = "left")),
        ).single()

        assertEquals(LaneDirection.SHAPE_U_TURN_RIGHT, lane.directions.single().shape)
    }

    @Test
    fun `every lane indication maps to its shape in both driving sides`() {
        val expectedByDrivingSide = mapOf(
            "right" to mapOf(
                "none" to LaneDirection.SHAPE_UNKNOWN,
                "straight" to LaneDirection.SHAPE_STRAIGHT,
                "left" to LaneDirection.SHAPE_NORMAL_LEFT,
                "slight left" to LaneDirection.SHAPE_SLIGHT_LEFT,
                "sharp left" to LaneDirection.SHAPE_SHARP_LEFT,
                "right" to LaneDirection.SHAPE_NORMAL_RIGHT,
                "slight right" to LaneDirection.SHAPE_SLIGHT_RIGHT,
                "sharp right" to LaneDirection.SHAPE_SHARP_RIGHT,
                "uturn" to LaneDirection.SHAPE_U_TURN_LEFT,
            ),
            "left" to mapOf(
                "none" to LaneDirection.SHAPE_UNKNOWN,
                "straight" to LaneDirection.SHAPE_STRAIGHT,
                "left" to LaneDirection.SHAPE_NORMAL_LEFT,
                "slight left" to LaneDirection.SHAPE_SLIGHT_LEFT,
                "sharp left" to LaneDirection.SHAPE_SHARP_LEFT,
                "right" to LaneDirection.SHAPE_NORMAL_RIGHT,
                "slight right" to LaneDirection.SHAPE_SLIGHT_RIGHT,
                "sharp right" to LaneDirection.SHAPE_SHARP_RIGHT,
                "uturn" to LaneDirection.SHAPE_U_TURN_RIGHT,
            ),
        )

        expectedByDrivingSide.forEach { (drivingSide, expected) ->
            expected.forEach { (indication, shape) ->
                val lane = carLaneMapper.mapLanes(
                    laneGuidanceOf(
                        laneIndicator(false, listOf(indication), drivingSide = drivingSide),
                    ),
                ).single()
                assertEquals(
                    "\"$indication\" with driving side $drivingSide",
                    shape,
                    lane.directions.single().shape,
                )
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun laneGuidanceOf(vararg indicators: LaneIndicator) = mockk<Lane> {
        every { allLanes } returns indicators.toList()
    }

    private fun laneIndicator(
        isActive: Boolean,
        directions: List<String>,
        activeDirection: String? = null,
        drivingSide: String = "right",
    ): LaneIndicator = LaneIndicator.Builder()
        .isActive(isActive)
        .directions(directions)
        .activeDirection(activeDirection)
        .drivingSide(drivingSide)
        .build()

    private fun laneWith(indication: String) = mockk<Lane> {
        every { allLanes } returns listOf(
            laneIndicator(isActive = false, directions = listOf(indication)),
        )
    }
}
