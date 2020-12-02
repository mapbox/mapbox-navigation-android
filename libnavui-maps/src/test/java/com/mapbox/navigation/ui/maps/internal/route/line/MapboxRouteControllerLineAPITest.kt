package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineActions
import com.mapbox.navigation.ui.maps.route.line.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxRouteControllerLineAPITest {

    private fun layerInitializerFun(style: Style) {}

    @Test
    fun setRoutes() {
        val routes = listOf<DirectionsRoute>()
        val drawRouteState = mockk<RouteLineState.DrawRouteState>()
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getDrawRoutesState(routes) } returns drawRouteState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .setRoutes(routes)

        verify { stateConsumer.render(drawRouteState) }
    }

    @Test
    fun setIdentifiableRoutes() {
        val routes = listOf<IdentifiableRoute>()
        val drawRouteState = mockk<RouteLineState.DrawRouteState>()
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getDrawIdentifiableRoutesState(routes) } returns drawRouteState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .setIdentifiableRoutes(routes)

        verify { stateConsumer.render(drawRouteState) }
    }

    @Test
    fun clearRoutes() {
        val state = mockk<RouteLineState.ClearRouteDataState>()
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { clearRouteData() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .clearRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hidePrimaryRouteCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.VISIBLE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHidePrimaryRouteState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .hidePrimaryRoute()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun showPrimaryRouteCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.VISIBLE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowPrimaryRouteState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .showPrimaryRoute()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hideAlternativeRoutesCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.NONE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHideAlternativeRoutesState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .hideAlternativeRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun showAlternativeRoutesCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.NONE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowAlternativeRoutesState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer)
            .showAlternativeRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hideOriginAndDestinationPoints() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.NONE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHideOriginAndDestinationPointsState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer).hideOriginAndDestinationPoints()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun showOriginAndDestinationPoints() {
        val state = RouteLineState.UpdateLayerVisibilityState(
            listOf(Pair("foobar", Visibility.NONE))
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowOriginAndDestinationPointsState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer).showOriginAndDestinationPoints()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun updateTraveledRouteLine() {
        val point = Point.fromLngLat(-122.4727051, 37.7577627)
        val expression = mockk<Expression>()
        val state = RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(
            expression,
            expression,
            expression
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getTraveledRouteLineUpdate(point) } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer).also {
            it.updateTraveledRouteLine(point)
        }

        verify { stateConsumer.render(state) }
    }

    @Test
    fun updatePrimaryRouteIndex() {
        val route = mockk<DirectionsRoute>()
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val state = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getUpdatePrimaryRouteIndexState(route) } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer).also {
            it.updatePrimaryRouteIndex(route)
        }

        verify { stateConsumer.render(state) }
    }
}
