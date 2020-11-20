package com.mapbox.navigation.ui.maps.route.line.api

class MapboxRouteLineViewTest {

    // todo restore this when the map issue is resolved which causes
    // java.lang.UnsatisfiedLinkError: com.mapbox.common.Log.error(Ljava/lang/String;Ljava/lang/String;)V
//    @Ignore
//    @Test
//    fun renderClearRouteDataState() {
//        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val altRoutesFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val primaryRouteSource = mockk<GeoJsonSource>()
//        val altRoutesSource = mockk<GeoJsonSource>()
//        val waypointSource = mockk<GeoJsonSource>()
//        val style = mockk<Style> {
//            every { isFullyLoaded() } returns true
//            every { getSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns primaryRouteSource
//            every { getSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID) } returns altRoutesSource
//            every { getSource(RouteConstants.WAYPOINT_SOURCE_ID) } returns waypointSource
//        }
//        val state = RouteLineState.ClearRouteDataState(
//            primaryRouteFeatureCollection,
//            altRoutesFeatureCollection,
//            waypointsFeatureCollection
//        )
//
//        MapboxRouteLineView().render(state)
//
//        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
//        verify { altRoutesSource.featureCollection(altRoutesFeatureCollection) }
//        verify { waypointSource.featureCollection(waypointsFeatureCollection) }
//    }

    // todo restore this when the map issue is resolved which causes
    // java.lang.UnsatisfiedLinkError: com.mapbox.common.Log.error(Ljava/lang/String;Ljava/lang/String;)V
//    @Ignore
//    @Test
//    fun renderTraveledRouteLineUpdate() {
//        val trafficLayer = mockk<LineLayer>()
//        val routeLayer = mockk<LineLayer>()
//        val casingLayer = mockk<LineLayer>()
//        val trafficLineExp = mockk<Expression>()
//        val routeLineExp = mockk<Expression>()
//        val casingLineEx = mockk<Expression>()
//        val state = RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(
//            trafficLineExp,
//            routeLineExp,
//            casingLineEx
//        )
//        val style = mockk<Style> {
//            every { isFullyLoaded() } returns true
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns trafficLayer
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_LAYER_ID) } returns routeLayer
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID) } returns casingLayer
//        }
//
//        MapboxRouteLineView().render(state)
//
//        verify { trafficLayer.lineGradient(trafficLineExp) }
//        verify { routeLayer.lineGradient(routeLineExp) }
//        verify { casingLayer.lineGradient(casingLineEx) }
//    }

    // todo restore this when the map issue is resolved which causes
    // java.lang.UnsatisfiedLinkError: com.mapbox.common.Log.error(Ljava/lang/String;Ljava/lang/String;)V
//    @Ignore
//    @Test
//    fun renderDrawRouteState() {
//        val primaryRouteSource = mockk<GeoJsonSource>()
//        val altRoutesSource = mockk<GeoJsonSource>()
//        val waypointSource = mockk<GeoJsonSource>()
//        val trafficLayer = mockk<LineLayer>()
//        val routeLayer = mockk<LineLayer>()
//        val casingLayer = mockk<LineLayer>()
//        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val altRoutesFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
//        val trafficLineExp = mockk<Expression>()
//        val routeLineExp = mockk<Expression>()
//        val casingLineEx = mockk<Expression>()
//        val state = RouteLineState.DrawRouteState(
//            primaryRouteFeatureCollection,
//            trafficLineExp,
//            routeLineExp,
//            casingLineEx,
//            altRoutesFeatureCollection,
//            waypointsFeatureCollection
//        )
//        val style = mockk<Style> {
//            every { isFullyLoaded() } returns true
//            every { getSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns primaryRouteSource
//            every { getSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID) } returns altRoutesSource
//            every { getSource(RouteConstants.WAYPOINT_SOURCE_ID) } returns waypointSource
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns trafficLayer
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_LAYER_ID) } returns routeLayer
//            every { getLayer(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID) } returns casingLayer
//        }
//
//        val updateStyleState: RouteLineState.UpdateViewStyleState =
//            RouteLineState.UpdateViewStyleState(style)
//
//        MapboxRouteLineView().also {
//            it.render(updateStyleState)
//            it.render(state)
//        }
//
//        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
//        verify { altRoutesSource.featureCollection(altRoutesFeatureCollection) }
//        verify { waypointSource.featureCollection(waypointsFeatureCollection) }
//        verify { trafficLayer.lineGradient(trafficLineExp) }
//        verify { routeLayer.lineGradient(routeLineExp) }
//        verify { casingLayer.lineGradient(casingLineEx) }
//    }
}
