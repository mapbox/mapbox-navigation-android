package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.STRAIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.navigation.ui.maneuver.LaneIconProcessor
import com.mapbox.navigation.ui.maneuver.R
import org.junit.Assert.assertEquals
import org.junit.Test

class LaneIconsProcessorTest {

    @Test
    fun `when direction is uturn`() {
        val mockDirectionList = listOf(UTURN)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_uturn

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight`() {
        val mockDirectionList = listOf(STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_straight

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is right`() {
        val mockDirectionList = listOf(RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is sharpRight`() {
        val mockDirectionList = listOf(SHARP_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_sharp_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slightRight`() {
        val mockDirectionList = listOf(SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_slight_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left`() {
        val mockDirectionList = listOf(LEFT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is sharpLeft`() {
        val mockDirectionList = listOf(SHARP_LEFT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_sharp_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slightLeft`() {
        val mockDirectionList = listOf(SLIGHT_LEFT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = R.drawable.mapbox_ic_turn_slight_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left right then left only`() {
        val mockDirectionList = listOf(LEFT, RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = LEFT
        val expected = R.drawable.mapbox_ic_lane_left_right_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left right then right only`() {
        val mockDirectionList = listOf(LEFT, RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = RIGHT
        val expected = R.drawable.mapbox_ic_lane_left_right_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight left then left only`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = LEFT
        val expected = R.drawable.mapbox_ic_lane_left_straight_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight left then straight only`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_left_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight right then right only`() {
        val mockDirectionList = listOf(RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = RIGHT
        val expected = R.drawable.mapbox_ic_lane_right_straight_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight right then straight only`() {
        val mockDirectionList = listOf(RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_right_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left slight right then slight right only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_RIGHT
        val expected = R.drawable.mapbox_ic_lane_slight_left_slight_right_slight_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left slight right active right then slight right only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = RIGHT
        val expected = R.drawable.mapbox_ic_lane_slight_left_slight_right_slight_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left slight right then slight left only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_LEFT
        val expected = R.drawable.mapbox_ic_lane_slight_left_slight_right_slight_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left slight right active left then slight left only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = LEFT
        val expected = R.drawable.mapbox_ic_lane_slight_left_slight_right_slight_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left slight right active uturn then straight only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = UTURN
        val expected = R.drawable.mapbox_ic_turn_straight

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight slight right then straight only`() {
        val mockDirectionList = listOf(SLIGHT_RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_slight_right_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight slight right then slight right only`() {
        val mockDirectionList = listOf(SLIGHT_RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_RIGHT
        val expected = R.drawable.mapbox_ic_lane_slight_right_straight_slight_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight slight left then straight only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_slight_left_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight slight left then slight left only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_LEFT
        val expected = R.drawable.mapbox_ic_lane_slight_left_straight_slight_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight sharp right then straight only`() {
        val mockDirectionList = listOf(SHARP_RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_sharp_right_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight sharp right then sharp right only`() {
        val mockDirectionList = listOf(SHARP_RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SHARP_RIGHT
        val expected = R.drawable.mapbox_ic_lane_sharp_right_straight_sharp_right_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight sharp left then straight only`() {
        val mockDirectionList = listOf(SHARP_LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_lane_sharp_left_straight_straight_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is straight sharp left then sharp left only`() {
        val mockDirectionList = listOf(SHARP_LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SHARP_LEFT
        val expected = R.drawable.mapbox_ic_lane_sharp_left_straight_sharp_left_only

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left straight right then left only`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT, RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = LEFT
        val expected = R.drawable.mapbox_ic_turn_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left straight right then straight only`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT, RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_turn_straight

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is left straight right then right only`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT, RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = RIGHT
        val expected = R.drawable.mapbox_ic_turn_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left straight slight right then slight left only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_LEFT
        val expected = R.drawable.mapbox_ic_turn_slight_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left straight slight right active left then slight left only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = LEFT
        val expected = R.drawable.mapbox_ic_turn_slight_left

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left straight slight right then straight only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = R.drawable.mapbox_ic_turn_straight

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left straight slight right then slight right only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = SLIGHT_RIGHT
        val expected = R.drawable.mapbox_ic_turn_slight_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `when direction is slight left straight slight right active right then slight right only`() {
        val mockDirectionList = listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = RIGHT
        val expected = R.drawable.mapbox_ic_turn_slight_right

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `invalid directions when more than one`() {
        val mockDirectionList = listOf("invalid", STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = STRAIGHT
        val expected = null

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `null active directions when combination of right and straight`() {
        val mockDirectionList = listOf(RIGHT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = null

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }

    @Test
    fun `null active directions when combination of left and straight`() {
        val mockDirectionList = listOf(LEFT, STRAIGHT)
        val mockLaneIndicator = LaneIndicator
            .Builder()
            .isActive(false)
            .directions(mockDirectionList)
            .build()
        val mockActiveDirections = null
        val expected = null

        val actual = LaneIconProcessor.getDrawableFrom(mockLaneIndicator, mockActiveDirections)

        assertEquals(expected, actual)
    }
}
