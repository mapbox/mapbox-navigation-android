package com.mapbox.navigation.ui.maps.route.routearrow.internal

import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowActions
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxRouteArrowAPITest {

    @Test
    fun hideManeuverArrow() {
        val state = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val actions = mockk<RouteArrowActions> {
            every { hideRouteArrowState() } returns state
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer).hideManeuverArrow()

        verify { viewConsumer.render(state) }
    }

    @Test
    fun showManeuverArrow() {
        val state = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val actions = mockk<RouteArrowActions> {
            every { showRouteArrowState() } returns state
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer).showManeuverArrow()

        verify { viewConsumer.render(state) }
    }
}
