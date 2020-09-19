package com.mapbox.navigation.ui.maps.route.routeline.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineAPI
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
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

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
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

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
            .clearRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hidePrimaryRouteCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.VISIBLE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHidePrimaryRouteState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
            .hidePrimaryRoute()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hidePrimaryRouteSetsInternalVariable() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHidePrimaryRouteState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.hidePrimaryRoute()
        }

        assertEquals(Visibility.NONE, (api as MapboxRouteLineAPI).primaryRouteLineLayerVisibility)
    }

    @Test
    fun showPrimaryRouteCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.VISIBLE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowPrimaryRouteState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
            .showPrimaryRoute()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun showPrimaryRouteSetsInternalVariable() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowPrimaryRouteState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.showPrimaryRoute()
        }

        assertEquals(Visibility.NONE, api.primaryRouteLineLayerVisibility)
    }

    @Test
    fun hideAlternativeRoutesCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHideAlternativeRoutesState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
            .hideAlternativeRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun hideAlternativeRoutesSetsInternalVariable() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHideAlternativeRoutesState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.hideAlternativeRoutes()
        }

        assertEquals(Visibility.NONE, api.alternativeRouteLineLayerVisibility)
    }

    @Test
    fun showAlternativeRoutesCallsRenderOnConsumer() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowAlternativeRoutesState() } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun)
            .showAlternativeRoutes()

        verify { stateConsumer.render(state) }
    }

    @Test
    fun showAlternativeRoutesSetsInternalVariable() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowAlternativeRoutesState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.showAlternativeRoutes()
        }

        assertEquals(Visibility.NONE, api.alternativeRouteLineLayerVisibility)
    }

    @Test
    fun hideOriginAndDestinationPoints() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getHideOriginAndDestinationPointsState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.hideOriginAndDestinationPoints()
        }

        assertEquals(Visibility.NONE, api.originAndDestinationPointsVisibility)
    }

    @Test
    fun showOriginAndDestinationPoints() {
        val state = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getShowOriginAndDestinationPointsState() } returns state
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.showOriginAndDestinationPoints()
        }

        assertEquals(Visibility.NONE, api.originAndDestinationPointsVisibility)
    }

    @Test
    fun updateTraveledRouteLine() {
        val point = Point.fromLngLat(-122.4727051, 37.7577627)
        val expression = mockk<Expression>()
        val state = RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(expression, expression, expression)
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { getTraveledRouteLineUpdate(point) } returns state
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
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

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updatePrimaryRouteIndex(route)
        }

        verify { stateConsumer.render(state) }
    }

    @Test
    fun updateRouteProgressDrawsRoutes() {
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "geometry"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
            every { distanceRemaining } returns 99f
            every { currentState } returns RouteProgressState.OFF_ROUTE
        }
        val routeLineState = RouteLineState.UnitState()
        val drawRouteState = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            //every { updateDistanceRemaining(99f, directionsRoute) } returns routeLineState
            every { getPrimaryRoute() } returns mockk<DirectionsRoute>()
            every { getDrawRoutesState(listOf(directionsRoute)) } returns drawRouteState
            //every { inhibitVanishingRouteLine(any()) } returns routeLineState //todo may need update to RouteProgressState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateRouteProgress(routeProgress)
        }

        verify { stateConsumer.render(routeLineState) }
        verify { stateConsumer.render(drawRouteState) }
    }

    @Test
    fun updateRouteProgressDrawsRoutesWhenSameRouteDoesNotDrawRoutes() {
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "geometry"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
            every { distanceRemaining } returns 99f
            every { currentState } returns RouteProgressState.OFF_ROUTE
        }
        val routeLineState = RouteLineState.UnitState()
        val drawRouteState = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            //every { updateDistanceRemaining(99f, directionsRoute) } returns routeLineState
            every { getPrimaryRoute() } returns directionsRoute
            every { getDrawRoutesState(listOf(directionsRoute)) } returns drawRouteState
            //every { inhibitVanishingRouteLine(any()) } returns routeLineState //todo may need update to RouteProgressState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateRouteProgress(routeProgress)
        }

        verify(exactly = 0) { stateConsumer.render(drawRouteState) }
    }

    @Test
    fun updateRouteProgressWhenProgressState_LOCATION_TRACKING() {
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "geometry"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
            every { distanceRemaining } returns 99f
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
        }
        val routeLineState = RouteLineState.UnitState()
        val drawRouteState = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            //every { updateDistanceRemaining(99f, directionsRoute) } returns routeLineState
            every { getPrimaryRoute() } returns directionsRoute
            every { getDrawRoutesState(listOf(directionsRoute)) } returns drawRouteState
            //every { inhibitVanishingRouteLine(false) } returns routeLineState //todo may need update to RouteProgressState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateRouteProgress(routeProgress)
        }

        verify { stateConsumer.render(routeLineState) }
    }

    @Test
    fun updateRouteProgressWhenProgressState_ROUTE_COMPLETE() {
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "geometry"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
            every { distanceRemaining } returns 99f
            every { currentState } returns RouteProgressState.ROUTE_COMPLETE
        }
        val routeLineState = RouteLineState.UnitState()
        val drawRouteState = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            //every { updateDistanceRemaining(99f, directionsRoute) } returns routeLineState
            every { getPrimaryRoute() } returns directionsRoute
            every { getDrawRoutesState(listOf(directionsRoute)) } returns drawRouteState
            //every { inhibitVanishingRouteLine(false) } returns routeLineState //todo may need update to RouteProgressState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateRouteProgress(routeProgress)
        }

        verify { stateConsumer.render(routeLineState) }
    }

    @Test
    fun updateRouteProgressWhenProgressState_Default() {
        val expression = mockk<Expression>()
        val featureCollection = mockk<FeatureCollection>()
        val directionsRoute = mockk<DirectionsRoute> {
            every { geometry() } returns "geometry"
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns directionsRoute
            every { distanceRemaining } returns 99f
            every { currentState } returns RouteProgressState.OFF_ROUTE
        }
        val routeLineState = RouteLineState.UnitState()
        val drawRouteState = RouteLineState.DrawRouteState(
            featureCollection,
            expression,
            expression,
            expression,
            featureCollection,
            featureCollection
        )
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            //every { updateDistanceRemaining(99f, directionsRoute) } returns routeLineState
            every { getPrimaryRoute() } returns directionsRoute
            every { getDrawRoutesState(listOf(directionsRoute)) } returns drawRouteState
            //every { inhibitVanishingRouteLine(true) } returns routeLineState //todo may need update to RouteProgressState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateRouteProgress(routeProgress)
        }

        verify { stateConsumer.render(routeLineState) }
    }

    @Test
    fun updateStyleCallsRedraw() {
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val visibilityState = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val state = mockk<RouteLineState.DrawRouteState> {
            every { getTrafficLineExpression() } returns Expression.color(1)
            every { getRouteLineExpression() } returns Expression.color(1)
            every { getCasingLineExpression() } returns Expression.color(1)
            every { getPrimaryRouteSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getAlternateRoutesSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getWaypointsSource() } returns FeatureCollection.fromFeatures(listOf())
        }
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { redraw() } returns state
            every { getShowPrimaryRouteState() } returns visibilityState
            every { getShowAlternativeRoutesState() } returns visibilityState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).updateStyle(style)

        verify { routeLineActions.redraw() }
    }

    @Test
    fun updateStyleRendersRedrawState() {
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val visibilityState = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val state = mockk<RouteLineState.DrawRouteState> {
            every { getTrafficLineExpression() } returns Expression.color(1)
            every { getRouteLineExpression() } returns Expression.color(1)
            every { getCasingLineExpression() } returns Expression.color(1)
            every { getPrimaryRouteSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getAlternateRoutesSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getWaypointsSource() } returns FeatureCollection.fromFeatures(listOf())
        }
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { redraw() } returns state
            every { getShowPrimaryRouteState() } returns visibilityState
            every { getShowAlternativeRoutesState() } returns visibilityState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).updateStyle(style)

        verify { state.getTrafficLineExpression() }
        verify { state.getRouteLineExpression() }
        verify { state.getCasingLineExpression() }
        verify { state.getPrimaryRouteSource() }
        verify { state.getAlternateRoutesSource() }
        verify { state.getWaypointsSource() }
    }

    @Test
    fun updateStyleCallsShowHidePrimaryRoute() {
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val visibilityState = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val state = mockk<RouteLineState.DrawRouteState> {
            every { getTrafficLineExpression() } returns Expression.color(1)
            every { getRouteLineExpression() } returns Expression.color(1)
            every { getCasingLineExpression() } returns Expression.color(1)
            every { getPrimaryRouteSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getAlternateRoutesSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getWaypointsSource() } returns FeatureCollection.fromFeatures(listOf())
        }
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { redraw() } returns state
            every { getShowPrimaryRouteState() } returns visibilityState
            every { getShowAlternativeRoutesState() } returns visibilityState
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateStyle(style)
        }
        assertEquals(Visibility.NONE, api.primaryRouteLineLayerVisibility)
    }

    @Test
    fun updateStyleCallsShowHideAlternativeRoutes() {
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val visibilityState = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val state = mockk<RouteLineState.DrawRouteState> {
            every { getTrafficLineExpression() } returns Expression.color(1)
            every { getRouteLineExpression() } returns Expression.color(1)
            every { getCasingLineExpression() } returns Expression.color(1)
            every { getPrimaryRouteSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getAlternateRoutesSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getWaypointsSource() } returns FeatureCollection.fromFeatures(listOf())
        }
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { redraw() } returns state
            every { getShowPrimaryRouteState() } returns visibilityState
            every { getShowAlternativeRoutesState() } returns visibilityState
        }

        val api = MapboxRouteLineAPI(routeLineActions, stateConsumer, ::layerInitializerFun).also {
            it.updateStyle(style)
        }
        assertEquals(Visibility.NONE, api.alternativeRouteLineLayerVisibility)
    }

    @Test
    fun updateStyleInitializesLayer() {
        var initializeLayerFunGotCalled = false
        val initializeLayerFun: (Style) -> Unit = { style ->
            initializeLayerFunGotCalled = true
        }
        val style = mockk<Style> {
            every { isFullyLoaded() } returns false
        }
        val visibilityState = RouteLineState.UpdateLayerVisibilityState(listOf(Pair("foobar", Visibility.NONE)))
        val state = mockk<RouteLineState.DrawRouteState> {
            every { getTrafficLineExpression() } returns Expression.color(1)
            every { getRouteLineExpression() } returns Expression.color(1)
            every { getCasingLineExpression() } returns Expression.color(1)
            every { getPrimaryRouteSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getAlternateRoutesSource() } returns FeatureCollection.fromFeatures(listOf())
            every { getWaypointsSource() } returns FeatureCollection.fromFeatures(listOf())
        }
        val stateConsumer = mockk<MapboxRouteLineView>(relaxUnitFun = true)
        val routeLineActions = mockk<RouteLineActions> {
            every { redraw() } returns state
            every { getShowPrimaryRouteState() } returns visibilityState
            every { getShowAlternativeRoutesState() } returns visibilityState
        }

        MapboxRouteLineAPI(routeLineActions, stateConsumer, initializeLayerFun).updateStyle(style)

        assertTrue(initializeLayerFunGotCalled)
    }
}
