package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import org.junit.Assert.assertEquals
import org.junit.Test

class ManeuverBehaviorTest {

    @Test
    fun `when maneuver behavior is updated`() {
        val sut = ManeuverBehavior()

        sut.updateBehavior(MapboxManeuverViewState.EXPANDED)

        assertEquals(MapboxManeuverViewState.EXPANDED, sut.maneuverBehavior.value)
    }

    @Test
    fun `when maneuver view visibility is updated to true`() {
        val sut = ManeuverBehavior()

        sut.updateViewVisibility(true)

        assertEquals(true, sut.maneuverViewVisibility.value)
    }

    @Test
    fun `when maneuver view visibility is updated to false`() {
        val sut = ManeuverBehavior()
        sut.updateViewVisibility(true)

        sut.updateViewVisibility(false)

        assertEquals(false, sut.maneuverViewVisibility.value)
    }
}
