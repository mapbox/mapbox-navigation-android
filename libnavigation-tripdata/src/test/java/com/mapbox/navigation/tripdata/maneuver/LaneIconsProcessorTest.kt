package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.STRAIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.navigation.tripdata.R
import com.mapbox.navigation.tripdata.maneuver.model.LaneIconResources
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import org.junit.Assert.assertEquals
import org.junit.Test

class LaneIconsProcessorTest {

    private val laneIconProcessor = LaneIconProcessor(LaneIconResources.Builder().build())

    @Test
    fun `when sharp left active sharp left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expected = R.drawable.mapbox_lane_straight_using_straight

        val actual = icon.drawableResId

        assertEquals(expected, actual)
    }

    @Test
    fun `when straight active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT))
            .isActive(false)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expected = R.drawable.mapbox_lane_straight

        val actual = icon.drawableResId

        assertEquals(expected, actual)
    }

    @Test
    fun `when slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp right active sharp right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when uturn active uturn`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(UTURN)
            .drivingSide(RIGHT)
            .directions(listOf(UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_uturn_using_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when uturn active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight left active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight left active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp right active sharp right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp left active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight sharp left active sharp left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight uturn active`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight uturn active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_uturn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight uturn active uturn`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(UTURN)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_straight_or_uturn_using_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left left active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left left active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right sharp right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right sharp right active sharp right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight right sharp right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left sharp left active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left sharp left active sharp left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left sharp left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left uturn active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left uturn active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_uturn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left uturn active uturn`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(UTURN)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_uturn_using_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when right sharp right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when right sharp right active sharp right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when right sharp right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(RIGHT, SHARP_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left sharp left active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left sharp left active sharp left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SHARP_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn_using_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left sharp left active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, SHARP_LEFT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_sharp_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left uturn active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left uturn active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_uturn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left uturn active uturn`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(UTURN)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable = R.drawable.mapbox_lane_turn_or_uturn_using_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_opposite_turn_or_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_opposite_turn_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_opposite_slight_turn_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_opposite_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left slight right active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left right active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight slight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight slight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight slight right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when slight left straight slight right active slight left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight right active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right right active slight right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(SLIGHT_RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_slight_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight slight right right active right`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left uturn active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left uturn active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left uturn active left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(LEFT)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when straight left uturn active uturn`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(UTURN)
            .drivingSide(RIGHT)
            .directions(listOf(STRAIGHT, LEFT, UTURN))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_uturn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp left slight right active null`() {
        val indicator = LaneIndicator.Builder()
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp left slight right active none`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection("none")
            .drivingSide(RIGHT)
            .directions(listOf(SHARP_LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when sharp left slight right active none driving side left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection("none")
            .drivingSide(LEFT)
            .directions(listOf(SHARP_LEFT, SLIGHT_RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `when left straight slight right right active straight`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(RIGHT)
            .directions(listOf(LEFT, STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `slight right right active right driving side left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(LEFT)
            .directions(listOf(SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable = R.drawable.mapbox_lane_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `straight slight right right active right driving side left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(RIGHT)
            .drivingSide(LEFT)
            .directions(listOf(STRAIGHT, SLIGHT_RIGHT, RIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = false
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_turn

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }

    @Test
    fun `left slight left straight active straight driving side left`() {
        val indicator = LaneIndicator.Builder()
            .activeDirection(STRAIGHT)
            .drivingSide(LEFT)
            .directions(listOf(LEFT, SLIGHT_LEFT, STRAIGHT))
            .isActive(true)
            .build()
        val icon = laneIconProcessor.getLaneIcon(indicator)
        val expectedFlip = true
        val expectedDrawable =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_straight

        val actualFlip = icon.shouldFlip
        val actualDrawable = icon.drawableResId

        assertEquals(expectedFlip, actualFlip)
        assertEquals(expectedDrawable, actualDrawable)
    }
}
