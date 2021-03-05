package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.common.ShadowValueConverter
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineUpdateValue
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowValueConverter::class])
@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineViewTest {

    lateinit var ctx: Context

    private val layerTypeValue = mockk<Value> {
        every { contents } returns "line"
    }
    private val sourceValue = mockk<Value> {
        every { contents } returns "mapbox-navigation-route-source"
    }
    private val layerValue = mockk<Value> {
        every { contents } returns HashMap<String, Value>().also {
            it["type"] = layerTypeValue
            it["source"] = sourceValue
        }
    }
    private val layerPropertyExpected = mockk<Expected<Value, String>> {
        every { value.hint(Value::class) } returns layerValue
    }
    private val geoJsonSourceTypeValue = mockk<Value> {
        every { contents } returns "geojson"
    }
    private val geoJsonSourceValue = mockk<Value> {
        every { contents } returns HashMap<String, Value>().also {
            it["type"] = geoJsonSourceTypeValue
        }
    }
    private val geoJsonSourceExpected = mockk<Expected<Value, String>> {
        every { value.hint(Value::class) } returns geoJsonSourceValue
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        ctx = ApplicationProvider.getApplicationContext()
    }

    private fun mockCheckForLayerInitialization(style: Style) {
        with(style) {
            every { styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
        }
    }

    @Test
    fun initializeLayers() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleSourceProperties(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.WAYPOINT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).initializeLayers(style)

        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun renderClearRouteDataState() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val altRoutesFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val primarySourceSlot = slot<Value>()
        val alt1SourceSlot = slot<Value>()
        val alt2SourceSlot = slot<Value>()
        val wayPointSourceSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleSourceProperties(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.WAYPOINT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        val state = com.mapbox.navigation.ui.base.model.Expected.Success(
            RouteLineClearValue(
                primaryRouteFeatureCollection,
                altRoutesFeatureCollection,
                altRoutesFeatureCollection,
                waypointsFeatureCollection
            )
        )

        MapboxRouteLineView(options).renderClearRouteLineValue(style, state)

        verify {
            style.setStyleSourceProperty(
                RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                any(),
                capture(primarySourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            primarySourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                any(),
                capture(alt1SourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            alt1SourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                any(),
                capture(alt2SourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            alt2SourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                any(),
                capture(wayPointSourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            wayPointSourceSlot.captured.contents as String
        )
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun renderTraveledRouteLineUpdate() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val state = com.mapbox.navigation.ui.base.model.Expected.Success(
            VanishingRouteLineUpdateValue(
                trafficLineExp,
                routeLineExp,
                casingLineEx
            )
        )
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    "line-gradient",
                    trafficLineExp
                )
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    "line-gradient",
                    routeLineExp
                )
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    "line-gradient",
                    casingLineEx
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).renderVanishingRouteLineUpdateValue(style, state)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                "line-gradient",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                "line-gradient",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                "line-gradient",
                casingLineEx
            )
        }
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun renderDrawRouteState() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val alternativeRoute1FeatureCollection = FeatureCollection.fromFeatures(listOf())
        val alternativeRoute2FeatureCollection = FeatureCollection.fromFeatures(listOf())
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(listOf())
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val alternativeRoute1Expression = mockk<Expression>()
        val alternativeRoute2Expression = mockk<Expression>()
        val primarySourceSlot = slot<Value>()
        val alt1SourceSlot = slot<Value>()
        val alt2SourceSlot = slot<Value>()
        val wayPointSourceSlot = slot<Value>()
        val state = com.mapbox.navigation.ui.base.model.Expected.Success(
            RouteSetValue(
                primaryRouteFeatureCollection,
                trafficLineExp,
                routeLineExp,
                casingLineEx,
                alternativeRoute1Expression,
                alternativeRoute2Expression,
                alternativeRoute1FeatureCollection,
                alternativeRoute2FeatureCollection,
                waypointsFeatureCollection
            )
        )
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true

            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    "line-gradient",
                    trafficLineExp
                )
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    "line-gradient",
                    routeLineExp
                )
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    "line-gradient",
                    casingLineEx
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                    "line-gradient",
                    alternativeRoute1Expression
                )
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                    "line-gradient",
                    alternativeRoute2Expression
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleSourceProperties(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.WAYPOINT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).renderRouteDrawData(style, state)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                "line-gradient",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                "line-gradient",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                "line-gradient",
                casingLineEx
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                "line-gradient",
                alternativeRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                "line-gradient",
                alternativeRoute2Expression
            )
        }
        verify {
            style.setStyleSourceProperty(
                RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                any(),
                capture(primarySourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            primarySourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                any(),
                capture(alt1SourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            alt1SourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                any(),
                capture(alt2SourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            alt2SourceSlot.captured.contents as String
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                any(),
                capture(wayPointSourceSlot)
            )
        }
        assertEquals(
            primaryRouteFeatureCollection.toJson(),
            wayPointSourceSlot.captured.contents as String
        )
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun showPrimaryRoute() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteValueSlot = slot<Value>()
        val trafficValueSlot = slot<Value>()
        val casingValueSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).showPrimaryRoute(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                "visibility",
                capture(primaryRouteValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                "visibility",
                capture(trafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                "visibility",
                capture(casingValueSlot)
            )
        }
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            primaryRouteValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            trafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            casingValueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun hidePrimaryRoute() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteValueSlot = slot<Value>()
        val trafficValueSlot = slot<Value>()
        val casingValueSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).hidePrimaryRoute(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                "visibility",
                capture(primaryRouteValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                "visibility",
                capture(trafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                "visibility",
                capture(casingValueSlot)
            )
        }
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            primaryRouteValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            trafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            casingValueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun showAlternativeRoutes() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val alternativeRoute1ValueSlot = slot<Value>()
        val alternativeRoute1TrafficValueSlot = slot<Value>()
        val alternativeRoute1CasingValueSlot = slot<Value>()
        val alternativeRoute2ValueSlot = slot<Value>()
        val alternativeRoute2TrafficValueSlot = slot<Value>()
        val alternativeRoute2CasingValueSlot = slot<Value>()

        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true

            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).showAlternativeRoutes(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                "visibility",
                capture(alternativeRoute1ValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                "visibility",
                capture(alternativeRoute1TrafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                "visibility",
                capture(alternativeRoute1CasingValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                "visibility",
                capture(alternativeRoute2ValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                "visibility",
                capture(alternativeRoute2TrafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                "visibility",
                capture(alternativeRoute2CasingValueSlot)
            )
        }
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute1ValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute1TrafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute1CasingValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute2ValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute2TrafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            alternativeRoute2CasingValueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun hideAlternativeRoutes() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val alternativeRoute1ValueSlot = slot<Value>()
        val alternativeRoute1TrafficValueSlot = slot<Value>()
        val alternativeRoute1CasingValueSlot = slot<Value>()
        val alternativeRoute2ValueSlot = slot<Value>()
        val alternativeRoute2TrafficValueSlot = slot<Value>()
        val alternativeRoute2CasingValueSlot = slot<Value>()

        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true

            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
            every {
                getStyleLayerProperties(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).hideAlternativeRoutes(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                "visibility",
                capture(alternativeRoute1ValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                "visibility",
                capture(alternativeRoute1TrafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                "visibility",
                capture(alternativeRoute1CasingValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                "visibility",
                capture(alternativeRoute2ValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                "visibility",
                capture(alternativeRoute2TrafficValueSlot)
            )
        }
        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                "visibility",
                capture(alternativeRoute2CasingValueSlot)
            )
        }
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute1ValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute1TrafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute1CasingValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute2ValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute2TrafficValueSlot.captured.contents.toString().toLowerCase()
        )
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            alternativeRoute2CasingValueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun showOriginAndDestinationPoints() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val valueSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.WAYPOINT_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.WAYPOINT_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).showOriginAndDestinationPoints(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.WAYPOINT_LAYER_ID,
                "visibility",
                capture(valueSlot)
            )
        }
        assertEquals(
            Visibility.VISIBLE.value.toLowerCase(),
            valueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun hideOriginAndDestinationPoints() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val valueSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.WAYPOINT_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteConstants.WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.WAYPOINT_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteLineView(options).hideOriginAndDestinationPoints(style)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.WAYPOINT_LAYER_ID,
                "visibility",
                capture(valueSlot)
            )
        }
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            valueSlot.captured.contents.toString().toLowerCase()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun getPrimaryRouteVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style>()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID
            )
        } returns Visibility.VISIBLE

        val result = MapboxRouteLineView(options).getPrimaryRouteVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun getAlternativeRoutesVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style>()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID
            )
        } returns Visibility.VISIBLE

        val result = MapboxRouteLineView(options).getAlternativeRoutesVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }
}
