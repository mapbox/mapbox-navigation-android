package com.mapbox.navigation.ui.maps.route.routeline.internal

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource

import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxRouteControllerLineViewTest {

    @Test
    fun renderClearRouteDataState() {
        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val altRoutesFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val primaryRouteSource = mockk<GeoJsonSource>()
        val altRoutesSource = mockk<GeoJsonSource>()
        val waypointSource = mockk<GeoJsonSource>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { getSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns primaryRouteSource
            every { getSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID) } returns altRoutesSource
            every { getSource(RouteConstants.WAYPOINT_SOURCE_ID) } returns waypointSource
        }
        val state = RouteLineState.ClearRouteDataState(
            primaryRouteFeatureCollection,
            altRoutesFeatureCollection,
            waypointsFeatureCollection
        )

        MapboxRouteLineView(style).render(state)

        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
        verify { altRoutesSource.featureCollection(altRoutesFeatureCollection) }
        verify { waypointSource.featureCollection(waypointsFeatureCollection) }
    }

    @Test
    fun renderTraveledRouteLineUpdate() {
        val trafficLayer = mockk<LineLayer>()
        val routeLayer = mockk<LineLayer>()
        val casingLayer = mockk<LineLayer>()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val state = RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(
            trafficLineExp,
            routeLineExp,
            casingLineEx
        )
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { getLayer(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns trafficLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_LAYER_ID) } returns routeLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID) } returns casingLayer
        }

        MapboxRouteLineView(style).render(state)

        verify { trafficLayer.lineGradient(trafficLineExp) }
        verify { routeLayer.lineGradient(routeLineExp) }
        verify { casingLayer.lineGradient(casingLineEx) }
    }

    @Test
    fun renderDrawRouteState() {
        val primaryRouteSource = mockk<GeoJsonSource>()
        val altRoutesSource = mockk<GeoJsonSource>()
        val waypointSource = mockk<GeoJsonSource>()
        val trafficLayer = mockk<LineLayer>()
        val routeLayer = mockk<LineLayer>()
        val casingLayer = mockk<LineLayer>()
        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val altRoutesFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val state = RouteLineState.DrawRouteState(
            primaryRouteFeatureCollection,
            trafficLineExp,
            routeLineExp,
            casingLineEx,
            altRoutesFeatureCollection,
            waypointsFeatureCollection
        )
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { getSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns primaryRouteSource
            every { getSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID) } returns altRoutesSource
            every { getSource(RouteConstants.WAYPOINT_SOURCE_ID) } returns waypointSource
            every { getLayer(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns trafficLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_LAYER_ID) } returns routeLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID) } returns casingLayer
        }

        MapboxRouteLineView(style).render(state)

        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
        verify { altRoutesSource.featureCollection(altRoutesFeatureCollection) }
        verify { waypointSource.featureCollection(waypointsFeatureCollection) }
        verify { trafficLayer.lineGradient(trafficLineExp) }
        verify { routeLayer.lineGradient(routeLineExp) }
        verify { casingLayer.lineGradient(casingLineEx) }
    }
}
