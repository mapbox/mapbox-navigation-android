package com.mapbox.navigation.ui.route

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.libnavigation.ui.R
import com.mapbox.mapboxsdk.location.LocationComponentConstants
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.ui.ThemeSwitcher
import com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_SHIELD_LAYER_ID
import com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_SHIELD_LAYER_ID
import com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_LAYER_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapRouteLineTest {

    lateinit var ctx: Context
    var styleRes: Int = 0
    lateinit var wayPointSource: GeoJsonSource
    lateinit var primaryRouteLineSource: GeoJsonSource
    lateinit var alternativeRouteLineSource: GeoJsonSource

    lateinit var mapRouteSourceProvider: MapRouteSourceProvider
    lateinit var layerProvider: MapRouteLayerProvider
    lateinit var alternativeRouteShieldLayer: LineLayer
    lateinit var alternativeRouteLayer: LineLayer
    lateinit var primaryRouteShieldLayer: LineLayer
    lateinit var primaryRouteLayer: LineLayer
    lateinit var waypointLayer: SymbolLayer

    lateinit var style: Style

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle, R.style.NavigationMapRoute
        )
        alternativeRouteShieldLayer = mockk {
            every { id } returns ALTERNATIVE_ROUTE_SHIELD_LAYER_ID
        }

        alternativeRouteLayer = mockk {
            every { id } returns ALTERNATIVE_ROUTE_LAYER_ID
        }

        primaryRouteShieldLayer = mockk {
            every { id } returns PRIMARY_ROUTE_SHIELD_LAYER_ID
        }

        primaryRouteLayer = mockk {
            every { id } returns PRIMARY_ROUTE_LAYER_ID
        }

        waypointLayer = mockk {
            every { id } returns WAYPOINT_LAYER_ID
        }

        style = mockk(relaxUnitFun = true) {
            every { getLayer(ALTERNATIVE_ROUTE_LAYER_ID) } returns alternativeRouteLayer
            every { getLayer(ALTERNATIVE_ROUTE_SHIELD_LAYER_ID) } returns alternativeRouteShieldLayer
            every { getLayer(PRIMARY_ROUTE_LAYER_ID) } returns primaryRouteLayer
            every { getLayer(PRIMARY_ROUTE_SHIELD_LAYER_ID) } returns primaryRouteShieldLayer
            every { getLayer(WAYPOINT_LAYER_ID) } returns waypointLayer
            every { isFullyLoaded } returns false
        }

        wayPointSource = mockk(relaxUnitFun = true)
        primaryRouteLineSource = mockk(relaxUnitFun = true)
        alternativeRouteLineSource = mockk(relaxUnitFun = true)

        mapRouteSourceProvider = mockk {
            every { build(RouteConstants.WAYPOINT_SOURCE_ID, any(), any()) } returns wayPointSource
            every { build(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any()) } returns primaryRouteLineSource
            every { build(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID, any(), any()) } returns alternativeRouteLineSource
        }
        layerProvider = mockk {
            every { initializeAlternativeRouteShieldLayer(style, 1.0f, -9273715) } returns alternativeRouteShieldLayer
            every { initializeAlternativeRouteLayer(style, true, 1.0f, -7957339) } returns alternativeRouteLayer
            every { initializePrimaryRouteShieldLayer(style, 1.0f, -13665594) } returns primaryRouteShieldLayer
            every { initializePrimaryRouteLayer(style, true, 1.0f, -11097861) } returns primaryRouteLayer
            every { initializeWayPointLayer(style, any(), any()) } returns waypointLayer
        }
    }

    @Test
    fun getStyledColor() {
        val result = MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            ctx,
            styleRes
        )

        assertEquals(-11097861, result)
    }

    @Test
    fun getPrimaryRoute() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.getPrimaryRoute()

        assertEquals(result, directionsRoute)
    }

    @Test
    fun getLineStringForRoute() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.getLineStringForRoute(directionsRoute)

        assertEquals(result.coordinates().size, 4)
    }

    @Test
    fun getLineStringForRouteWhenCalledWithUnknownRoute() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val directionsRoute2: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.getLineStringForRoute(directionsRoute2)

        assertNotNull(result)
    }

    @Test
    fun retrieveRouteFeatureData() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.retrieveRouteFeatureData()

        assertEquals(result.size, 1)
        assertEquals(result[0].route, directionsRoute)
    }

    @Test
    fun retrieveRouteLineStrings() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.retrieveRouteLineStrings()

        assertEquals(result.size, 1)
    }

    @Test
    fun retrieveDirectionsRoutes() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute)) }

        val result = mapRouteLine.retrieveDirectionsRoutes()

        assertEquals(result[0], directionsRoute)
    }

    @Test
    fun retrieveDirectionsRoutesPrimaryRouteIsFirstInList() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val primaryRoute: DirectionsRoute = getDirectionsRoute(true)
        val alternativeRoute: DirectionsRoute = getDirectionsRoute(false)
        val directionsRoutes = mutableListOf(primaryRoute, alternativeRoute)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(directionsRoutes) }
        directionsRoutes.reverse()

        val result = mapRouteLine.retrieveDirectionsRoutes()

        assertEquals(result[0], primaryRoute)
        assertEquals(2, result.size)
    }

    @Test
    fun retrieveDirectionsRoutesWhenPrimaryRouteIsNull() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val firstRoute: DirectionsRoute = getDirectionsRoute(true)
        val secondRoute: DirectionsRoute = getDirectionsRoute(false)
        val directionsRoutes = listOf(
            RouteFeatureData(firstRoute, mockk<FeatureCollection>(), mockk<LineString>()),
            RouteFeatureData(secondRoute, mockk<FeatureCollection>(), mockk<LineString>()))
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            directionsRoutes,
            false,
            false,
            mapRouteSourceProvider)

        val result = mapRouteLine.retrieveDirectionsRoutes()

        assertEquals(2, result.size)
    }

    @Test
    fun getTopLayerId() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider)

        val result = mapRouteLine.getTopLayerId()

        assertEquals(result, "mapbox-navigation-waypoint-layer")
    }

    @Test
    fun updatePrimaryRouteIndex() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val directionsRoute: DirectionsRoute = getDirectionsRoute(true)
        val directionsRoute2: DirectionsRoute = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider).also { it.draw(listOf(directionsRoute, directionsRoute2)) }

        assertEquals(mapRouteLine.getPrimaryRoute(), directionsRoute)

        mapRouteLine.updatePrimaryRouteIndex(directionsRoute2)
        val result = mapRouteLine.getPrimaryRoute()

        assertEquals(result, directionsRoute2)
    }

    @Test
    fun getStyledColorRecyclesAttributes() {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every { context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute) } returns typedArray
        every { context.resources } returns resources
        every { context.getColor(R.color.mapbox_navigation_route_layer_blue) } returns 0
        every { resources.getColor(R.color.mapbox_navigation_route_layer_blue) } returns 0
        every { typedArray.getColor(R.styleable.NavigationMapRoute_routeColor, anyInt()) } returns 0

        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getFloatStyledValue() {
        val result: Float = MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.NavigationMapRoute_alternativeRouteScale,
            1.0f,
            ctx,
            styleRes
        )

        assertEquals(1.0f, result)
    }

    @Test
    fun getFloatStyledValueRecyclesAttributes() {
        val context = mockk<Context>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every { context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute) } returns typedArray
        every { typedArray.getFloat(R.styleable.NavigationMapRoute_alternativeRouteScale, 1.0f) } returns 1.0f

        MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.NavigationMapRoute_alternativeRouteScale,
            1.0f,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getBooleanStyledValue() {
        val result = MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
            R.styleable.NavigationMapRoute_roundedLineCap,
            true,
            ctx,
            styleRes
        )

        assertEquals(true, result)
    }

    @Test
    fun getBooleanStyledValueRecyclesAttributes() {
        val context = mockk<Context>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every { context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute) } returns typedArray
        every { typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true) } returns true

        MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
            R.styleable.NavigationMapRoute_roundedLineCap,
            true,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getResourceStyledValue() {
        val result = MapRouteLine.MapRouteLineSupport.getResourceStyledValue(
            R.styleable.NavigationMapRoute_originWaypointIcon,
            R.drawable.ic_route_origin,
            ctx,
            styleRes
        )

        assertEquals(R.drawable.ic_route_origin, result)
    }

    @Test
    fun getResourceStyledValueRecyclesAttributes() {
        val context = mockk<Context>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every { context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute) } returns typedArray
        every { typedArray.getResourceId(R.styleable.NavigationMapRoute_originWaypointIcon, R.drawable.ic_route_origin) } returns R.drawable.ic_route_origin

        MapRouteLine.MapRouteLineSupport.getResourceStyledValue(
            R.styleable.NavigationMapRoute_originWaypointIcon,
            R.drawable.ic_route_origin,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getBelowLayerWithNullLayerId() {
        val style = mockk<Style>()
        val layerApple = mockk<Layer>()
        val layerBanana = mockk<Layer>()
        val layerCantaloupe = mockk<Layer>()
        val layerDragonfruit = mockk<SymbolLayer>()
        val layers = listOf(layerApple, layerBanana, layerCantaloupe, layerDragonfruit)
        every { style.layers } returns layers
        every { layerApple.id } returns "layerApple"
        every { layerBanana.id } returns RouteConstants.MAPBOX_LOCATION_ID
        every { layerCantaloupe.id } returns "layerCantaloupe"
        every { layerDragonfruit.id } returns "layerDragonfruit"

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer(null, style)

        assertEquals("layerCantaloupe", result)
    }

    @Test
    fun getBelowLayerWithEmptyLayerId() {
        val style = mockk<Style>()
        val layerApple = mockk<Layer>()
        val layerBanana = mockk<Layer>()
        val layerCantaloupe = mockk<Layer>()
        val layerDragonfruit = mockk<SymbolLayer>()
        val layers = listOf(layerApple, layerBanana, layerCantaloupe, layerDragonfruit)
        every { style.layers } returns layers
        every { layerApple.id } returns "layerApple"
        every { layerBanana.id } returns RouteConstants.MAPBOX_LOCATION_ID
        every { layerCantaloupe.id } returns "layerCantaloupe"
        every { layerDragonfruit.id } returns "layerDragonfruit"

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer("", style)

        assertEquals("layerCantaloupe", result)
    }

    @Test
    fun getBelowLayerReturnsShadowLayerIdAsDefault() {
        val style = mockk<Style>()
        val layerApple = mockk<Layer>()
        val layerBanana = mockk<SymbolLayer>()
        val layers = listOf(layerApple, layerBanana)
        every { style.layers } returns layers
        every { layerApple.id } returns RouteConstants.MAPBOX_LOCATION_ID
        every { layerBanana.id } returns "layerBanana"

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer(null, style)

        assertEquals(LocationComponentConstants.SHADOW_LAYER, result)
    }

    @Test
    fun getBelowLayerReturnsInputIdIfFound() {
        val style = mockk<Style>()
        val layerApple = mockk<Layer>()
        val layerBanana = mockk<Layer>()
        val layerCantaloupe = mockk<Layer>()
        val layerDragonfruit = mockk<Layer>()
        val layers = listOf(layerApple, layerBanana, layerCantaloupe, layerDragonfruit)
        every { style.layers } returns layers
        every { layerApple.id } returns "layerApple"
        every { layerBanana.id } returns "layerBanana"
        every { layerCantaloupe.id } returns "layerCantaloupe"
        every { layerDragonfruit.id } returns "layerDragonfruit"

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer("layerBanana", style)

        assertEquals("layerBanana", result)
    }

    @Test
    fun getBelowLayerReturnsShadowLayerIfInputNotNullOrEmptyAndNotFound() {
        val style = mockk<Style>()
        val layerApple = mockk<Layer>()
        val layerBanana = mockk<Layer>()
        val layerCantaloupe = mockk<Layer>()
        val layerDragonfruit = mockk<Layer>()
        val layers = listOf(layerApple, layerBanana, layerCantaloupe, layerDragonfruit)
        every { style.layers } returns layers
        every { layerApple.id } returns "layerApple"
        every { layerBanana.id } returns "layerBanana"
        every { layerCantaloupe.id } returns "layerCantaloupe"
        every { layerDragonfruit.id } returns "layerDragonfruit"

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer("foobar", style)

        assertEquals(LocationComponentConstants.SHADOW_LAYER, result)
    }

    @Test
    fun generateFeatureCollectionContainsRoute() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.generateFeatureCollection(route)

        assertEquals(route, result.route)
    }

    @Test
    fun generateFeatureLineStringContainsCorrectCoordinates() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.generateFeatureCollection(route)

        assertEquals(4, result.lineString.coordinates().size)
    }

    @Test
    fun generateFeatureFeatureCollectionContainsCorrectFeatures() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.generateFeatureCollection(route)

        assertEquals(1, result.featureCollection.features()!!.size)
    }

    @Test
    fun buildRouteLineExpression() {
        val expectedExpression = "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.2, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.31436133, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.92972755, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 1.0003215, [\"rgba\", 0.0, 7.0, 255.0, 0.0]]"
        val route = getDirectionsRoute(true)
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val expression = MapRouteLine.MapRouteLineSupport.buildRouteLineExpression(
            route,
            lineString,
            true,
            .2) { _, _ -> 2047 }

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun buildRouteLineExpressionMultiLeg() {
        val expectedExpression = "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.2, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.20224349, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.60784495, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.60878944, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.61067015, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6156201, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.61646795, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6179003, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6259702, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.62744135, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.62851965, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.62889737, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6293098, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6322208, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.63238966, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.6330752, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.7178918, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.7254863, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.7288398, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.7349327, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.76355195, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.77338606, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.81858283, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8187517, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8194124, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8216627, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8224529, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8235312, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.82419235, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8265749, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8357827, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.83612746, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.84998137, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8503432, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8695798, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.87023586, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.87054545, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.87165487, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8722659, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.87362933, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8744832, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8995805, [\"rgba\", 0.0, 7.0, 255.0, 0.0], 0.8999844, [\"rgba\", 0.0, 7.0, 255.0, 0.0]]"
        val route = getMultiLegDirectionsRoute()
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val expression = MapRouteLine.MapRouteLineSupport.buildRouteLineExpression(
            route,
            lineString,
            true,
            .2) { _, _ -> 2047 }

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun buildRouteLineExpressionWhenNoTraffic() {
        val expectedExpression = "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.2, [\"rgba\", 0.0, 7.0, 255.0, 0.0]]"
        val route = getDirectionsRoute(false)
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val expression = MapRouteLine.MapRouteLineSupport.buildRouteLineExpression(
            route,
            lineString,
            true,
            .2) { _, _ -> 2047 }

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getStopsFromCongestionReturnsCorrectNumStops() {
        val route = getDirectionsRoute(true)
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val result = MapRouteLine.MapRouteLineSupport.getStopsFromCongestion(
            route.legs()!![0].annotation()!!.congestion()!!.toList(),
            0.0,
            lineString,
            route.distance()!!,
            true
        ) { _, _ -> 1 }

        assertEquals(4, result.size)
    }

    @Test
    fun buildWayPointFeatureCollection() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.buildWayPointFeatureCollection(route)

        assertEquals(2, result.features()!!.size)
    }

    @Test
    fun buildWayPointFeatureCollectionFirstFeatureOrigin() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.buildWayPointFeatureCollection(route)

        assertEquals("{\"wayPoint\":\"origin\"}", result.features()!![0].properties().toString())
    }

    @Test
    fun buildWayPointFeatureCollectionSecondFeatureOrigin() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.buildWayPointFeatureCollection(route)

        assertEquals("{\"wayPoint\":\"destination\"}", result.features()!![1].properties().toString())
    }

    @Test
    fun buildWayPointFeatureFromLeg() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(route.legs()!![0], 0)

        assertEquals(-122.523514, (result!!.geometry() as Point).coordinates()[0], 0.0)
        assertEquals(37.975355, (result.geometry() as Point).coordinates()[1], 0.0)
    }

    @Test
    fun buildWayPointFeatureFromLegContainsOriginWaypoint() {
        val route = getDirectionsRoute(true)

        val result = MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(route.legs()!![0], 0)

        assertEquals("\"origin\"", result!!.properties()!!["wayPoint"].toString())
    }

    private fun getDirectionsRoute(includeCongestion: Boolean): DirectionsRoute {
        val congestion = when (includeCongestion) {
            true -> "\"unknown\",\"heavy\",\"low\""
            false -> ""
        }
        val tokenHere = "someToken"
        val directionsRouteAsJson = "{\"routeIndex\":\"0\",\"distance\":66.9,\"duration\":45.0,\"geometry\":\"urylgArvfuhFjJ`CbC{[pAZ\",\"weight\":96.6,\"weight_name\":\"routability\",\"legs\":[{\"distance\":66.9,\"duration\":45.0,\"summary\":\"Laurel Place, Lincoln Avenue\",\"steps\":[{\"distance\":21.0,\"duration\":16.7,\"geometry\":\"urylgArvfuhFjJ`C\",\"name\":\"\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523514,37.975355],\"bearing_before\":0.0,\"bearing_after\":196.0,\"instruction\":\"Head south\",\"type\":\"depart\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":21.0,\"announcement\":\"Head south, then turn left onto Laurel Place\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead south, then turn left onto Laurel Place\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":18.9,\"announcement\":\"Turn left onto Laurel Place, then turn right onto Lincoln Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Laurel Place, then turn right onto Lincoln Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":21.0,\"primary\":{\"text\":\"Laurel Place\",\"components\":[{\"text\":\"Laurel Place\",\"type\":\"text\",\"abbr\":\"Laurel Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":18.9,\"primary\":{\"text\":\"Laurel Place\",\"components\":[{\"text\":\"Laurel Place\",\"type\":\"text\",\"abbr\":\"Laurel Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"Lincoln Avenue\",\"components\":[{\"text\":\"Lincoln Avenue\",\"type\":\"text\",\"abbr\":\"Lincoln Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":52.6,\"intersections\":[{\"location\":[-122.523514,37.975355],\"bearings\":[196],\"entry\":[true],\"out\":0}]},{\"distance\":41.2,\"duration\":27.3,\"geometry\":\"igylgAtzfuhFbC{[\",\"name\":\"Laurel Place\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523579,37.975173],\"bearing_before\":195.0,\"bearing_after\":99.0,\"instruction\":\"Turn left onto Laurel Place\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":22.6,\"announcement\":\"Turn right onto Lincoln Avenue, then you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Lincoln Avenue, then you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":41.2,\"primary\":{\"text\":\"Lincoln Avenue\",\"components\":[{\"text\":\"Lincoln Avenue\",\"type\":\"text\",\"abbr\":\"Lincoln Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":43.0,\"intersections\":[{\"location\":[-122.523579,37.975173],\"bearings\":[15,105,285],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":4.7,\"duration\":1.0,\"geometry\":\"ecylgAx}euhFpAZ\",\"name\":\"Lincoln Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523117,37.975107],\"bearing_before\":99.0,\"bearing_after\":194.0,\"instruction\":\"Turn right onto Lincoln Avenue\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":4.7,\"announcement\":\"You have arrived at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":4.7,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"straight\"}}],\"driving_side\":\"right\",\"weight\":1.0,\"intersections\":[{\"location\":[-122.523117,37.975107],\"bearings\":[15,105,195,285],\"entry\":[true,true,true,false],\"in\":3,\"out\":2}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"s`ylgAt~euhF\",\"name\":\"Lincoln Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523131,37.975066],\"bearing_before\":195.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination\",\"type\":\"arrive\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.523131,37.975066],\"bearings\":[15],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[21.030105037432428,41.16669115760234,4.722589365163041],\"congestion\":[$congestion]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-122.5237559,37.9754094],[-122.5231475,37.9750697]],\"alternatives\":true,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"$tokenHere\",\"uuid\":\"ck9g2sbdk6pod7ynuece0r2yo\"},\"voiceLocale\":\"en-US\"}"
        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    private fun getMultiLegDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = "{\"routeIndex\":\"0\",\"distance\":22421.1,\"duration\":1390.1,\"geometry\":\"}pw~hAdjd|qCLtAhExn@vAnS|Bjb@`Dzr@xAt_@nA~l@z@te@b@ri@r@~~A|CfaH^lo@Zji@jAf{BRb_@@pWMj[w@~k@yAxj@sBjh@mHp`BkO`dDcFhcAwJxbBeEps@mFvt@_BjViFvu@{Ghx@yDnb@eKjgAeMrmA{K~cAmQn}AqHjl@sFne@eJlt@}Gpk@oGvc@uFd]eGn[}FrWsIf]sWv|@wRtr@m]~jAia@~uA}Ntg@wOxn@ej@jdCaKbg@gDhQ}B~LaDtR_ExXyKh_AcFlf@yEzd@qB~W}ArU}Cvf@wDvo@eCxPwCxMgFhGeEdEoFjAgIkAgEeB}CmCiBsD_CiIaBiR]{IbAoJpBuGxG}GrFcBvFRvMvCdh@jj@xf@hg@xFhIdGlMdFnQBjJaAnI_ErI}FvFoDdAeDPiEMiDaAyGiFaDqGuAsFk@uPhDmv@|Dco@dJw`BxDwi@~Cs\\\\lJwy@dGqd@~DeXjD_S~Gi^pFiWjPsu@vIu^xQu|@za@wvAfRio@tPkl@zNse@|c@cwA|W_z@zTsr@vHwU|CsJna@evAbHcY|DaRfGc]vFk]vCsXbCe\\\\jAqV~AyYvG{yA`Cwi@bGysAlCqq@jCqk@fA}SpCap@pG{yAfDcy@jFevAxFkrA~L{cDdHm~AfCgs@`Aea@P}Gf@oh@BaCFac@U{m@gAwzB]ui@uDwqI_AsfBiAk_AiBicAmBip@{Du~@mAuT{Ic}AqI}|AyDqk@qDgd@uDua@kJaq@oHaf@kMst@gL_j@yJqd@cPsp@kRkr@gN{d@cMca@}Ke\\\\yKyZ{IqUwS}h@wXeq@mfCicG}i@grAufA{oC_vAahDqbAqhCgEkMyBwJgAaJe@yJNaIr@uHhByGlCoGbHoHxEwCdHqC~Do@tLiB|JkMu@aG}@aHS_BeAoH{BgPo@eDyCqOc@wBwGmb@mEkZuAmLqAuMo@eHm@eJa@gIQ{Ck@iMEyBWcKWiMaA_e@OkGe@cRA{@OyJ]cVCeAIgGCg@oAua@mIxK_FlE{EhEwFfDkBz@o@X{An@gAd@oDtAwElAk@BeIViGTqL^wCJeDJi[z@iGPcABsGPeKVwLTen@v@aa@xAsZzBqJx@}MdCy@Nye@`O_D}Pc@mEUuCWsCIkCCiEDkRf@y`@LwNTyHV_El@_ExCsP|A_JlAuHn@qFv@mIt@gKnA}U`Fg`AjBya@bAaT`CyZlBsQfCeR|BaPdCeP~CeRvCgR|@eIf@yGd@aHV{HDeG?oJg@wQwAeSqAwSy@sKqAuU_BySu@qGaAmF{@_DmBkGmD}HiG_JdBaEz@cDz@uCp@aEXuCNiDF}EGyFQmOuFy}@eFqu@mG_dAaMfEyFlCaDdBcDvBiDjCaGxGsDnEeChDgCxDcChEuLwK_KkJsF_FiE{CmDoB}CoA{Cs@iDUkHE@ei@Adi@jHDhDTzCr@|CnAlDnBhEzCrF~E~JjJtLvKbCiEfCyDdCiDrDoE`GyGhDkCbDwB`DeBxFmC`MgElG~cAdFpu@tFx}@PlOFxFG|EOhDYtCq@`E{@tC{@bDeB`EhG~IlD|HlBjGz@~C`AlFt@pG~AxSpAtUx@rKpAvSvAdSf@vQ?nJEdGWzHe@`Hg@xG}@dIwCfR_DdReCdP}B`PgCdRmBrQaCxZcA`TkBxa@aFf`AoA|Uu@fKw@lIo@pFmAtH}A~IyCrPm@~DW~DUxHMvNg@x`@EjRBhEHjCVrCTtCb@lE~C|Pxe@aOx@O|MeCpJy@rZ{B`a@yAdn@w@vLUdKWrGQbAChGQh[{@dDKvCKpL_@hGUdIWj@CvEmAnDuAfAe@zAo@n@YjB{@vFgDzEiE~EmElIyKpQk]rIkPlA{B~C}F`FiJ^y@p@uAxGeN`AkBtRw^bA}AdJqNfCaDdBwB`EmDfEyDnNsHfBy@rEwBfBy@xIiEf[eNlD}AdCiAtWoLno@sYzq@}ZjFaCrd@mSlSiJxQkIfGkCjEiBtCgA`Bm@vBk@rBi@pDw@h@oDY{Ck@qCm@q@gBqAeBmAkCmC}@i@mHsEcTsMwg@i]lIcSlBuDjBwDvBgEdBoCrDwEtBiCpCsCfDmDtCkCvAmAdW}QtYsSxo@we@|WgRxIsF`I{CRGzC{@`D_A`GeBrRuFjUeHfJgDzOmG|MsG``@_UdS{OpFwEpPePdJgJpDqDnKeK`CuBzAkA~B_BVOxD{BzBuAfBs@dC{@|G{BbF}AjG}@nDe@vHs@pFQnHD`I^fD^vHlAdEdAdGdBnXrL~c@lSdZnMvQnIvR~I~E`C`E`BjGvBrCdArCt@~D|@vE~@`BL|BVfDTrEV`F?dEAlBGhCSzJiA`Ew@tCm@zBk@bDgAhGaCdEqB`Bu@lO_HbAe@rPaI|ReJxR{IhSiJ~IgEpJkEfDkAjFaBxHiBbLaB`DQlJy@pNKlN]tFUhXcA|Oe@tF_@`Gc@lEYdMiBzKkBlEeA|DcA`GwAnFiBzJmDbHqClHiDvKqFnKqGrJeGnp@md@bUqPfLcJdAy@rLgKxOgNjJqInNoMnVoU\",\"weight\":1802.6,\"weight_name\":\"routability\",\"legs\":[{\"distance\":16265.7,\"duration\":882.7,\"summary\":\"Capital Beltway (Local), Birchwood Drive\",\"steps\":[{\"distance\":4835.7,\"duration\":183.20000000000002,\"geometry\":\"}pw~hAdjd|qCLtAhExn@vAnS|Bjb@`Dzr@xAt_@nA~l@z@te@b@ri@r@~~A|CfaH^lo@Zji@jAf{BRb_@@pWMj[w@~k@yAxj@sBjh@mHp`BkO`dDcFhcAwJxbBeEps@mFvt@_BjViFvu@{Ghx@yDnb@eKjgAeMrmA{K~cAmQn}AqHjl@sFne@eJlt@}Gpk@oGvc@uFd]eGn[}FrWsIf]sWv|@wRtr@m]~jAia@~uA}Ntg@wOxn@ej@jdCaKbg@gDhQ}B~LaDtR_ExXyKh_AcFlf@yEzd@qB~W}ArU}Cvf@wDvo@\",\"name\":\"Capital Beltway (Local) (I-95; I-495)\",\"ref\":\"I-95; I-495\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.023923,38.793503],\"bearing_before\":0.0,\"bearing_after\":258.0,\"instruction\":\"Head west on I-95\",\"type\":\"depart\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":4835.7,\"announcement\":\"Head west on Capital Beltway (Local) (I-95) for 3 miles\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead west on Capital Beltway (Local) (\\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e) for 3 miles\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":3219.0,\"announcement\":\"In 2 miles, take exit 176A towards VA 611 South\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 2 miles, take exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e176A\\u003c/say-as\\u003e towards VA \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e611\\u003c/say-as\\u003e South\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":804.0,\"announcement\":\"In a half mile, take exit 176A towards VA 611 South\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, take exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e176A\\u003c/say-as\\u003e towards VA \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e611\\u003c/say-as\\u003e South\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.0,\"announcement\":\"Take exit 176A towards VA 611 South, then merge left onto VA 241\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTake exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e176A\\u003c/say-as\\u003e towards VA \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e611\\u003c/say-as\\u003e South, then merge left onto VA \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e241\\u003c/say-as\\u003e\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":4835.7,\"primary\":{\"text\":\"Exit 176A VA 611 South\",\"components\":[{\"text\":\"Exit\",\"type\":\"exit\"},{\"text\":\"176A\",\"type\":\"exit-number\"},{\"text\":\"VA 611 South\",\"type\":\"text\",\"abbr\":\"VA 611 S\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":659.9,\"primary\":{\"text\":\"Exit 176A VA 611 South\",\"components\":[{\"text\":\"Exit\",\"type\":\"exit\"},{\"text\":\"176A\",\"type\":\"exit-number\"},{\"text\":\"VA 611 South\",\"type\":\"text\",\"abbr\":\"VA 611 S\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"},\"sub\":{\"text\":\"VA 241\",\"components\":[{\"text\":\"VA 241\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/va-241\"}],\"type\":\"merge\",\"modifier\":\"slight left\"}}],\"driving_side\":\"right\",\"weight\":201.3,\"intersections\":[{\"location\":[-77.023923,38.793503],\"bearings\":[258],\"classes\":[\"motorway\"],\"entry\":[true],\"out\":0},{\"location\":[-77.025059,38.793351],\"bearings\":[75,255],\"classes\":[\"motorway\"],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-77.036648,38.792939],\"bearings\":[90,270],\"classes\":[\"motorway\"],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-77.046282,38.793441],\"bearings\":[90,285],\"classes\":[\"motorway\"],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-77.047375,38.793555],\"bearings\":[98,279,284],\"classes\":[\"motorway\"],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-77.048972,38.793743],\"bearings\":[105,285],\"classes\":[\"tunnel\",\"motorway\"],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-77.049813,38.793842],\"bearings\":[105,285],\"classes\":[\"motorway\"],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-77.051047,38.794009],\"bearings\":[100,280,284],\"classes\":[\"motorway\"],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-77.064743,38.796962],\"bearings\":[105,120,300],\"classes\":[\"motorway\"],\"entry\":[false,false,true],\"in\":1,\"out\":2},{\"location\":[-77.071728,38.799528],\"bearings\":[120,285,300],\"classes\":[\"motorway\"],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":307.6,\"duration\":22.6,\"geometry\":\"wue_iAtan_rCeCxPwCxMgFhGeEdEoFjAgIkAgEeB}CmCiBsD_CiIaBiR]{IbAoJpBuGxG}GrFcBvFRvMvC\",\"name\":\"\",\"destinations\":\"VA 611 South\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.078059,38.800748],\"bearing_before\":278.0,\"bearing_after\":285.0,\"instruction\":\"Take exit 176A towards VA 611 South\",\"type\":\"off ramp\",\"modifier\":\"slight right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":307.6,\"announcement\":\"Merge left onto VA 241, then take the ramp towards I-95 North: Baltimore\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eMerge left onto VA \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e241\\u003c/say-as\\u003e, then take the ramp towards \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e North: Baltimore\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":307.6,\"primary\":{\"text\":\"VA 241\",\"components\":[{\"text\":\"VA 241\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/va-241\"}],\"type\":\"merge\",\"modifier\":\"slight left\"},\"sub\":{\"text\":\"I-95 North / I-495 East\",\"components\":[{\"text\":\"I-95\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-95\"},{\"text\":\"North\",\"type\":\"text\",\"abbr\":\"N\",\"abbr_priority\":0},{\"text\":\"/\",\"type\":\"delimiter\"},{\"text\":\"I-495\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-495\"},{\"text\":\"East\",\"type\":\"text\",\"abbr\":\"E\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":30.4,\"intersections\":[{\"location\":[-77.078059,38.800748],\"bearings\":[99,279,286],\"classes\":[\"motorway\"],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-77.07763,38.801659],\"bearings\":[30,135,300],\"classes\":[\"motorway\"],\"entry\":[true,true,false],\"in\":2,\"out\":1}],\"exits\":\"176A\"},{\"distance\":185.1,\"duration\":8.9,\"geometry\":\"wgf_iAd`m_rCdh@jj@xf@hg@\",\"name\":\"Telegraph Road (VA 241)\",\"ref\":\"VA 241\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.077523,38.801036],\"bearing_before\":192.0,\"bearing_after\":219.0,\"instruction\":\"Merge left onto VA 241\",\"type\":\"merge\",\"modifier\":\"slight left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":165.1,\"announcement\":\"Take the ramp towards I-95 North: Baltimore\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTake the ramp towards \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e North: Baltimore\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":185.1,\"primary\":{\"text\":\"I-95 North / I-495 East\",\"components\":[{\"text\":\"I-95\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-95\"},{\"text\":\"North\",\"type\":\"text\",\"abbr\":\"N\",\"abbr_priority\":0},{\"text\":\"/\",\"type\":\"delimiter\"},{\"text\":\"I-495\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-495\"},{\"text\":\"East\",\"type\":\"text\",\"abbr\":\"E\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"},\"secondary\":{\"text\":\"Baltimore\",\"components\":[{\"text\":\"Baltimore\",\"type\":\"text\"}],\"type\":\"off ramp\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":14.6,\"intersections\":[{\"location\":[-77.077523,38.801036],\"bearings\":[15,30,225],\"classes\":[\"motorway\"],\"entry\":[false,false,true],\"in\":0,\"out\":2}]},{\"distance\":1143.1,\"duration\":59.800000000000004,\"geometry\":\"wvc_iAzso_rCxFhIdGlMdFnQBjJaAnI_ErI}FvFoDdAeDPiEMiDaAyGiFaDqGuAsFk@uPhDmv@|Dco@dJw`BxDwi@~Cs\\\\lJwy@dGqd@~DeXjD_S~Gi^pFiWjPsu@vIu^xQu|@\",\"name\":\"\",\"destinations\":\"I-95 North, I-495 East: Baltimore\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.078862,38.79974],\"bearing_before\":217.0,\"bearing_after\":225.0,\"instruction\":\"Take the ramp towards I-95 North: Baltimore\",\"type\":\"off ramp\",\"modifier\":\"slight right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":1098.4,\"announcement\":\"In a half mile, merge left onto I-95\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, merge left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.0,\"announcement\":\"Merge left onto I-95\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eMerge left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1143.1,\"primary\":{\"text\":\"I-95 / I-495 East\",\"components\":[{\"text\":\"I-95\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-95\"},{\"text\":\"/\",\"type\":\"delimiter\"},{\"text\":\"I-495\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/i-495\"},{\"text\":\"East\",\"type\":\"text\",\"abbr\":\"E\",\"abbr_priority\":0}],\"type\":\"merge\",\"modifier\":\"slight left\"}}],\"driving_side\":\"right\",\"weight\":65.5,\"intersections\":[{\"location\":[-77.078862,38.79974],\"bearings\":[38,218,225],\"classes\":[\"motorway\"],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-77.074224,38.799552],\"bearings\":[106,279,284],\"classes\":[\"motorway\"],\"entry\":[true,false,false],\"in\":2,\"out\":0}]},{\"distance\":6420.299999999999,\"duration\":250.2,\"geometry\":\"}w`_iArs}~qCza@wvAfRio@tPkl@zNse@|c@cwA|W_z@zTsr@vHwU|CsJna@evAbHcY|DaRfGc]vFk]vCsXbCe\\\\jAqV~AyYvG{yA`Cwi@bGysAlCqq@jCqk@fA}SpCap@pG{yAfDcy@jFevAxFkrA~L{cDdHm~AfCgs@`Aea@P}Gf@oh@BaCFac@U{m@gAwzB]ui@uDwqI_AsfBiAk_AiBicAmBip@{Du~@mAuT{Ic}AqI}|AyDqk@qDgd@uDua@kJaq@oHaf@kMst@gL_j@yJqd@cPsp@kRkr@gN{d@cMca@}Ke\\\\yKyZ{IqUwS}h@wXeq@mfCicG}i@grAufA{oC_vAahD\",\"name\":\"Capital Beltway (Local) (I-95; I-495 East)\",\"ref\":\"I-95; I-495 East\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.069642,38.798223],\"bearing_before\":111.0,\"bearing_after\":116.0,\"instruction\":\"Merge left onto I-95\",\"type\":\"merge\",\"modifier\":\"slight left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":6400.3,\"announcement\":\"Continue on I-95 for 4 miles\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eI-95\\u003c/say-as\\u003e for 4 miles\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":3219.0,\"announcement\":\"In 2 miles, take exit 3B towards Indian Head\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 2 miles, take exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3B\\u003c/say-as\\u003e towards Indian Head\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":804.0,\"announcement\":\"In a half mile, take exit 3B towards Indian Head\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, take exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3B\\u003c/say-as\\u003e towards Indian Head\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.0,\"announcement\":\"Take exit 3B towards Indian Head, then turn left onto MD 414\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTake exit \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3B\\u003c/say-as\\u003e towards Indian Head, then turn left onto Oxon Hill Road (MD \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e414\\u003c/say-as\\u003e)\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":6420.299999999999,\"primary\":{\"text\":\"Exit 3B Indian Head\",\"components\":[{\"text\":\"Exit\",\"type\":\"exit\"},{\"text\":\"3B\",\"type\":\"exit-number\"},{\"text\":\"Indian Head\",\"type\":\"text\"}],\"type\":\"off ramp\",\"modifier\":\"right\"},\"secondary\":{\"text\":\"Forest Heights\",\"components\":[{\"text\":\"Forest Heights\",\"type\":\"text\",\"abbr\":\"Forest Hts\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":641.5,\"primary\":{\"text\":\"Exit 3B Indian Head\",\"components\":[{\"text\":\"Exit\",\"type\":\"exit\"},{\"text\":\"3B\",\"type\":\"exit-number\"},{\"text\":\"Indian Head\",\"type\":\"text\"}],\"type\":\"off ramp\",\"modifier\":\"right\"},\"secondary\":{\"text\":\"Forest Heights\",\"components\":[{\"text\":\"Forest Heights\",\"type\":\"text\",\"abbr\":\"Forest Hts\",\"abbr_priority\":0}],\"type\":\"off ramp\",\"modifier\":\"right\"},\"sub\":{\"text\":\"Oxon Hill Road MD 414\",\"components\":[{\"text\":\"Oxon Hill Road\",\"type\":\"text\",\"abbr\":\"Oxon Hill Rd\",\"abbr_priority\":0},{\"text\":\"MD 414\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/md-414\"}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":268.20000000000005,\"intersections\":[{\"location\":[-77.069642,38.798223],\"bearings\":[120,285,300],\"classes\":[\"motorway\"],\"entry\":[true,false,false],\"in\":1,\"out\":0},{\"location\":[-77.062391,38.795245],\"bearings\":[117,121,299],\"classes\":[\"motorway\"],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-77.05227,38.793456],\"bearings\":[97,274,277],\"classes\":[\"motorway\"],\"entry\":[true,false,false],\"in\":2,\"out\":0},{\"location\":[-77.050031,38.793246],\"bearings\":[97,272,277],\"classes\":[\"tunnel\",\"motorway\"],\"entry\":[true,false,false],\"in\":2,\"out\":0},{\"location\":[-77.049101,38.793162],\"bearings\":[90,270],\"classes\":[\"motorway\"],\"entry\":[true,false],\"in\":1,\"out\":0},{\"location\":[-77.046372,38.792919],\"bearings\":[90,270],\"classes\":[\"motorway\"],\"entry\":[true,false],\"in\":1,\"out\":0},{\"location\":[-77.036645,38.792459],\"bearings\":[90,270],\"classes\":[\"motorway\"],\"entry\":[true,false],\"in\":1,\"out\":0},{\"location\":[-77.024953,38.792836],\"bearings\":[75,270],\"classes\":[\"motorway\"],\"entry\":[true,false],\"in\":1,\"out\":0},{\"location\":[-77.024606,38.792875],\"bearings\":[75,90,255],\"classes\":[\"motorway\"],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-77.019733,38.793491],\"bearings\":[74,77,258],\"classes\":[\"motorway\"],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-77.00648,38.799007],\"bearings\":[57,234,236],\"classes\":[\"motorway\"],\"entry\":[true,false,false],\"in\":2,\"out\":0}]},{\"distance\":470.9,\"duration\":60.0,\"geometry\":\"qrh_iAxzuzqCqbAqhCgEkMyBwJgAaJe@yJNaIr@uHhByGlCoGbHoHxEwCdHqC~Do@tLiB|JkM\",\"name\":\"\",\"destinations\":\"Indian Head, Forest Heights\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-77.000125,38.802233],\"bearing_before\":56.0,\"bearing_after\":57.0,\"instruction\":\"Take exit 3B towards Indian Head\",\"type\":\"off ramp\",\"modifier\":\"slight right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":470.9,\"announcement\":\"Turn left onto MD 414\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Oxon Hill Road (MD \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e414\\u003c/say-as\\u003e)\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":470.9,\"primary\":{\"text\":\"Oxon Hill Road MD 414\",\"components\":[{\"text\":\"Oxon Hill Road\",\"type\":\"text\",\"abbr\":\"Oxon Hill Rd\",\"abbr_priority\":0},{\"text\":\"MD 414\",\"type\":\"icon\",\"imageBaseURL\":\"https://s3.amazonaws.com/mapbox/shields/v3/md-414\"}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":104.6,\"intersections\":[{\"location\":[-77.000125,38.802233],\"bearings\":[56,58,237],\"classes\":[\"motorway\"],\"entry\":[true,true,false],\"in\":2,\"out\":0}],\"exits\":\"3B\"},{\"distance\":555.4,\"duration\":71.9,\"geometry\":\"_ai_iAdumzqCu@aG}@aHS_BeAoH{BgPo@eDyCqOc@wBwGmb@mEkZuAmLqAuMo@eHm@eJa@gIQ{Ck@iMEyBWcKWiMaA_e@OkGe@cRA{@OyJ]cVCeAIgGCg@oAua@\",\"name\":\"Oxon Hill Road (MD 414)\",\"ref\":\"MD 414\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.995939,38.802464],\"bearing_before\":161.0,\"bearing_after\":74.0,\"instruction\":\"Turn left onto MD 414\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":535.4,\"announcement\":\"In a quarter mile, make a sharp left onto Livingston Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, make a sharp left onto Livingston Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":115.9,\"announcement\":\"Make a sharp left onto Livingston Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eMake a sharp left onto Livingston Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":555.4,\"primary\":{\"text\":\"Livingston Road\",\"components\":[{\"text\":\"Livingston Road\",\"type\":\"text\",\"abbr\":\"Livingston Rd\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"sharp left\"}}],\"driving_side\":\"right\",\"weight\":128.6,\"intersections\":[{\"location\":[-76.995939,38.802464],\"bearings\":[75,254,342,345],\"entry\":[true,true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.995189,38.802629],\"bearings\":[75,165,255],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":true,\"indications\":[\"left\"]},{\"valid\":true,\"indications\":[\"straight\"]},{\"valid\":true,\"indications\":[\"straight\"]},{\"valid\":false,\"indications\":[\"right\"]}]}]},{\"distance\":639.9,\"duration\":51.5,\"geometry\":\"cwj_iAbmazqCmIxK_FlE{EhEwFfDkBz@o@X{An@gAd@oDtAwElAk@BeIViGTqL^wCJeDJi[z@iGPcABsGPeKVwLTen@v@aa@xAsZzBqJx@}MdCy@Nye@`O\",\"name\":\"Livingston Road\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.989666,38.80333],\"bearing_before\":84.0,\"bearing_after\":319.0,\"instruction\":\"Make a sharp left onto Livingston Road\",\"type\":\"turn\",\"modifier\":\"sharp left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":619.9,\"announcement\":\"In a half mile, turn right onto Birchwood Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, turn right onto Birchwood Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":186.4,\"announcement\":\"Turn right onto Birchwood Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Birchwood Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":639.9,\"primary\":{\"text\":\"Birchwood Drive\",\"components\":[{\"text\":\"Birchwood Drive\",\"type\":\"text\",\"abbr\":\"Birchwood Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":63.7,\"intersections\":[{\"location\":[-76.989666,38.80333],\"bearings\":[75,135,270,315],\"entry\":[true,true,false,true],\"in\":2,\"out\":3},{\"location\":[-76.990526,38.807366],\"bearings\":[0,180,270],\"entry\":[true,false,true],\"in\":1,\"out\":0}]},{\"distance\":1015.3,\"duration\":90.6,\"geometry\":\"cru_iAh}czqC_D}Pc@mEUuCWsCIkCCiEDkRf@y`@LwNTyHV_El@_ExCsP|A_JlAuHn@qFv@mIt@gKnA}U`Fg`AjBya@bAaT`CyZlBsQfCeR|BaPdCeP~CeRvCgR|@eIf@yGd@aHV{HDeG?oJg@wQwAeSqAwSy@sKqAuU_BySu@qGaAmF{@_DmBkGmD}HiG_J\",\"name\":\"Birchwood Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.990949,38.808882],\"bearing_before\":341.0,\"bearing_after\":70.0,\"instruction\":\"Turn right onto Birchwood Drive\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":995.3,\"announcement\":\"In a half mile, turn right onto Fenwood Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, turn right onto Fenwood Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":168.1,\"announcement\":\"Turn right onto Fenwood Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Fenwood Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1015.3,\"primary\":{\"text\":\"Fenwood Avenue\",\"components\":[{\"text\":\"Fenwood Avenue\",\"type\":\"text\",\"abbr\":\"Fenwood Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":97.9,\"intersections\":[{\"location\":[-76.990949,38.808882],\"bearings\":[75,165,330],\"entry\":[true,false,true],\"in\":1,\"out\":0},{\"location\":[-76.988787,38.808934],\"bearings\":[30,105,285],\"entry\":[true,true,false],\"in\":2,\"out\":1},{\"location\":[-76.987323,38.808652],\"bearings\":[105,180,285],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.984335,38.808263],\"bearings\":[15,105,195,285],\"entry\":[true,true,true,false],\"in\":3,\"out\":1},{\"location\":[-76.98348,38.808053],\"bearings\":[105,195,285],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.981093,38.808026],\"bearings\":[75,255,345],\"entry\":[true,false,true],\"in\":1,\"out\":0}]},{\"distance\":353.3,\"duration\":34.7,\"geometry\":\"_yt_iAnwmyqCdBaEz@cDz@uCp@aEXuCNiDF}EGyFQmOuFy}@eFqu@mG_dA\",\"name\":\"Fenwood Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.979592,38.80848],\"bearing_before\":45.0,\"bearing_after\":123.0,\"instruction\":\"Turn right onto Fenwood Avenue\",\"type\":\"end of road\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":333.3,\"announcement\":\"In a quarter mile, turn left onto Ironton Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn left onto Ironton Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":152.7,\"announcement\":\"Turn left onto Ironton Drive, then turn right onto Bari Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Ironton Drive, then turn right onto Bari Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":353.3,\"primary\":{\"text\":\"Ironton Drive\",\"components\":[{\"text\":\"Ironton Drive\",\"type\":\"text\",\"abbr\":\"Ironton Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":152.7,\"primary\":{\"text\":\"Ironton Drive\",\"components\":[{\"text\":\"Ironton Drive\",\"type\":\"text\",\"abbr\":\"Ironton Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"Bari Drive\",\"components\":[{\"text\":\"Bari Drive\",\"type\":\"text\",\"abbr\":\"Bari Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":73.3,\"intersections\":[{\"location\":[-76.979592,38.80848],\"bearings\":[120,225,315],\"entry\":[true,false,true],\"in\":1,\"out\":0},{\"location\":[-76.978582,38.808332],\"bearings\":[75,270,345],\"entry\":[true,false,true],\"in\":1,\"out\":0},{\"location\":[-76.977577,38.808455],\"bearings\":[75,255,345],\"entry\":[true,false,true],\"in\":1,\"out\":0}]},{\"distance\":138.7,\"duration\":15.2,\"geometry\":\"agu_iA~}eyqCaMfEyFlCaDdBcDvBiDjCaGxGsDnEeChDgCxDcChE\",\"name\":\"Ironton Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.9756,38.808705],\"bearing_before\":80.0,\"bearing_after\":340.0,\"instruction\":\"Turn left onto Ironton Drive\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":136.9,\"announcement\":\"Turn right onto Bari Drive, then turn right onto Jarvis Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Bari Drive, then turn right onto Jarvis Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":138.7,\"primary\":{\"text\":\"Bari Drive\",\"components\":[{\"text\":\"Bari Drive\",\"type\":\"text\",\"abbr\":\"Bari Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":136.9,\"primary\":{\"text\":\"Bari Drive\",\"components\":[{\"text\":\"Bari Drive\",\"type\":\"text\",\"abbr\":\"Bari Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"},\"sub\":{\"text\":\"Jarvis Avenue\",\"components\":[{\"text\":\"Jarvis Avenue\",\"type\":\"text\",\"abbr\":\"Jarvis Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":26.1,\"intersections\":[{\"location\":[-76.9756,38.808705],\"bearings\":[75,255,345],\"entry\":[true,false,true],\"in\":1,\"out\":2},{\"location\":[-76.975952,38.809303],\"bearings\":[60,150,315],\"entry\":[true,false,true],\"in\":1,\"out\":2}]},{\"distance\":141.9,\"duration\":26.4,\"geometry\":\"ufw_iAvtgyqCuLwK_KkJsF_FiE{CmDoB}CoA{Cs@iDUkHE\",\"name\":\"Bari Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.976476,38.809723],\"bearing_before\":309.0,\"bearing_after\":35.0,\"instruction\":\"Turn right onto Bari Drive\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":80.6,\"announcement\":\"Turn right onto Jarvis Avenue, then you will arrive at your 1st destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Jarvis Avenue, then you will arrive at your 1st destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":141.9,\"primary\":{\"text\":\"Jarvis Avenue\",\"components\":[{\"text\":\"Jarvis Avenue\",\"type\":\"text\",\"abbr\":\"Jarvis Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":38.6,\"intersections\":[{\"location\":[-76.976476,38.809723],\"bearings\":[30,135,300],\"entry\":[true,false,true],\"in\":1,\"out\":0}]},{\"distance\":58.5,\"duration\":7.7,\"geometry\":\"gly_iAfhfyqC@ei@\",\"name\":\"Jarvis Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.975764,38.810836],\"bearing_before\":0.0,\"bearing_after\":90.0,\"instruction\":\"Turn right onto Jarvis Avenue\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":38.0,\"announcement\":\"You have arrived at your 1st destination, on the right\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your 1st destination, on the right\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":58.5,\"primary\":{\"text\":\"You will arrive\",\"components\":[{\"text\":\"You will arrive\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":38.0,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":7.7,\"intersections\":[{\"location\":[-76.975764,38.810836],\"bearings\":[90,180,270],\"entry\":[true,false,true],\"in\":1,\"out\":0}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"ely_iA`~dyqC\",\"name\":\"Jarvis Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.975089,38.810835],\"bearing_before\":90.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your 1st destination, on the right\",\"type\":\"arrive\",\"modifier\":\"right\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-76.975089,38.810835],\"bearings\":[270],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[3.8081450180262246,67.26323712146393,28.852710144382463,49.56493474732002,72.5154427377666,45.61491412502117,63.95966403014442,53.76553494509318,59.15734880122501,133.1892637230834,402.690656659677,67.20943111208027,58.79743195676057,172.39450125357757,44.57332036638997,34.06995113588629,39.36565126096354,62.49552703239257,60.976480407894435,57.75114705174457,136.36330730000677,230.79809854930664,95.59766717425671,140.01513962232286,73.73351132690556,75.71946413061276,32.858760518908454,77.04747587663296,81.04848886716198,50.31458505337458,102.70288896716468,111.93980803074744,98.40929873439896,135.11795767012714,65.19603385191276,55.09716035486898,76.74583831368798,63.82220968023617,53.16961512458349,44.04853427862207,42.129382255498854,36.960622672579156,46.020486566306595,96.20709302239581,79.84204175142389,118.51313945647817,135.23673756387257,63.15844998282344,72.70522836125463,200.31452344383752,59.64745348532789,27.062127574453175,20.642720237787806,28.753065868579167,37.35847925463512,92.06475197144877,56.147145599464835,53.91041939069277,35.24776335354998,31.811431872086356,55.82569101453264,68.3819442686242,25.8038678972355,22.2147679826415,17.302551576710194,13.960400113437768,13.747593269306657,18.53612459361029,11.968952243687704,10.727780360510454,9.778087285526292,15.975913642730806,27.33323532388164,15.174448878661321,16.391466085197766,13.614841904050458,19.989971010400964,14.244939904924516,13.819273358364102,27.063450659345694,94.82362255218868,90.25441766925692,19.946673021863994,24.764069396950486,28.669855956118713,15.777992011123352,15.018286228942445,18.198063061335983,17.750268081443515,10.247336631652072,9.264688195896696,11.250231603684863,9.877512016398146,18.676496910568524,14.906246467854642,11.606507626570668,24.652907144075442,77.4665459254835,67.57690481673538,137.02613925396395,60.186760633352336,42.04027494162325,83.98612678288143,54.096014063990054,36.52894386447676,29.341763995945215,46.28797835269473,36.30665176662603,81.82905745154076,47.93238534063436,91.87499470418744,136.61768189885257,75.25748867905999,70.36704737927396,60.56516272006059,138.78379117715127,93.0923933816819,81.50261579021706,36.010563840729404,18.36287685316595,135.6242049125592,39.708526077602116,28.473519818377877,44.28878715738017,44.33131093682672,36.53428315969813,41.14443923592249,32.954348086046615,37.57134852019187,127.00590185303103,59.73536677067511,118.52436467158924,70.5759597858797,62.29894021200939,29.316163834818763,68.5350124658137,126.96644711436655,81.16231845731866,121.64442875518695,116.47909301474903,230.04566314788912,133.38436066945957,72.86808451126007,47.562388122523316,12.437316532046678,57.606513562242014,5.639375775015331,50.02332320538187,65.03058610541073,171.6969999333871,59.23416495287535,469.9795177087809,143.77923471897802,89.38740152538777,94.9373404991213,68.67279967095097,88.95535178083175,30.393122605516837,131.98395165059878,131.64582587123093,62.67029644992775,52.60756415541485,49.16653371641592,72.32976172561138,56.75796633934874,78.65659803701743,64.13476195801456,56.18167178205332,75.27650585578911,79.16215151938209,59.12946990474101,53.59277527041669,46.572553461874314,44.81096196661896,36.79517088472468,68.89879127282884,83.33939077194134,434.10538454399136,138.45753122768082,238.00989150378643,280.97790283043645,225.50880611620937,22.82896868693441,17.651626927815087,15.856032200100213,16.517999315987375,13.983591973400802,13.742895429822712,13.56910748405372,14.189005280873157,20.911527434501238,13.79782015727432,17.53195563267677,10.878486599568967,24.78798899112034,29.133741808053284,11.577959307549854,13.03295412575757,4.3067361253425425,13.738415041963053,24.89767014162922,7.673684288530096,24.51478426103949,5.57282114143065,51.555109358285044,39.65638441558955,19.23996269921751,20.873832271976,13.018453161320405,15.725042127911447,14.340570989660607,6.83467182906447,19.99975800489662,5.297938693841629,16.86861523085541,19.89431395713224,52.82847770582632,11.649012358760205,26.60780810498116,2.602745141581096,16.40646140034076,32.114558561695176,3.041905817874518,11.455120081285425,1.7477918269755026,48.31205531149178,25.705353447483194,15.326209653479616,15.044408817025532,15.595946758556689,6.544958045483093,2.8975120934263168,5.523150268278654,4.329597422563011,10.473534785714225,12.479025171270289,2.4531115762766564,18.15969914437514,14.823792240662817,24.175917358635452,8.469181477423534,9.246419839322574,50.45256751228712,14.813651833709333,3.7856653400615548,15.369039510387362,21.71405363367242,24.4883535458054,84.01091788551761,60.743682945915566,49.45484835804432,20.729814290201585,27.209975347807152,3.2992475508781935,72.57440440726555,26.418468225184277,9.148957251717773,6.614535113890083,6.551130376636104,6.092470033426347,8.756689801432085,26.870366872738533,46.94225404962486,21.855204111701326,13.662390817579826,8.426882287670239,8.70490239367231,25.89859604297864,16.125188466230103,14.117158174175406,10.821743915181573,14.805521995753347,17.251172317443128,32.11837943303784,91.3547299908277,48.648785720408185,29.452452124580212,39.241067949530056,26.54303844806474,27.662601265306304,24.677465809575065,24.972880919121412,28.05699510598131,28.001729626519907,14.542406184994773,12.421735541560576,12.744062597207778,13.75926609126173,11.359101171362596,15.947883079540844,26.0969626720427,28.419995756155743,29.13460476973739,17.802622365146863,31.79110636765839,29.351754121027387,12.24807277528854,10.947721717448692,7.694946636341754,13.12676463844293,16.83904596737583,21.249282649335093,10.141964071082274,7.851486293453664,7.306847684182453,8.855165779873742,6.659329200566484,7.42072482492241,9.630957637489905,10.84322569122163,22.81690227921075,88.1738387883163,76.73864429674225,96.85728790501398,26.484281476757108,15.204259966476467,10.035275426035474,10.49893815944994,11.233486290857893,18.847165029479292,13.470575497764036,10.478942810913148,11.053289008058817,11.42444766525767,30.099044651368352,26.549505432045688,16.684182253103316,13.111102212239048,10.825667259712793,9.446055078546978,8.963527894540448,9.502183000593043,16.68597092279872,58.50215079780706],\"duration\":[0.2,2.5,1.1,1.8,2.7,1.7,2.4,2.0,2.2,4.9,15.3,2.5,2.2,6.5,1.7,1.3,1.5,2.4,2.2,2.1,5.2,8.5,3.5,5.0,2.7,2.8,1.2,2.9,3.1,1.9,3.9,4.2,3.7,5.1,2.5,2.1,2.9,2.4,2.0,1.7,1.6,1.4,1.7,3.6,2.9,4.4,5.0,2.3,2.7,7.4,2.3,1.0,0.8,1.1,1.4,3.6,2.2,2.0,1.3,1.2,2.2,2.6,1.9,1.6,1.3,1.0,1.0,1.4,0.9,0.8,0.7,1.2,2.0,1.1,1.2,1.0,1.5,1.0,1.0,2.0,4.6,4.3,1.5,1.8,2.1,1.2,1.1,1.3,1.3,0.8,0.7,0.8,0.7,1.4,1.1,0.9,1.8,5.7,3.5,6.8,3.0,2.1,4.1,2.0,1.4,1.1,1.8,1.4,3.1,1.8,3.5,5.0,2.7,2.5,2.2,5.2,3.4,2.9,1.3,0.7,5.5,1.6,1.2,1.8,1.8,1.5,1.7,1.3,1.5,5.2,2.4,4.6,2.8,2.5,1.2,2.7,5.0,3.2,4.7,4.5,9.0,5.2,2.9,1.9,0.5,2.3,0.2,2.0,2.5,6.7,2.3,18.4,5.6,3.5,3.7,2.7,3.5,1.2,4.9,4.9,2.3,2.0,1.8,2.6,2.0,2.8,2.3,2.0,2.7,2.8,2.1,1.9,1.7,1.7,1.4,2.7,3.3,17.4,5.5,9.5,11.2,26.2,2.4,1.8,1.7,1.7,1.6,1.4,1.4,1.6,2.4,1.6,1.9,1.3,2.6,3.0,1.3,1.5,0.5,1.2,2.2,0.6,1.9,0.4,3.9,3.0,1.3,1.4,1.0,1.7,1.5,0.5,1.4,0.4,2.2,2.7,7.0,2.1,2.0,0.2,2.3,6.1,0.6,2.2,0.3,9.2,2.5,1.5,1.5,1.5,0.6,0.3,0.5,0.4,1.0,1.2,0.3,2.0,2.5,1.9,0.7,1.1,6.1,1.0,0.3,1.0,1.2,1.3,4.5,4.1,3.0,1.3,1.7,0.2,4.4,2.6,0.9,0.6,0.6,0.6,0.9,2.6,4.6,2.1,1.3,0.8,0.8,1.9,1.2,1.1,0.8,1.1,1.3,2.4,7.1,3.8,2.3,3.1,2.1,2.2,2.1,2.1,2.4,2.2,1.1,1.0,1.0,1.1,0.9,1.2,2.0,2.2,2.3,1.4,3.9,3.6,1.5,1.4,1.0,1.6,2.1,2.6,1.1,0.9,0.8,1.0,0.7,0.8,1.1,1.2,2.5,6.5,5.2,6.6,2.9,1.7,1.1,1.1,1.2,1.6,1.1,0.9,0.9,1.0,5.2,4.6,2.9,2.2,1.9,1.6,1.5,1.6,2.9,7.7],\"congestion\":[\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"low\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"heavy\",\"low\",\"unknown\",\"heavy\",\"heavy\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"unknown\"]}},{\"distance\":6155.4,\"duration\":507.4,\"summary\":\"Livingston Road, Tucker Road\",\"steps\":[{\"distance\":58.5,\"duration\":13.0,\"geometry\":\"ely_iA`~dyqCAdi@\",\"name\":\"Jarvis Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.975089,38.810835],\"bearing_before\":0.0,\"bearing_after\":270.0,\"instruction\":\"Head west on Jarvis Avenue\",\"type\":\"depart\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":58.5,\"announcement\":\"Head west on Jarvis Avenue, then turn left onto Bari Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead west on Jarvis Avenue, then turn left onto Bari Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":58.5,\"primary\":{\"text\":\"Bari Drive\",\"components\":[{\"text\":\"Bari Drive\",\"type\":\"text\",\"abbr\":\"Bari Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"Ironton Drive\",\"components\":[{\"text\":\"Ironton Drive\",\"type\":\"text\",\"abbr\":\"Ironton Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":44.8,\"intersections\":[{\"location\":[-76.975089,38.810835],\"bearings\":[270],\"entry\":[true],\"out\":0}]},{\"distance\":141.9,\"duration\":29.3,\"geometry\":\"gly_iAfhfyqCjHDhDTzCr@|CnAlDnBhEzCrF~E~JjJtLvK\",\"name\":\"Bari Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.975764,38.810836],\"bearing_before\":270.0,\"bearing_after\":180.0,\"instruction\":\"Turn left onto Bari Drive\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":72.6,\"announcement\":\"Turn left onto Ironton Drive, then turn right onto Fenwood Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Ironton Drive, then turn right onto Fenwood Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":141.9,\"primary\":{\"text\":\"Ironton Drive\",\"components\":[{\"text\":\"Ironton Drive\",\"type\":\"text\",\"abbr\":\"Ironton Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":72.6,\"primary\":{\"text\":\"Ironton Drive\",\"components\":[{\"text\":\"Ironton Drive\",\"type\":\"text\",\"abbr\":\"Ironton Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"Fenwood Avenue\",\"components\":[{\"text\":\"Fenwood Avenue\",\"type\":\"text\",\"abbr\":\"Fenwood Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":59.0,\"intersections\":[{\"location\":[-76.975764,38.810836],\"bearings\":[90,180,270],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":138.7,\"duration\":16.7,\"geometry\":\"ufw_iAvtgyqCbCiEfCyDdCiDrDoE`GyGhDkCbDwB`DeBxFmC`MgE\",\"name\":\"Ironton Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.976476,38.809723],\"bearing_before\":215.0,\"bearing_after\":129.0,\"instruction\":\"Turn left onto Ironton Drive\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":124.6,\"announcement\":\"Turn right onto Fenwood Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Fenwood Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":138.7,\"primary\":{\"text\":\"Fenwood Avenue\",\"components\":[{\"text\":\"Fenwood Avenue\",\"type\":\"text\",\"abbr\":\"Fenwood Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":36.4,\"intersections\":[{\"location\":[-76.976476,38.809723],\"bearings\":[30,135,300],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.975952,38.809303],\"bearings\":[60,150,315],\"entry\":[true,true,false],\"in\":2,\"out\":1}]},{\"distance\":353.3,\"duration\":32.3,\"geometry\":\"agu_iA~}eyqClG~cAdFpu@tFx}@PlOFxFG|EOhDYtCq@`E{@tC{@bDeB`E\",\"name\":\"Fenwood Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.9756,38.808705],\"bearing_before\":160.0,\"bearing_after\":260.0,\"instruction\":\"Turn right onto Fenwood Avenue\",\"type\":\"end of road\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":333.3,\"announcement\":\"In a quarter mile, turn left onto Birchwood Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn left onto Birchwood Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":164.1,\"announcement\":\"Turn left onto Birchwood Drive\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Birchwood Drive\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":353.3,\"primary\":{\"text\":\"Birchwood Drive\",\"components\":[{\"text\":\"Birchwood Drive\",\"type\":\"text\",\"abbr\":\"Birchwood Dr\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":55.7,\"intersections\":[{\"location\":[-76.9756,38.808705],\"bearings\":[75,255,345],\"entry\":[true,true,false],\"in\":2,\"out\":1},{\"location\":[-76.977577,38.808455],\"bearings\":[75,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.978582,38.808332],\"bearings\":[75,270,345],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":1015.3,\"duration\":95.0,\"geometry\":\"_yt_iAnwmyqChG~IlD|HlBjGz@~C`AlFt@pG~AxSpAtUx@rKpAvSvAdSf@vQ?nJEdGWzHe@`Hg@xG}@dIwCfR_DdReCdP}B`PgCdRmBrQaCxZcA`TkBxa@aFf`AoA|Uu@fKw@lIo@pFmAtH}A~IyCrPm@~DW~DUxHMvNg@x`@EjRBhEHjCVrCTtCb@lE~C|P\",\"name\":\"Birchwood Drive\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.979592,38.80848],\"bearing_before\":303.0,\"bearing_after\":225.0,\"instruction\":\"Turn left onto Birchwood Drive\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":995.3,\"announcement\":\"In a half mile, turn left onto Livingston Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, turn left onto Livingston Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":160.3,\"announcement\":\"Turn left onto Livingston Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Livingston Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1015.3,\"primary\":{\"text\":\"Livingston Road\",\"components\":[{\"text\":\"Livingston Road\",\"type\":\"text\",\"abbr\":\"Livingston Rd\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":126.4,\"intersections\":[{\"location\":[-76.979592,38.80848],\"bearings\":[120,225,315],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.981093,38.808026],\"bearings\":[75,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.98348,38.808053],\"bearings\":[105,195,285],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-76.984335,38.808263],\"bearings\":[15,105,195,285],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-76.987323,38.808652],\"bearings\":[105,180,285],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-76.988787,38.808934],\"bearings\":[30,105,285],\"entry\":[true,false,true],\"in\":1,\"out\":2}]},{\"distance\":1561.5,\"duration\":137.5,\"geometry\":\"cru_iAh}czqCxe@aOx@O|MeCpJy@rZ{B`a@yAdn@w@vLUdKWrGQbAChGQh[{@dDKvCKpL_@hGUdIWj@CvEmAnDuAfAe@zAo@n@YjB{@vFgDzEiE~EmElIyKpQk]rIkPlA{B~C}F`FiJ^y@p@uAxGeN`AkBtRw^bA}AdJqNfCaDdBwB`EmDfEyDnNsHfBy@rEwBfBy@xIiEf[eNlD}AdCiAtWoLno@sYzq@}ZjFaCrd@mSlSiJxQkIfGkCjEiBtCgA`Bm@vBk@rBi@pDw@\",\"name\":\"Livingston Road\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.990949,38.808882],\"bearing_before\":250.0,\"bearing_after\":161.0,\"instruction\":\"Turn left onto Livingston Road\",\"type\":\"end of road\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":1541.5,\"announcement\":\"Continue on Livingston Road for 1 mile\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on Livingston Road for 1 mile\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":794.9,\"announcement\":\"In a half mile, turn left onto Saint Barnabas Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, turn left onto Saint Barnabas Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":170.3,\"announcement\":\"Turn left onto Saint Barnabas Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Saint Barnabas Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1561.5,\"primary\":{\"text\":\"Saint Barnabas Road\",\"components\":[{\"text\":\"Saint\",\"type\":\"text\",\"abbr\":\"St\",\"abbr_priority\":0},{\"text\":\"Barnabas Road\",\"type\":\"text\",\"abbr\":\"Barnabas Rd\",\"abbr_priority\":1}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":159.8,\"intersections\":[{\"location\":[-76.990949,38.808882],\"bearings\":[75,165,330],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.990526,38.807366],\"bearings\":[0,180,270],\"entry\":[false,true,true],\"in\":0,\"out\":1},{\"location\":[-76.989666,38.80333],\"bearings\":[75,135,270,315],\"entry\":[true,true,true,false],\"in\":3,\"out\":1},{\"location\":[-76.987278,38.80182],\"bearings\":[105,150,240,315],\"entry\":[false,true,true,false],\"in\":3,\"out\":1},{\"location\":[-76.987038,38.801572],\"bearings\":[60,150,330],\"entry\":[true,true,false],\"in\":2,\"out\":1},{\"location\":[-76.986375,38.800402],\"bearings\":[150,240,330],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.986122,38.79994],\"bearings\":[150,255,330],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.985696,38.799164],\"bearings\":[150,255,330],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.984328,38.796644],\"bearings\":[120,165,255,345],\"entry\":[false,true,true,false],\"in\":3,\"out\":1}]},{\"distance\":197.9,\"duration\":21.9,\"geometry\":\"ih}~hA`{vyqCh@oDY{Ck@qCm@q@gBqAeBmAkCmC}@i@mHsEcTsMwg@i]\",\"name\":\"Saint Barnabas Road\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.984257,38.796437],\"bearing_before\":164.0,\"bearing_after\":106.0,\"instruction\":\"Turn left onto Saint Barnabas Road\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":177.9,\"announcement\":\"In 600 feet, turn right onto Tucker Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 600 feet, turn right onto Tucker Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":135.5,\"announcement\":\"Turn right onto Tucker Road\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Tucker Road\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":197.9,\"primary\":{\"text\":\"Tucker Road\",\"components\":[{\"text\":\"Tucker Road\",\"type\":\"text\",\"abbr\":\"Tucker Rd\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":37.2,\"intersections\":[{\"location\":[-76.984257,38.796437],\"bearings\":[75,105,165,345],\"entry\":[false,true,true,false],\"in\":3,\"out\":1},{\"location\":[-76.983952,38.796526],\"bearings\":[30,210,255],\"entry\":[true,false,true],\"in\":1,\"out\":0},{\"location\":[-76.983913,38.796577],\"bearings\":[45,210,240],\"entry\":[true,false,true],\"in\":1,\"out\":0},{\"location\":[-76.983842,38.796647],\"bearings\":[30,210,225],\"entry\":[true,false,false],\"in\":1,\"out\":0}]},{\"distance\":2688.3,\"duration\":161.7,\"geometry\":\"u~__iAfltyqClIcSlBuDjBwDvBgEdBoCrDwEtBiCpCsCfDmDtCkCvAmAdW}QtYsSxo@we@|WgRxIsF`I{CRGzC{@`D_A`GeBrRuFjUeHfJgDzOmG|MsG``@_UdS{OpFwEpPePdJgJpDqDnKeK`CuBzAkA~B_BVOxD{BzBuAfBs@dC{@|G{BbF}AjG}@nDe@vHs@pFQnHD`I^fD^vHlAdEdAdGdBnXrL~c@lSdZnMvQnIvR~I~E`C`E`BjGvBrCdArCt@~D|@vE~@`BL|BVfDTrEV`F?dEAlBGhCSzJiA`Ew@tCm@zBk@bDgAhGaCdEqB`Bu@lO_HbAe@rPaI|ReJxR{IhSiJ~IgEpJkEfDkAjFaBxHiBbLaB`DQlJy@pNKlN]tFUhXcA|Oe@tF_@`Gc@lEYdMiBzKkBlEeA|DcA`GwAnFiBzJmDbHqClHiDvKqFnKqGrJeGnp@md@bUqPfLcJdAy@rLgKxOgNjJqInNoMnVoU\",\"name\":\"Tucker Road\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.982996,38.797819],\"bearing_before\":29.0,\"bearing_after\":122.0,\"instruction\":\"Turn right onto Tucker Road\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":2668.3,\"announcement\":\"Continue on Tucker Road for 1.5 miles\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on Tucker Road for 1.5 miles\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":1163.8,\"announcement\":\"In a half mile, you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a half mile, you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":83.1,\"announcement\":\"You have arrived at your destination, on the left\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination, on the left\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":2688.3,\"primary\":{\"text\":\"You will arrive\",\"components\":[{\"text\":\"You will arrive\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":83.1,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":162.8,\"intersections\":[{\"location\":[-76.982996,38.797819],\"bearings\":[30,120,210],\"entry\":[true,true,false],\"in\":2,\"out\":1},{\"location\":[-76.980189,38.79484],\"bearings\":[165,240,330],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.979505,38.793259],\"bearings\":[75,150,345],\"entry\":[true,true,false],\"in\":2,\"out\":1},{\"location\":[-76.977745,38.791111],\"bearings\":[150,240,330],\"entry\":[true,true,false],\"in\":2,\"out\":0},{\"location\":[-76.977502,38.782867],\"bearings\":[75,165,240,345],\"entry\":[true,true,true,false],\"in\":3,\"out\":1}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"gqu}hAn_cyqC\",\"name\":\"Tucker Road\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-76.974088,38.7761],\"bearing_before\":143.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination, on the left\",\"type\":\"arrive\",\"modifier\":\"left\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-76.974088,38.7761],\"bearings\":[323],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[58.50215079780706,16.68597092279872,9.502183000593043,8.963527894540448,9.446055078546978,10.825667259712793,13.111102212239048,16.684182253103316,26.549505432045688,30.099044651368352,11.42444766525767,11.053289008058817,10.478942810913148,13.470575497764036,18.847165029479292,11.233486290857893,10.49893815944994,10.035275426035474,15.204259966476467,26.484281476757108,96.85728790501398,76.73864429674225,88.1738387883163,22.81690227921075,10.84322569122163,9.630957637489905,7.42072482492241,6.659329200566484,8.855165779873742,7.306847684182453,7.851486293453664,10.141964071082274,21.249282649335093,16.83904596737583,13.12676463844293,7.694946636341754,10.947721717448692,12.24807277528854,29.351754121027387,31.79110636765839,17.802622365146863,29.13460476973739,28.419995756155743,26.0969626720427,15.947883079540844,11.359101171362596,13.75926609126173,12.744062597207778,12.421735541560576,14.542406184994773,28.001729626519907,28.05699510598131,24.972880919121412,24.677465809575065,27.662601265306304,26.54303844806474,39.241067949530056,29.452452124580212,48.648785720408185,91.3547299908277,32.11837943303784,17.251172317443128,14.805521995753347,10.821743915181573,14.117158174175406,16.125188466230103,25.89859604297864,8.70490239367231,8.426882287670239,13.662390817579826,21.855204111701326,46.94225404962486,26.870366872738533,8.756689801432085,6.092470033426347,6.551130376636104,6.614535113890083,9.148957251717773,26.418468225184277,72.57440440726555,3.2992475508781935,27.209975347807152,20.729814290201585,49.45484835804432,60.743682945915566,84.01091788551761,24.4883535458054,21.71405363367242,15.369039510387362,3.7856653400615548,14.813651833709333,50.45256751228712,9.246419839322574,8.469181477423534,24.175917358635452,14.823792240662817,18.15969914437514,2.4531115762766564,12.479025171270289,10.473534785714225,4.329597422563011,5.523150268278654,2.8975120934263168,6.544958045483093,15.595946758556689,15.044408817025532,15.326209653479616,25.705353447483194,53.53371655095349,30.629856831973314,6.906369563804513,14.154816263870364,20.10258716677716,3.0799014403484284,4.650191608246014,26.260460441623835,5.948229446035404,56.271581896384376,5.558641250962244,29.363809033974803,10.319937843424032,7.695874183548994,13.163266713406689,13.736733447735586,30.644358800114624,6.306420961587649,12.886155511145152,6.306423182638306,21.140190029170146,54.50857395206729,10.499349380134204,8.113024833722724,47.75770782109435,93.8792168501074,98.48131110988655,14.283055691110254,72.71112478582116,39.610992539350185,36.440655769192674,15.886432404826051,12.240073066841774,8.90659535785542,5.803335497475962,6.940729563651378,6.703057409559025,10.192373751411138,7.9780562647966775,6.914480138856293,6.784791227951007,3.3527719023141644,6.7885278381727,6.60359790809776,9.924746818153864,3.8990715839984267,19.144500310927405,42.71782504787377,83.82524262335663,33.52828417904585,9.982488820070403,9.983829377680381,10.93988582152465,8.43401877853343,13.70606631271884,8.879241963569653,10.34775418150611,12.007048438899764,10.315509124337195,5.94814630420318,50.425618543340654,55.4436061527924,102.15035949277625,51.79206619703744,21.957082297475164,19.141515729585816,1.165062098174828,9.057067718259336,9.426741998367355,15.013906550356023,36.51652002288089,41.80849394799967,21.30393188687317,32.23094216780151,29.15103970189662,66.28113091778313,42.878515388021675,16.39481156008955,39.30927105685296,25.29617400328985,12.550891797213604,27.939889564942952,8.856131854703253,6.085263208511906,8.245554885185529,1.5041531128158707,11.657188449828752,7.83913215530252,6.207469005361203,7.892970408559475,16.789029296832748,13.31840441788646,15.144691507127966,9.925549082475857,17.497099911578726,13.480981053350522,16.908398033479838,17.961077782265505,9.445418964918503,17.6776615274082,11.421833612071099,15.226728076796142,49.15867454054789,71.68964361027767,52.39772806295395,36.40827005764805,38.31685027213371,13.672716967214923,11.595211479186172,15.786034578327222,8.772293269423518,8.557152678960144,11.010784665211851,12.328660594795855,5.483774994311737,7.0840711992499745,9.391557509943066,11.835803010424737,12.568571900877213,11.011745015762726,6.1272686913108965,7.723431830881135,21.37508396173334,11.058695601205613,8.577002803344664,7.154960080042136,9.639842916068448,15.83019655705092,12.069542838151493,5.931559500735012,31.805408782131227,4.124910927353576,34.331712697998924,38.726913853940495,38.350641774958916,39.40803634204987,21.40997624112553,22.39684864452492,9.90691898599266,13.79519226479059,18.05703822940403,23.740756801368878,9.043061263070282,20.509130615356387,27.700234211648105,27.50366349224224,13.714039941852736,45.143010496200326,30.187312719546235,13.750993823399469,14.432825381961742,11.511626762940598,25.66316955418942,23.38612180347382,11.851441549136691,10.970041933274551,14.846741424959191,14.116128030255386,22.439000075956134,17.429028209326503,18.341133194151485,24.998378087850497,25.218311017764474,23.601270668491043,102.26290737437903,46.30331143514812,28.18220060066584,4.63444105225483,29.61033592413834,36.64472621934098,24.99062511961933,34.14057252824154,52.18673854713761],\"duration\":[7.7,2.9,1.6,1.5,1.6,1.9,2.2,2.9,4.6,5.2,1.0,0.9,0.9,1.1,1.6,1.2,1.1,1.1,1.7,2.9,6.6,5.2,6.5,2.5,1.2,1.1,0.8,0.7,1.0,0.8,0.9,1.1,1.9,1.5,1.2,0.7,1.0,1.1,2.6,2.9,2.0,3.3,3.2,2.9,1.8,1.3,1.5,1.4,1.4,1.6,3.2,2.4,2.1,2.1,2.0,1.9,2.8,2.1,3.4,6.4,2.4,1.3,1.1,0.8,1.1,1.2,1.9,0.8,0.8,1.3,2.1,4.6,2.6,0.9,0.6,0.6,0.6,0.9,2.6,4.9,0.2,1.8,1.4,3.4,4.1,5.6,1.6,1.4,1.7,0.4,1.0,6.1,1.1,0.7,1.9,2.5,3.8,0.5,2.1,1.8,0.7,0.7,0.4,0.8,2.0,1.9,2.0,3.3,5.2,3.0,0.5,1.1,1.9,0.3,0.4,1.7,0.4,3.7,0.4,1.9,0.7,1.0,1.7,1.8,1.9,0.4,0.7,0.4,1.2,3.1,0.6,0.4,2.6,5.2,5.6,1.0,5.0,2.7,2.5,1.1,5.5,4.0,0.9,1.1,1.1,1.7,1.7,1.5,1.4,0.7,1.4,0.7,1.1,0.4,1.5,3.1,6.0,3.4,1.0,0.6,0.6,0.5,0.8,0.5,0.6,0.7,0.6,0.3,2.8,3.1,5.7,2.9,1.2,1.2,0.1,0.6,0.6,0.9,2.3,2.6,1.3,2.0,1.6,3.7,2.4,0.9,2.2,1.4,0.7,1.5,0.5,0.3,0.5,0.1,0.6,0.4,0.3,0.4,0.9,0.7,0.8,0.6,1.0,0.8,1.0,1.0,0.5,1.0,0.6,0.9,2.8,4.0,2.9,2.0,2.3,0.8,0.7,0.9,0.5,0.5,0.7,0.7,0.3,0.4,0.6,0.7,0.8,0.7,0.4,0.5,1.3,0.7,0.5,0.4,0.6,0.9,0.7,0.4,1.9,0.2,2.1,2.3,2.3,2.4,1.3,1.3,0.6,0.8,1.1,1.5,0.6,1.3,1.7,1.7,0.9,2.9,1.9,0.9,0.9,0.7,1.6,1.5,0.7,0.7,0.9,0.9,1.4,1.1,1.2,1.6,1.6,1.5,6.5,2.9,1.8,0.3,1.9,2.3,1.6,2.2,3.3],\"congestion\":[\"unknown\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"moderate\",\"low\",\"heavy\",\"heavy\",\"unknown\",\"low\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"unknown\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"heavy\",\"moderate\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"unknown\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\"]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-77.0241637,38.7944089],[-76.9750887,38.8107503],[-76.9734033,38.776499]],\"alternatives\":false,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion,distance,duration\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"$tokenHere\",\"uuid\":\"cka2xf9770elc7fnw0z2qx582\"},\"voiceLocale\":\"en-US\"}"
        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
