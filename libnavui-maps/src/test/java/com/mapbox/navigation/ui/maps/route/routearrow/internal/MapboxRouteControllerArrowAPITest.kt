package com.mapbox.navigation.ui.maps.route.routearrow.internal

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowActions
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class MapboxRouteControllerArrowAPITest {

    private fun layerInitializerFun(style: Style) {}

    @Test
    fun hideManeuverArrow() {
        val state = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val actions = mockk<RouteArrowActions> {
            every { hideRouteArrowState() } returns state
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer, ::layerInitializerFun).hideManeuverArrow()

        verify { viewConsumer.render(state) }
    }

    @Test
    fun showManeuverArrow() {
        val state = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val actions = mockk<RouteArrowActions> {
            every { showRouteArrowState() } returns state
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer, ::layerInitializerFun).showManeuverArrow()

        verify { viewConsumer.render(state) }
    }

    @Test
    fun updateRouteProgress_whenNewRoute() {
        val routeProgress = mockk<RouteProgress>()
        val updateState = RouteArrowState.UpdateManeuverArrowState(listOf(), null, null)
        val hideState = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)
        val actions = mockk<RouteArrowActions> {
            every { hideRouteArrowState() } returns hideState
            every { getAddUpcomingManeuverArrowState(routeProgress) } returns updateState
        }


        MapboxRouteArrowAPI(actions, viewConsumer, ::layerInitializerFun).updateRouteProgress(routeProgress, true)

        verify { viewConsumer.render(hideState) }
        verify(exactly = 0) { viewConsumer.render(updateState) }
    }

    @Test
    fun updateRouteProgress_whenNotNewRoute() {
        val routeProgress = mockk<RouteProgress>()
        val updateState = RouteArrowState.UpdateManeuverArrowState(listOf(), null, null)
        val hideState = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)
        val actions = mockk<RouteArrowActions> {
            every { hideRouteArrowState() } returns hideState
            every { getAddUpcomingManeuverArrowState(routeProgress) } returns updateState
        }

        MapboxRouteArrowAPI(actions, viewConsumer, ::layerInitializerFun).updateRouteProgress(routeProgress, false)

        verify(exactly = 0) { viewConsumer.render(hideState) }
        verify { viewConsumer.render(updateState) }
    }

    @Test
    fun updateStyleInitializesLayer() {
        val showArrowState = RouteArrowState.UpdateRouteArrowVisibilityState(listOf())
        val updateState = RouteArrowState.UpdateManeuverArrowState(listOf(), null, null)
        var initializeLayerFunGotCalled = false
        val initializeLayerFun: (Style) -> Unit = { _ ->
            initializeLayerFunGotCalled = true
        }
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val actions = mockk<RouteArrowActions> {
            every { showRouteArrowState() } returns showArrowState
            every { redraw() } returns updateState
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer, initializeLayerFun).updateStyle(style)

        Assert.assertTrue(initializeLayerFunGotCalled)
    }

    @Test
    fun updateStyleCallsRedraw() {
        val foobarLayer = mockk<Layer>(relaxUnitFun = true)
        val showArrowState = RouteArrowState.UpdateRouteArrowVisibilityState(listOf(Pair("foobar", Visibility.VISIBLE)))
        val updateState = RouteArrowState.UpdateManeuverArrowState(listOf(), null, null)
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
            every { getLayer("foobar") } returns foobarLayer
        }
        val actions = mockk<RouteArrowActions> {
            every { showRouteArrowState() } returns showArrowState
            every { redraw() } returns updateState
        }
        val viewConsumer = mockk<MapboxRouteArrowView>(relaxUnitFun = true)

        MapboxRouteArrowAPI(actions, viewConsumer, ::layerInitializerFun).updateStyle(style)

        verify { foobarLayer.visibility(Visibility.VISIBLE) }
    }
}
