package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.View.GONE
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.TurnIconHelper
import com.mapbox.navigation.ui.maneuver.model.TurnIcon
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxTurnIconManeuverTest {

    private val turnIconHelper = mockk<TurnIconHelper>()
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render primary maneuver icon flip icon`() {
        val maneuver = getRoundaboutPrimaryManeuver()
        val mockState = ManeuverState.ManeuverPrimary.Instruction(maneuver)
        val turnIcon = getTurnIconForPrimary(maneuver)
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 180f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type, maneuver.degrees?.toFloat(), maneuver.drivingSide, maneuver.modifier
            )
        } returns turnIcon

        view.render(mockState)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `render sub maneuver icon flip icon`() {
        val maneuver = getRoundaboutSubManeuver()
        val mockState = ManeuverState.ManeuverSub.Instruction(maneuver)
        val turnIcon = getTurnIconForSub(maneuver)
        val view = MapboxTurnIconManeuver(ctx)
        val expectedFlip = 180f
        every {
            turnIconHelper.retrieveTurnIcon(
                maneuver.type, maneuver.degrees?.toFloat(), maneuver.drivingSide, maneuver.modifier
            )
        } returns turnIcon

        view.render(mockState)
        val actualFlip = view.rotationY

        assertEquals(expectedFlip, actualFlip)
    }

    @Test
    fun `render view visibility`() {
        val expectedVisibility = GONE
        val mockState = ManeuverState.ManeuverSub.Hide
        val view = MapboxTurnIconManeuver(ctx)

        view.render(mockState)

        assertEquals(expectedVisibility, view.visibility)
    }

    private fun getPrimaryManeuver() = PrimaryManeuver
        .Builder()
        .text("I-880")
        .type(StepManeuver.TURN)
        .degrees(null)
        .modifier(ManeuverModifier.LEFT)
        .drivingSide("left")
        .componentList(listOf())
        .build()

    private fun getRoundaboutPrimaryManeuver() = PrimaryManeuver
        .Builder()
        .text("I-880")
        .type(StepManeuver.ROUNDABOUT)
        .degrees(23.0)
        .modifier(ManeuverModifier.LEFT)
        .drivingSide("left")
        .componentList(listOf())
        .build()

    private fun getTurnIconForPrimary(maneuver: PrimaryManeuver) = TurnIcon(
        maneuver.degrees?.toFloat(),
        maneuver.drivingSide,
        false,
        R.drawable.mapbox_ic_turn_left
    )

    private fun getSubManeuver() = SubManeuver
        .Builder()
        .text("I-880")
        .type(StepManeuver.TURN)
        .degrees(null)
        .modifier(ManeuverModifier.LEFT)
        .drivingSide("left")
        .componentList(listOf())
        .build()

    private fun getRoundaboutSubManeuver() = SubManeuver
        .Builder()
        .text("I-880")
        .type(StepManeuver.ROUNDABOUT)
        .degrees(23.0)
        .modifier(ManeuverModifier.LEFT)
        .drivingSide("left")
        .componentList(listOf())
        .build()

    private fun getTurnIconForSub(maneuver: SubManeuver) = TurnIcon(
        maneuver.degrees?.toFloat(),
        maneuver.drivingSide,
        false,
        R.drawable.mapbox_ic_turn_left
    )
}
