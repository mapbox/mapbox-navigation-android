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
    fun `when maneuver view height is updated`() {
        val sut = ManeuverBehavior()

        sut.updateViewHeight(43)

        assertEquals(43, sut.maneuverViewHeight.value)
    }
}
