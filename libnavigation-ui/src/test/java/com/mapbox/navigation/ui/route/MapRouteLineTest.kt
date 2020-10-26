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
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.location.LocationComponentConstants
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.ThemeSwitcher
import com.mapbox.navigation.ui.internal.route.MapRouteSourceProvider
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider
import com.mapbox.turf.TurfMeasurement
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.RobolectricTestRunner
import java.util.Scanner

@RunWith(RobolectricTestRunner::class)
class MapRouteLineTest {

    lateinit var ctx: Context
    var styleRes: Int = 0
    lateinit var wayPointSource: GeoJsonSource
    lateinit var primaryRouteLineSource: GeoJsonSource
    lateinit var primaryRouteCasingSource: GeoJsonSource
    lateinit var primaryRouteLineTrafficSource: GeoJsonSource
    lateinit var alternativeRouteLineSource: GeoJsonSource

    lateinit var mapRouteSourceProvider: MapRouteSourceProvider
    lateinit var layerProvider: RouteLayerProvider
    lateinit var alternativeRouteCasingLayer: LineLayer
    lateinit var alternativeRouteLayer: LineLayer
    lateinit var primaryRouteCasingLayer: LineLayer
    lateinit var primaryRouteLayer: LineLayer
    lateinit var primaryRouteTrafficLayer: LineLayer
    lateinit var waypointLayer: SymbolLayer

