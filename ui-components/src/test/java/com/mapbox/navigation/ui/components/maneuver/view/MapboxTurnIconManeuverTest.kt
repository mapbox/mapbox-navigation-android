package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuverFactory
import com.mapbox.navigation.ui.components.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxTurnIconManeuverTest {

    private val turnIconHelper = mockk<TurnIconHelper>()
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when primary driving side and degrees not null then rotationY should not be zero`() {
        val maneuver = PrimaryManeuverFactory.buildPrimaryManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.ROUNDABOUT,
            degrees = 23.0,
            modifier = ManeuverModifier.LEFT,
            drivingSide = "left",
            componentList = listOf(),
        )
        val turnIcon = getRoundaboutWithPrimary(maneuver)
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 180f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type,
                maneuver.degrees?.toFloat(),
                maneuver.drivingSide,
                maneuver.modifier,
            )
        } returns turnIcon

        view.renderPrimaryTurnIcon(maneuver)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `when primary driving side and degrees null then rotationY should be zero`() {
        val maneuver = PrimaryManeuverFactory.buildPrimaryManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.LEFT,
            drivingSide = null,
            componentList = listOf(),
        )
        val turnIcon = getTurnIcon()
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 0f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type,
                maneuver.degrees?.toFloat(),
                maneuver.drivingSide,
                maneuver.modifier,
            )
        } returns turnIcon

        view.renderPrimaryTurnIcon(maneuver)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `when sub driving side and degrees not null then rotationY should not be zero`() {
        val maneuver = SubManeuverFactory.buildSubManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.ROUNDABOUT,
            degrees = 23.0,
            modifier = ManeuverModifier.LEFT,
            drivingSide = "left",
            componentList = listOf(),
        )
        val turnIcon = getRoundaboutWithSub(maneuver)
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 180f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type,
                maneuver.degrees?.toFloat(),
                maneuver.drivingSide,
                maneuver.modifier,
            )
        } returns turnIcon

        view.renderSubTurnIcon(maneuver)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `when sub driving side and degrees null then rotationY should be zero`() {
        val maneuver = SubManeuverFactory.buildSubManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.LEFT,
            drivingSide = null,
            componentList = listOf(),
        )
        val turnIcon = getTurnIcon()
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 0f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type,
                maneuver.degrees?.toFloat(),
                maneuver.drivingSide,
                maneuver.modifier,
            )
        } returns turnIcon

        view.renderSubTurnIcon(maneuver)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `when maneuver roundabout followed by normal turn then rotation angle is zero`() {
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 0f
        val roundaboutManeuver = PrimaryManeuverFactory.buildPrimaryManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.ROUNDABOUT,
            degrees = 23.0,
            modifier = ManeuverModifier.LEFT,
            drivingSide = "left",
            componentList = listOf(),
        )
        val turnManeuver = PrimaryManeuverFactory.buildPrimaryManeuver(
            id = UUID.randomUUID().toString(),
            text = "I-880",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.LEFT,
            drivingSide = null,
            componentList = listOf(),
        )
        val roundaboutTurnIcon = getRoundaboutWithPrimary(roundaboutManeuver)
        val turnIcon = getTurnIcon()
        every {
            turnIconHelper.retrieveTurnIcon(
                roundaboutManeuver.type,
                roundaboutManeuver.degrees?.toFloat(),
                roundaboutManeuver.drivingSide,
                roundaboutManeuver.modifier,
            )
        } returns roundaboutTurnIcon
        every {
            turnIconHelper.retrieveTurnIcon(
                turnManeuver.type,
                turnManeuver.degrees?.toFloat(),
                turnManeuver.drivingSide,
                turnManeuver.modifier,
            )
        } returns turnIcon

        view.renderPrimaryTurnIcon(roundaboutManeuver)
        view.renderPrimaryTurnIcon(turnManeuver)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    private fun getTurnIcon() = ManeuverTurnIcon(
        null,
        null,
        false,
        R.drawable.mapbox_ic_turn_left,
    )

    private fun getRoundaboutWithPrimary(maneuver: PrimaryManeuver) = ManeuverTurnIcon(
        maneuver.degrees?.toFloat(),
        maneuver.drivingSide,
        true,
        R.drawable.mapbox_ic_roundabout_left,
    )

    private fun getRoundaboutWithSub(maneuver: SubManeuver) = ManeuverTurnIcon(
        maneuver.degrees?.toFloat(),
        maneuver.drivingSide,
        false,
        R.drawable.mapbox_ic_roundabout_left,
    )
}
