package com.mapbox.navigation.ui.maps.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowAPI
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineAPI
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteControllerTest {

    val routeTools = object : RouteController {
        override val mapboxRouteArrowAPI: RouteArrowAPI = mockk<RouteArrowAPI>(relaxUnitFun = true)
        override val mapboxRouteLineAPI: RouteLineAPI = mockk<RouteLineAPI>(relaxUnitFun = true)
    }

    @Test
    fun clearRoutes() {
        routeTools.clearRoutes()

        verify { routeTools.mapboxRouteLineAPI.clearRoutes() }
    }

    @Test
    fun updateStyle() {
        val style = mockk<Style> { }

        routeTools.updateStyle(style)

        verify { routeTools.mapboxRouteLineAPI.updateStyle(style) }
        verify { routeTools.mapboxRouteArrowAPI.updateStyle(style) }
    }

    @Test
    fun hideAlternativeRoutes() {
        routeTools.hideAlternativeRoutes()

        verify { routeTools.mapboxRouteLineAPI.hideAlternativeRoutes() }
    }

    @Test
    fun hideOriginAndDestinationPoints() {
        routeTools.hideOriginAndDestinationPoints()

        verify { routeTools.mapboxRouteLineAPI.hideOriginAndDestinationPoints() }
    }

    @Test
    fun hidePrimaryRoute() {
        routeTools.hidePrimaryRoute()

        verify { routeTools.mapboxRouteLineAPI.hidePrimaryRoute() }
    }

    @Test
    fun setIdentifiableRoutes() {
        val inputList = listOf<IdentifiableRoute>()

        routeTools.setIdentifiableRoutes(inputList)

        verify { routeTools.mapboxRouteLineAPI.setIdentifiableRoutes(inputList) }
    }

    @Test
    fun setRoutes() {
        val routes = listOf<DirectionsRoute>()

        routeTools.setRoutes(routes)

        verify { routeTools.mapboxRouteLineAPI.setRoutes(routes) }
    }

    @Test
    fun showAlternativeRoutes() {
        routeTools.showAlternativeRoutes()

        verify { routeTools.mapboxRouteLineAPI.showAlternativeRoutes() }
    }

    @Test
    fun showOriginAndDestinationPoints() {
        routeTools.showOriginAndDestinationPoints()

        verify { routeTools.mapboxRouteLineAPI.showOriginAndDestinationPoints() }
    }

    @Test
    fun showPrimaryRoute() {
        routeTools.showPrimaryRoute()

        verify { routeTools.mapboxRouteLineAPI.showPrimaryRoute() }
    }

    @Test
    fun updatePrimaryRouteIndex() {
        val route = mockk<DirectionsRoute>()

        routeTools.updatePrimaryRouteIndex(route)

        verify { routeTools.mapboxRouteLineAPI.updatePrimaryRouteIndex(route) }
    }

    @Test
    fun updateRouteProgress_NotNewRouteWhenGeometryEmpty() {
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns ""
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
        }

        routeTools.updateRouteProgress(routeProgress)

        verify { routeTools.mapboxRouteLineAPI.updateRouteProgress(routeProgress) }
        verify { routeTools.mapboxRouteArrowAPI.updateRouteProgress(routeProgress, false) }
    }

    @Test
    fun updateRouteProgress_NotNewRouteWhenCurrentRouteMatchesPrimaryRoute() {
        val primaryRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "notEmpty"
        }
        every { routeTools.mapboxRouteLineAPI.getPrimaryRoute() } returns primaryRoute
        val routeProgress = mockk<RouteProgress> {
            every { route } returns primaryRoute
        }

        routeTools.updateRouteProgress(routeProgress)

        verify { routeTools.mapboxRouteLineAPI.updateRouteProgress(routeProgress) }
        verify { routeTools.mapboxRouteArrowAPI.updateRouteProgress(routeProgress, false) }
    }

    @Test
    fun updateRouteProgress_whenNewRoute() {
        val primaryRoute = mockk<DirectionsRoute>()
        every { routeTools.mapboxRouteLineAPI.getPrimaryRoute() } returns primaryRoute
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "notEmpty"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
        }

        routeTools.updateRouteProgress(routeProgress)

        verify { routeTools.mapboxRouteLineAPI.updateRouteProgress(routeProgress) }
        verify { routeTools.mapboxRouteArrowAPI.updateRouteProgress(routeProgress, true) }
    }

    @Test
    fun updateRouteProgress() {
        val routeProgress = mockk<RouteProgress>()

        routeTools.updateRouteProgress(routeProgress, false)

        verify { routeTools.mapboxRouteArrowAPI.updateRouteProgress(routeProgress, false) }
        verify { routeTools.mapboxRouteLineAPI.updateRouteProgress(routeProgress) }
    }

    @Test
    fun updateTraveledRouteLine() {
        val point = Point.fromLngLat(44.0, 33.0)

        routeTools.updateTraveledRouteLine(point)

        verify { routeTools.mapboxRouteLineAPI.updateTraveledRouteLine(point) }
    }

    @Test
    fun getPrimaryRoute() {
        val primaryRoute = mockk<DirectionsRoute>()
        every { routeTools.mapboxRouteLineAPI.getPrimaryRoute() } returns primaryRoute

        val result = routeTools.getPrimaryRoute()

        assertEquals(primaryRoute, result)
    }

    @Test
    fun hideManeuverArrow() {
        routeTools.hideManeuverArrow()

        verify { routeTools.mapboxRouteArrowAPI.hideManeuverArrow() }
    }

    @Test
    fun showManeuverArrow() {
        routeTools.showManeuverArrow()

        verify { routeTools.mapboxRouteArrowAPI.showManeuverArrow() }
    }
}