    lateinit var style: Style

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )
        alternativeRouteCasingLayer = mockk {
            every { id } returns ALTERNATIVE_ROUTE_CASING_LAYER_ID
        }

        alternativeRouteLayer = mockk {
            every { id } returns ALTERNATIVE_ROUTE_LAYER_ID
        }

        primaryRouteCasingLayer = mockk(relaxUnitFun = true) {
            every { id } returns PRIMARY_ROUTE_CASING_LAYER_ID
        }

        primaryRouteLayer = mockk(relaxUnitFun = true) {
            every { id } returns PRIMARY_ROUTE_LAYER_ID
        }

        waypointLayer = mockk {
            every { id } returns WAYPOINT_LAYER_ID
        }

        primaryRouteTrafficLayer = mockk(relaxUnitFun = true) {
            every { id } returns PRIMARY_ROUTE_TRAFFIC_LAYER_ID
        }

        style = mockk(relaxUnitFun = true) {
            every { getLayer(ALTERNATIVE_ROUTE_LAYER_ID) } returns alternativeRouteLayer
            every { getLayer(ALTERNATIVE_ROUTE_CASING_LAYER_ID) } returns
                alternativeRouteCasingLayer
            every { getLayer(PRIMARY_ROUTE_LAYER_ID) } returns primaryRouteLayer
            every { getLayer(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns primaryRouteTrafficLayer
            every { getLayer(PRIMARY_ROUTE_CASING_LAYER_ID) } returns primaryRouteCasingLayer
            every { getLayer(WAYPOINT_LAYER_ID) } returns waypointLayer
            every { isFullyLoaded } returns false
        }

        wayPointSource = mockk(relaxUnitFun = true)
        primaryRouteLineSource = mockk(relaxUnitFun = true)
        primaryRouteCasingSource = mockk(relaxUnitFun = true)
        primaryRouteLineTrafficSource = mockk(relaxUnitFun = true)
        alternativeRouteLineSource = mockk(relaxUnitFun = true)

        mapRouteSourceProvider = mockk {
            every { build(RouteConstants.WAYPOINT_SOURCE_ID, any(), any()) } returns wayPointSource
            every {
                build(
                    RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                    any(),
                    any()
                )
            } returns primaryRouteLineSource
            every {
                build(
                    RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID,
                    any(),
                    any()
                )
            } returns alternativeRouteLineSource
        }
        layerProvider = mockk {
            every {
                initializeAlternativeRouteCasingLayer(
                    style,
                    -9273715
                )
            } returns alternativeRouteCasingLayer
            every {
                initializeAlternativeRouteLayer(
                    style,
                    true,
                    -7957339
                )
            } returns alternativeRouteLayer
            every {
                initializePrimaryRouteCasingLayer(
                    style,
                    -13665594
                )
            } returns primaryRouteCasingLayer
            every {
                initializePrimaryRouteLayer(
                    style,
                    true,
                    -11097861
                )
            } returns primaryRouteLayer
            every { initializeWayPointLayer(style, any(), any()) } returns waypointLayer
            every {
                initializePrimaryRouteTrafficLayer(
                    style,
                    true,
                    -11097861
                )
            } returns primaryRouteTrafficLayer
        }
    }

    @Test
    fun getStyledColor() {
        val result = MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeColor,
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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute)) }

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(directionsRoutes) }
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
        val firstRouteFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val secondRouteFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val directionsRoutes = listOf(
            RouteFeatureData(firstRoute, firstRouteFeatureCollection, mockk<LineString>()),
            RouteFeatureData(secondRoute, secondRouteFeatureCollection, mockk<LineString>())
        )
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            directionsRoutes,
            listOf(),
            false,
            false,
            mapRouteSourceProvider,
            0.0,
            null
        )

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
            mapRouteSourceProvider,
            null
        )

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
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(directionsRoute, directionsRoute2)) }
        val routeFeatureData = mapRouteLine.retrieveRouteFeatureData()

        assertEquals(routeFeatureData.first().route, directionsRoute)

        mapRouteLine.updatePrimaryRouteIndex(directionsRoute2)

        assertEquals(routeFeatureData.first().route, directionsRoute2)
    }

    @Test
    fun getStyledColorRecyclesAttributes() {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every {
            context.obtainStyledAttributes(
                styleRes,
                R.styleable.MapboxStyleNavigationMapRoute
            )
        } returns typedArray
        every { context.resources } returns resources
        every { context.getColor(R.color.mapbox_navigation_route_layer_blue) } returns 0
        every { resources.getColor(R.color.mapbox_navigation_route_layer_blue) } returns 0
        every {
            typedArray.getColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeColor,
                anyInt()
            )
        } returns 0

        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getFloatStyledValue() {
        val result: Float = MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteScale,
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
        every {
            context.obtainStyledAttributes(
                styleRes,
                R.styleable.MapboxStyleNavigationMapRoute
            )
        } returns typedArray
        every {
            typedArray.getFloat(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteScale,
                1.0f
            )
        } returns 1.0f

        MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteScale,
            1.0f,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getBooleanStyledValue() {
        val result = MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_roundedLineCap,
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
        every {
            context.obtainStyledAttributes(
                styleRes,
                R.styleable.MapboxStyleNavigationMapRoute
            )
        } returns typedArray
        every {
            typedArray.getBoolean(
                R.styleable.MapboxStyleNavigationMapRoute_roundedLineCap,
                true
            )
        } returns true

        MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_roundedLineCap,
            true,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getResourceStyledValue() {
        val result = MapRouteLine.MapRouteLineSupport.getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_originWaypointIcon,
            R.drawable.mapbox_ic_route_origin,
            ctx,
            styleRes
        )

        assertEquals(R.drawable.mapbox_ic_route_origin, result)
    }

    @Test
    fun getResourceStyledValueRecyclesAttributes() {
        val context = mockk<Context>()
        val typedArray = mockk<TypedArray>(relaxUnitFun = true)
        every {
            context.obtainStyledAttributes(
                styleRes,
                R.styleable.MapboxStyleNavigationMapRoute
            )
        } returns typedArray
        every {
            typedArray.getResourceId(
                R.styleable.MapboxStyleNavigationMapRoute_originWaypointIcon,
                R.drawable.mapbox_ic_route_origin
            )
        } returns R.drawable.mapbox_ic_route_origin

        MapRouteLine.MapRouteLineSupport.getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_originWaypointIcon,
            R.drawable.mapbox_ic_route_origin,
            context,
            styleRes
        )

        verify(exactly = 1) { typedArray.recycle() }
    }

    @Test
    fun getBelowLayerWithNullLayerId_1() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<SymbolLayer> {
                every { id } returns RouteConstants.MAPBOX_LOCATION_ID + "1"
            },
            mockk<SymbolLayer> {
                every { id } returns RouteConstants.MAPBOX_LOCATION_ID + "2"
            },
            mockk<FillLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer(null, style)

        assertEquals(RouteConstants.MAPBOX_LOCATION_ID + "1", result)
    }

    @Test
    fun getBelowLayerWithNullLayerId_2() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<CircleLayer> {
                every { id } returns "layerBanana"
            },
            mockk<FillLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer(null, style)

        assertEquals("layerDragonfruit", result)
    }

    @Test
    fun getBelowLayerWithEmptyLayerId() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerBanana"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<FillLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer("", style)

        assertEquals("layerBanana", result)
    }

    @Test
    fun getBelowLayerReturnsShadowLayerIdAsDefault() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<CircleLayer> {
                every { id } returns "layerBanana"
            },
            mockk<FillLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<FillLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer(null, style)

        assertEquals(LocationComponentConstants.SHADOW_LAYER, result)
    }

    @Test
    fun getBelowLayerReturnsInputIdIfFound() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<CircleLayer> {
                every { id } returns "layerBanana"
            },
            mockk<FillLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

        val result = MapRouteLine.MapRouteLineSupport.getBelowLayer("layerBanana", style)

        assertEquals("layerBanana", result)
    }

    @Test
    fun getBelowLayerReturnsShadowLayerIfInputNotNullOrEmptyAndNotFound() {
        val style = mockk<Style>()
        every { style.layers } returns listOf(
            mockk<FillLayer> {
                every { id } returns "layerApple"
            },
            mockk<CircleLayer> {
                every { id } returns "layerBanana"
            },
            mockk<FillLayer> {
                every { id } returns "layerCantaloupe"
            },
            mockk<SymbolLayer> {
                every { id } returns "layerDragonfruit"
            }
        )

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
        every { style.layers } returns listOf(primaryRouteLayer)
        val expectedExpression =
            "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.2, [\"rgba\", " +
                "86.0, 168.0, 251.0, 1.0], 0.31436133, [\"rgba\", 86.0, 168.0, 251.0, 1.0], " +
                "0.92972755, [\"rgba\", 255.0, 77.0, 77.0, 1.0], 1.0003215, [\"rgba\", 86.0, " +
                "168.0, 251.0, 1.0]]"
        val route = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        val expression = mapRouteLine.getExpressionAtOffset(.2)

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun buildRouteLineExpressionMultileg() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val expectedExpression = loadJsonFixture("build_route_line_expression_multileg_text.txt")
        val route = getMultilegRoute()
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        val expression = mapRouteLine.getExpressionAtOffset(0.0)

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun buildRouteLineExpressionWhenNoTraffic() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val expectedExpression =
            "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.2, [\"rgba\", " +
                "86.0, 168.0, 251.0, 1.0]]"
        val route = getDirectionsRoute(false)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        val expression = mapRouteLine.getExpressionAtOffset(.2)

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun buildRouteLineExpressionOffsetAfterLastLeg() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val expectedExpression =
            "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.9, " +
                "[\"rgba\", 86.0, 168.0, 251.0, 1.0]]"
        val route = getDirectionsRoute(false)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        val expression = mapRouteLine.getExpressionAtOffset(.9)

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRoute() {
        val route = getMultilegRoute()
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val result = MapRouteLine.MapRouteLineSupport.calculateRouteLineSegments(
            route,
            lineString,
            true
        ) { _, _ -> 1 }

        assertEquals(21, result.size)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRouteFirstDistanceValueAboveMinimumOffset() {
        val route = getMultilegRoute()
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val result = MapRouteLine.MapRouteLineSupport.calculateRouteLineSegments(
            route,
            lineString,
            true
        ) { _, _ -> 1 }

        assertTrue(result[1].offset > .001f)
    }

    @Test
    fun calculateRouteLineSegmentFromCongestion() {
        val route = getMultilegRoute()
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)

        val result = MapRouteLine.MapRouteLineSupport.calculateRouteLineSegmentsFromCongestion(
            route.legs()!![0].annotation()!!.congestion()!!.toList(),
            lineString,
            route.distance()!!,
            true
        ) { _, _ -> 1 }

        assertEquals(9, result.size)
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

        assertEquals(
            "{\"wayPoint\":\"destination\"}",
            result.features()!![1].properties().toString()
        )
    }

    @Test
    fun buildWayPointFeatureFromLeg() {
        val route = getDirectionsRoute(true)

        val result =
            MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(route.legs()!![0], 0)

        assertEquals(-122.523514, (result!!.geometry() as Point).coordinates()[0], 0.0)
        assertEquals(37.975355, (result.geometry() as Point).coordinates()[1], 0.0)
    }

    @Test
    fun buildWayPointFeatureFromLegContainsOriginWaypoint() {
        val route = getDirectionsRoute(true)

        val result =
            MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(route.legs()!![0], 0)

        assertEquals("\"origin\"", result!!.properties()!!["wayPoint"].toString())
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionModerate() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.MODERATE_CONGESTION_VALUE, true)

        assertEquals(-27392, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionHeavy() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.HEAVY_CONGESTION_VALUE, true)

        assertEquals(-45747, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionSevere() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.SEVERE_CONGESTION_VALUE, true)

        assertEquals(-7396281, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionUnknown() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.UNKNOWN_CONGESTION_VALUE, true)

        assertEquals(-11097861, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionDefault() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result = mapRouteLine.getRouteColorForCongestion("foobar", true)

        assertEquals(-11097861, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionModerate() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.MODERATE_CONGESTION_VALUE, false)

        assertEquals(-4881791, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionHeavy() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.HEAVY_CONGESTION_VALUE, false)

        assertEquals(-4881791, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionSevere() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.SEVERE_CONGESTION_VALUE, false)

        assertEquals(-4881791, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionUnknown() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result =
            mapRouteLine.getRouteColorForCongestion(RouteConstants.UNKNOWN_CONGESTION_VALUE, false)

        assertEquals(-7957339, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionDefault() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        val result = mapRouteLine.getRouteColorForCongestion("foobar", false)

        assertEquals(-7957339, result)
    }

    @Test
    fun reinitializeWithRoutes() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val route = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        mapRouteLine.reinitializeWithRoutes(listOf(route))

        assertEquals(route, mapRouteLine.getPrimaryRoute())
    }

    @Test
    fun reinitializePrimaryRoute() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returns true
        every { style.getLayer(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns primaryRouteLayer
        every { primaryRouteLayer.setFilter(any()) } returns Unit
        every { primaryRouteCasingLayer.setFilter(any()) } returns Unit
        every { alternativeRouteLayer.setFilter(any()) } returns Unit
        every { alternativeRouteCasingLayer.setFilter(any()) } returns Unit
        every { primaryRouteTrafficLayer.setFilter(any()) } returns Unit
        every { waypointLayer.setFilter(any()) } returns Unit
        every { primaryRouteLayer.setProperties(any()) } returns Unit
        every { primaryRouteCasingLayer.setProperties(any()) } returns Unit
        every { alternativeRouteLayer.setProperties(any()) } returns Unit
        every { alternativeRouteCasingLayer.setProperties(any()) } returns Unit
        every { primaryRouteTrafficLayer.setProperties(any()) } returns Unit
        every { waypointLayer.setProperties(any()) } returns Unit
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer

        val route = getDirectionsRoute(true)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        )

        mapRouteLine.reinitializeWithRoutes(listOf(route))
        mapRouteLine.reinitializePrimaryRoute()

        verify { primaryRouteLayer.setProperties(any()) }
    }

    @Test
    fun getExpressionAtOffsetWhenExpressionDataEmpty() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val expectedExpression = "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, 0.0]," +
            " 0.2, [\"rgba\", 86.0, 168.0, 251.0, 1.0]]"
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            listOf<RouteFeatureData>(),
            listOf<RouteLineExpressionData>(),
            true,
            false,
            mapRouteSourceProvider,
            0.0,
            null
        )

        val expression = mapRouteLine.getExpressionAtOffset(.2)

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun updateVanishingPoint() {
        val expectedRouteLineVanishingExpression = "[\"step\", [\"line-progress\"], " +
            "[\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.01792979, [\"rgba\", 86.0, 168.0, 251.0, 1.0]]"
        val expectedRouteLineCasingVanishingExpression = "[\"step\", [\"line-progress\"], " +
            "[\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.01792979, [\"rgba\", 47.0, 122.0, 198.0, 1.0]]"
        val expectedRouteTrafficLineVanishingExpression = "[\"step\", [\"line-progress\"], " +
            "[\"rgba\", 0.0, 0.0, 0.0, 0.0], 0.01792979, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.056129422, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.09373502, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.150941, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.1905395, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.26964808, " +
            "[\"rgba\", 255.0, 77.0, 77.0, 1.0], 0.27944908, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.33272266, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.39298487, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.48991048, " +
            "[\"rgba\", 255.0, 77.0, 77.0, 1.0], 0.504132, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 0.8017851, " +
            "[\"rgba\", 86.0, 168.0, 251.0, 1.0], 1.0000086, [\"rgba\", 86.0, 168.0, 251.0, 1.0]]"
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val routeLineExpressionSlot = slot<PropertyValue<Expression>>()
        val routeLineCasingExpressionSlot = slot<PropertyValue<Expression>>()
        val routeLineTrafficExpressionSlot = slot<PropertyValue<Expression>>()
        val route = getDirectionsRoute()
        val coordinates = LineString.fromPolyline(
            route.geometry()!!,
            Constants.PRECISION_6
        ).coordinates()
        val inputPoint = Point.fromLngLat(-122.524000, 37.975207)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }
        mapRouteLine.inhibitAutomaticVanishingPointUpdate(false)
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![0].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 15f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![0].distance()
                    }
                    every { stepIndex } returns 0
                }
            }
        }
        mapRouteLine.updateUpcomingRoutePointIndex(routeProgress)

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify { primaryRouteCasingLayer.setProperties(capture(routeLineCasingExpressionSlot)) }
        verify { primaryRouteLayer.setProperties(capture(routeLineExpressionSlot)) }
        verify { primaryRouteTrafficLayer.setProperties(capture(routeLineTrafficExpressionSlot)) }
        assertEquals(
            expectedRouteLineVanishingExpression,
            routeLineExpressionSlot.captured.expression.toString()
        )
        assertEquals(
            expectedRouteLineCasingVanishingExpression,
            routeLineCasingExpressionSlot.captured.expression.toString()
        )

        assertEquals(
            expectedRouteTrafficLineVanishingExpression,
            routeLineTrafficExpressionSlot.captured.expression.toString()
        )
    }

    @Test
    fun doNotUpdateVanishingPointWhenInhibited() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val route = getDirectionsRoute()
        val inputPoint = Point.fromLngLat(-122.524000, 37.975207)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }
        mapRouteLine.inhibitAutomaticVanishingPointUpdate(false)
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![0].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 15f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![0].distance()
                    }
                    every { stepIndex } returns 0
                }
            }
        }
        mapRouteLine.updateUpcomingRoutePointIndex(routeProgress)
        mapRouteLine.inhibitAutomaticVanishingPointUpdate(true)

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify(exactly = 0) { primaryRouteCasingLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteTrafficLayer.setProperties(any()) }
    }

    @Test
    fun doNotUpdateVanishingPointWhenRouteProgressOutdated() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val route = getDirectionsRoute()
        val inputPoint = Point.fromLngLat(-122.524000, 37.975207)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }
        mapRouteLine.inhibitAutomaticVanishingPointUpdate(false)
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![0].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 15f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![0].distance()
                    }
                    every { stepIndex } returns 0
                }
            }
        }
        mapRouteLine.updateUpcomingRoutePointIndex(routeProgress)

        Thread.sleep((RouteConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO / 1E6).toLong())

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify(exactly = 0) { primaryRouteCasingLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteTrafficLayer.setProperties(any()) }
    }

    @Test
    fun updateVanishingPointInhibitedByDefault() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val route = getDirectionsRoute()
        val coordinates = LineString.fromPolyline(
            route.geometry()!!,
            Constants.PRECISION_6
        ).coordinates()
        val inputPoint = TurfMeasurement.midpoint(coordinates[4], coordinates[5])
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify(exactly = 0) { primaryRouteCasingLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteTrafficLayer.setProperties(any()) }
    }

    @Test
    fun updateVanishingPointWhenPointDistanceBeyondThreshold() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val route = getDirectionsRoute()
        val inputPoint = Point.fromLngLat(-122.508527, 37.974846)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify(exactly = 0) { primaryRouteCasingLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteTrafficLayer.setProperties(any()) }
    }

    @Test
    fun updateVanishingPointWhenLineCoordinatesIsLessThanTwoPoints() {
        every { style.layers } returns listOf(primaryRouteLayer)
        every { style.isFullyLoaded } returnsMany listOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            true
        )
        every {
            style.getLayerAs<LineLayer>("mapbox-navigation-route-casing-layer")
        } returns primaryRouteCasingLayer
        every { style.getLayer("mapbox-navigation-route-layer") } returns primaryRouteLayer
        every {
            style.getLayer("mapbox-navigation-route-traffic-layer")
        } returns primaryRouteTrafficLayer
        val route = getSingleCoordinateDirectionsRoute()

        val inputPoint = Point.fromLngLat(-122.508527, 37.974846)
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            null
        ).also { it.draw(listOf(route)) }

        mapRouteLine.updateTraveledRouteLine(inputPoint)

        verify(exactly = 0) { primaryRouteCasingLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteLayer.setProperties(any()) }
        verify(exactly = 0) { primaryRouteTrafficLayer.setProperties(any()) }
    }

    private fun getDirectionsRoute(includeCongestion: Boolean): DirectionsRoute {
        val congestionValue = when (includeCongestion) {
            true -> "\"unknown\",\"heavy\",\"low\""
            false -> ""
        }
        val tokenHere = "someToken"
        val directionsRouteAsJson = loadJsonFixture("222.txt")
            ?.replace("tokenHere", tokenHere)
            ?.replace("congestion_value", congestionValue)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = loadJsonFixture("vanish_point_test.txt")
            ?.replace("tokenHere", tokenHere)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    private fun getSingleCoordinateDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = loadJsonFixture("single_coordinate_route.json")
            ?.replace("tokenHere", tokenHere)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    @Test
    fun onInitializedCallback() {
        val callback = mockk<MapRouteLineInitializedCallback>(relaxUnitFun = true)

        every { style.layers } returns listOf(primaryRouteLayer)
        MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteSourceProvider,
            callback
        )

        verify {
            callback.onInitialized(
                RouteLineLayerIds(
                    PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    PRIMARY_ROUTE_LAYER_ID,
                    listOf(ALTERNATIVE_ROUTE_LAYER_ID)
                )
            )
        }
    }

    @Test
    fun getStyledFloatArrayTest() {
        val result = MapRouteLine.MapRouteLineSupport.getStyledFloatArray(
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
            ctx,
            styleRes,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertEquals(6, result.size)
        assertEquals(4.0f, result[0])
        assertEquals(10.0f, result[1])
        assertEquals(13.0f, result[2])
        assertEquals(16.0f, result[3])
        assertEquals(19.0f, result[4])
        assertEquals(22.0f, result[5])
    }

    @Test
    fun getStyledFloatArrayWhenResourceNotFount() {
        val result = MapRouteLine.MapRouteLineSupport.getStyledFloatArray(
            0,
            ctx,
            styleRes,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun getRouteLineScalingValuesTest() {
        val result = MapRouteLine.MapRouteLineSupport.getRouteLineScalingValues(
            styleRes,
            ctx,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleMultipliers,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScales,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertEquals(result.size, 6)
        assertEquals(4.0f, result[0].scaleStop)
        assertEquals(3.0f, result[0].scaleMultiplier)
        assertEquals(1.0f, result[0].scale)
    }

    @Test
    fun keepPrimaryRouteWhenRecreate() {
        every { style.layers } returns listOf(primaryRouteLayer)
        val firstRoute: DirectionsRoute = getDirectionsRoute(true)
        val secondRoute: DirectionsRoute = getDirectionsRoute(false)
        val firstRouteFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val secondRouteFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val directionsRoutes = listOf(
            RouteFeatureData(firstRoute, firstRouteFeatureCollection, mockk<LineString>()),
            RouteFeatureData(secondRoute, secondRouteFeatureCollection, mockk<LineString>())
        )
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            directionsRoutes,
            listOf(),
            false,
            false,
            mapRouteSourceProvider,
            0.0,
            null
        )
        val primaryRouteBeforeRecreate = mapRouteLine.getPrimaryRoute()

        mapRouteLine.updatePrimaryRouteIndex(secondRoute)
        val recreatedMapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            mapRouteLine.retrieveRouteFeatureData(),
            mapRouteLine.retrieveRouteExpressionData(),
            mapRouteLine.retrieveVisibility(),
            mapRouteLine.retrieveAlternativesVisible(),
            mapRouteSourceProvider,
            0.0,
            null
        )

        assertEquals(primaryRouteBeforeRecreate, firstRoute)
        assertEquals(recreatedMapRouteLine.getPrimaryRoute(), secondRoute)
    }

    private fun getMultilegRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("multileg_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun loadJsonFixture(filename: String): String? {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader?.getResourceAsStream(filename)
        val scanner = Scanner(inputStream, "UTF-8").useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }
}
