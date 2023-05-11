package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.observable.eventdata.SourceDataLoadedEventData
import com.mapbox.maps.extension.observable.model.SourceDataType
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.buildScalingExpression
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_VIOLATED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_VIOLATED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_VIOLATED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@ExperimentalCoroutinesApi
class MapboxRouteLineViewTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()
    private val routesExpector = mockk<RoutesExpector>(relaxed = true)
    private val dataIdHolder = mockk<DataIdHolder>(relaxed = true)

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk<Drawable>()
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
    }

    private fun mockCheckForLayerInitialization(style: Style) {
        with(style) {
            every { styleSourceExists(WAYPOINT_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_1_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_2_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_3_SOURCE_ID) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) } returns true
            every {
                styleLayerExists(LAYER_GROUP_1_TRAIL)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_1_CASING)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_1_MAIN)
            } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) } returns true
            every {
                styleLayerExists(LAYER_GROUP_2_TRAIL)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_2_CASING)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_2_MAIN)
            } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) } returns true
            every {
                styleLayerExists(LAYER_GROUP_3_TRAIL)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_3_CASING)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_3_MAIN)
            } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAIL_CASING) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAIL) } returns true
            every { styleLayerExists(MASKING_LAYER_CASING) } returns true
            every { styleLayerExists(MASKING_LAYER_MAIN) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_RESTRICTED) } returns true

            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
            every {
                styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
            every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
            every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
            every { getStyleLayerProperty(any(), any()) } returns mockk(relaxed = true)
        }
    }

    @Test
    fun initializeLayers() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = getMockedStyle().apply {
            every {
                setStyleSourceProperty(LAYER_GROUP_1_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(LAYER_GROUP_2_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(LAYER_GROUP_3_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(WAYPOINT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every { getSource(LAYER_GROUP_1_SOURCE_ID) } returns null
            every { getSource(LAYER_GROUP_2_SOURCE_ID) } returns null
            every { getSource(LAYER_GROUP_3_SOURCE_ID) } returns null
            every { getSource(WAYPOINT_SOURCE_ID) } returns null
            every {
                removeStyleSource(LAYER_GROUP_1_SOURCE_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleSource(LAYER_GROUP_2_SOURCE_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleSource(LAYER_GROUP_3_SOURCE_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleSource(WAYPOINT_SOURCE_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleSource(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleSource(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_TRAIL)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_MAIN)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_TRAFFIC)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_1_RESTRICTED)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_TRAIL)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_MAIN)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_TRAFFIC)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_2_RESTRICTED)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_TRAIL)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_CASING)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_MAIN)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_TRAFFIC)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(LAYER_GROUP_3_RESTRICTED)
            } returns ExpectedFactory.createNone()
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).initializeLayers(style)

        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun renderClearRouteLineValue() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val altRoutesFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
        val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
        val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
        val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
        val primaryDataId = 2
        val altDataId1 = 4
        val altDataId2 = 7
        every { dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID) } returns primaryDataId
        every { dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID) } returns altDataId1
        every { dataIdHolder.incrementDataId(LAYER_GROUP_3_SOURCE_ID) } returns altDataId2
        val topLevelRouteLayer = StyleObjectInfo(
            TOP_LEVEL_ROUTE_LINE_LAYER_ID,
            "background"
        )
        val bottomLevelRouteLayer = StyleObjectInfo(
            BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
            "background"
        )
        val mainLayer = StyleObjectInfo(
            LAYER_GROUP_1_MAIN,
            "line"
        )
        val style = getMockedStyle().apply {
            every { getSource(LAYER_GROUP_1_SOURCE_ID) } returns primaryRouteSource
            every { getSource(LAYER_GROUP_2_SOURCE_ID) } returns altRoute1Source
            every { getSource(LAYER_GROUP_3_SOURCE_ID) } returns altRoute2Source
            every { getSource(WAYPOINT_SOURCE_ID) } returns wayPointSource
            every { styleLayers } returns listOf(
                bottomLevelRouteLayer,
                mainLayer,
                topLevelRouteLayer
            )
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val state: Expected<RouteLineError, RouteLineClearValue> = ExpectedFactory.createValue(
            RouteLineClearValue(
                primaryRouteFeatureCollection,
                listOf(altRoutesFeatureCollection, altRoutesFeatureCollection),
                waypointsFeatureCollection
            )
        )

        MapboxRouteLineView(options, routesExpector, dataIdHolder).renderClearRouteLineValue(
            style,
            state
        )
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        verify {
            primaryRouteSource.featureCollection(
                primaryRouteFeatureCollection,
                primaryDataId.toString()
            )
        }
        verify {
            altRoute1Source.featureCollection(
                altRoutesFeatureCollection,
                altDataId1.toString()
            )
        }
        verify {
            altRoute2Source.featureCollection(
                altRoutesFeatureCollection,
                altDataId2.toString()
            )
        }
        verify { wayPointSource.featureCollection(waypointsFeatureCollection) }
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun renderClearRouteLineValue_noInitializeRepeat() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val altRoutesFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
        val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
        val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
        val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
        val primaryDataId = 2
        val altDataId1 = 4
        val altDataId2 = 7
        every { dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID) } returns primaryDataId
        every { dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID) } returns altDataId1
        every { dataIdHolder.incrementDataId(LAYER_GROUP_3_SOURCE_ID) } returns altDataId2
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } answers {
            style.apply {
                every { styleSourceExists(any()) } returns true
                every { getSource(LAYER_GROUP_1_SOURCE_ID) } returns primaryRouteSource
                every { getSource(LAYER_GROUP_2_SOURCE_ID) } returns altRoute1Source
                every { getSource(LAYER_GROUP_3_SOURCE_ID) } returns altRoute2Source
                every { getSource(WAYPOINT_SOURCE_ID) } returns wayPointSource
            }
            every { MapboxRouteLineUtils.layersAreInitialized(style, options) } returns true
        }

        val state: Expected<RouteLineError, RouteLineClearValue> = ExpectedFactory.createValue(
            RouteLineClearValue(
                primaryRouteFeatureCollection,
                listOf(altRoutesFeatureCollection, altRoutesFeatureCollection),
                waypointsFeatureCollection
            )
        )

        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder)
        view.renderClearRouteLineValue(style, state)
        view.renderClearRouteLineValue(style, state)

        verify(exactly = 1) { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExpression = mockk<Expression>()
        val trailCasingExpression = mockk<Expression>()
        val violatedSectionExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExpression },
                        trailCasingExpressionProvider = { trailCasingExpression },
                        violatedSectionExpressionProvider = { violatedSectionExpression },
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                        ),
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                        )
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExpression },
                        trailCasingExpressionProvider = { trailCasingExpression },
                        violatedSectionExpressionProvider = { violatedSectionExpression },
                    )
                )
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val view = MapboxRouteLineView(options)
        view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
        view.renderRouteLineUpdate(style, state)

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-gradient",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                casingLineEx
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                restrictedRoadExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_VIOLATED,
                "line-gradient",
                violatedSectionExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                trailExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                casingLineEx
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                trailExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate_whenIgnorePrimaryRouteLineData() {
            mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
            mockkObject(MapboxRouteLineUtils)
            val options = MapboxRouteLineOptions.Builder(ctx).build()
            val trafficLineExp = mockk<Expression>()
            val routeLineExp = mockk<Expression>()
            val casingLineEx = mockk<Expression>()
            val restrictedRoadExp = mockk<Expression>()
            val trailExpression = mockk<Expression>()
            val trailCasingExpression = mockk<Expression>()
            val violatedSectionExpression = mockk<Expression>()
            val state: Expected<RouteLineError, RouteLineUpdateValue> =
                ExpectedFactory.createValue(
                    RouteLineUpdateValue(
                        primaryRouteLineDynamicData = RouteLineDynamicData(
                            { routeLineExp },
                            { casingLineEx },
                            { trafficLineExp },
                            { restrictedRoadExp },
                            trailExpressionProvider = { trailExpression },
                            trailCasingExpressionProvider = { trailCasingExpression },
                            violatedSectionExpressionProvider = { violatedSectionExpression },
                        ),
                        alternativeRouteLinesDynamicData = listOf(
                            RouteLineDynamicData(
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                            ),
                            RouteLineDynamicData(
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                            )
                        ),
                        routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                            { routeLineExp },
                            { casingLineEx },
                            { trafficLineExp },
                            { restrictedRoadExp },
                            trailExpressionProvider = { trailExpression },
                            trailCasingExpressionProvider = { trailCasingExpression },
                            violatedSectionExpressionProvider = { violatedSectionExpression },
                        )
                    ).also {
                        it.ignorePrimaryRouteLineData = true
                    }
                )
            val style = getMockedStyle()
            every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val view = MapboxRouteLineView(options)
        view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
        view.renderRouteLineUpdate(style, state)

            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_TRAFFIC,
                    "line-gradient",
                    trafficLineExp
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_MAIN,
                    "line-gradient",
                    routeLineExp
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_CASING,
                    "line-gradient",
                    casingLineEx
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_RESTRICTED,
                    "line-gradient",
                    restrictedRoadExp
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_VIOLATED,
                    "line-gradient",
                    violatedSectionExpression
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_TRAIL,
                    "line-gradient",
                    trailExpression
                )
            }
            verify(exactly = 0) {
                style.setStyleLayerProperty(
                    LAYER_GROUP_1_TRAIL_CASING,
                    "line-gradient",
                    trailCasingExpression
                )
            }
            verify {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAFFIC,
                    "line-gradient",
                    trafficLineExp
                )
            }
            verify {
                style.setStyleLayerProperty(
                    MASKING_LAYER_MAIN,
                    "line-gradient",
                    routeLineExp
                )
            }
            verify {
                style.setStyleLayerProperty(
                    MASKING_LAYER_CASING,
                    "line-gradient",
                    casingLineEx
                )
            }
            verify {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAIL,
                    "line-gradient",
                    trailExpression
                )
            }
            verify {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAIL_CASING,
                    "line-gradient",
                    trailCasingExpression
                )
            }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineTrimUpdate() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExp = mockk<Expression>()
        val trailCasingExp = mockk<Expression>()
        val violatedSectionExp = mockk<Expression>()
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        RouteLineTrimExpressionProvider { routeLineExp },
                        RouteLineTrimExpressionProvider { casingLineEx },
                        RouteLineTrimExpressionProvider { trafficLineExp },
                        RouteLineTrimExpressionProvider { restrictedRoadExp },
                        RouteLineTrimExpressionProvider { violatedSectionExp },
                        RouteLineTrimOffset(.5),
                        RouteLineTrimExpressionProvider { trailExp },
                        RouteLineTrimExpressionProvider { trailCasingExp }
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                        ),
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                        )
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExp },
                        trailCasingExpressionProvider = { trailCasingExp },
                        violatedSectionExpressionProvider = { violatedSectionExp }
                    )
                )
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val view = MapboxRouteLineView(options)
        view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
        view.renderRouteLineUpdate(style, state)

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-trim-offset",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-trim-offset",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-trim-offset",
                casingLineEx
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-trim-offset",
                restrictedRoadExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_VIOLATED,
                "line-trim-offset",
                violatedSectionExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-trim-offset",
                trailExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-trim-offset",
                trailCasingExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                trafficLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                routeLineExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                casingLineEx
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                trailExp
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                trailCasingExp
            )
        }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun `renderRouteDrawData when new routes`() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        val expectedRoute1Expression = literal(listOf(0.0, 9.9))
        val expectedRoute2Expression = literal(listOf(0.0, 0.0))
        val expectedRoute3Expression = literal(listOf(0.0, 0.1))
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("1")))
        val alternativeRoute1FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("2")))
        val alternativeRoute2FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("3")))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val layerGroup1Expression = mockk<Expression>()
        val layerGroup2Expression = mockk<Expression>()
        val layerGroup3Expression = mockk<Expression>()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val bottomLevelLayer = mockk<BackgroundLayer>(relaxed = true)
        val topLevelLayer = mockk<BackgroundLayer>(relaxed = true)
        val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
        val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
        val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
        val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
        val maskingExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteFeatureCollection,
                    RouteLineDynamicData(
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        RouteLineTrimOffset(9.9),
                        { layerGroup1Expression },
                        { layerGroup1Expression }
                    )
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            RouteLineTrimOffset(0.0),
                            { layerGroup2Expression },
                            { layerGroup2Expression }
                        )
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            RouteLineTrimOffset(0.1),
                            { layerGroup3Expression },
                            { layerGroup3Expression }
                        )
                    )
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    trimOffset = RouteLineTrimOffset(9.9),
                    trailExpressionProvider = { maskingExpression },
                    trailCasingExpressionProvider = { maskingExpression }
                )
            )
        )
        val style = getMockedStyle(
            route1TrailCasing,
            route1Trail,
            route1Casing,
            route1Main,
            route1Traffic,
            route1Restricted,
            route1Violated,
            route2TrailCasing,
            route2Trail,
            route2Casing,
            route2Main,
            route2Traffic,
            route2Restricted,
            route2Violated,
            route3TrailCasing,
            route3Trail,
            route3Casing,
            route3Main,
            route3Traffic,
            route3Restricted,
            route3Violated,
            maskingTrailCasing,
            maskingTrail,
            maskingCasing,
            maskingMain,
            maskingTraffic,
            topLevelLayer,
            bottomLevelLayer,
            primaryRouteSource,
            altRoute1Source,
            altRoute2Source,
            wayPointSource
        )
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs
        every {
            style.setStyleLayerProperty(any(), any(), any())
        } returns ExpectedFactory.createNone()
        every {
            style.getStyleLayerProperty(MASKING_LAYER_TRAFFIC, "source")
        } returns mockk {
            every { value } returns Value.valueOf("foobar")
        }
        every {
            MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style)
        } returns LAYER_GROUP_1_MAIN
        val primaryDataId = 2
        val alt1DataId = 4
        val alt2DataId = 7
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID)
        } returns primaryDataId
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID)
        } returns alt1DataId
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_3_SOURCE_ID)
        } returns alt2DataId

        MapboxRouteLineView(options, routesExpector, dataIdHolder).renderRouteDrawData(style, state)
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        verify(exactly = 1) {
            primaryRouteSource.featureCollection(
                primaryRouteFeatureCollection,
                primaryDataId.toString()
            )
        }
        verify(exactly = 1) {
            altRoute1Source.featureCollection(
                alternativeRoute1FeatureCollection,
                alt1DataId.toString()
            )
        }
        verify(exactly = 1) {
            altRoute2Source.featureCollection(
                alternativeRoute2FeatureCollection,
                alt2DataId.toString()
            )
        }
        verify(exactly = 1) { wayPointSource.featureCollection(waypointsFeatureCollection) }

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_VIOLATED,
                "line-gradient",
                layerGroup1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_RESTRICTED,
                "line-gradient",
                layerGroup2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_VIOLATED,
                "line-gradient",
                layerGroup2Expression
            )
        }

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_RESTRICTED,
                "line-gradient",
                layerGroup3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_VIOLATED,
                "line-gradient",
                layerGroup3Expression
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                maskingExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                maskingExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                maskingExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                maskingExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                maskingExpression
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_VIOLATED,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_RESTRICTED,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_VIOLATED,
                "line-trim-offset",
                expectedRoute2Expression
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_RESTRICTED,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_VIOLATED,
                "line-trim-offset",
                expectedRoute3Expression
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-trim-offset",
                expectedRoute1Expression
            )
        }

        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_MAIN, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAFFIC, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_RESTRICTED, any()) }
        verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_VIOLATED, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAIL_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAIL, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_MAIN, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAFFIC, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_RESTRICTED, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_VIOLATED, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAIL_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAIL, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_MAIN, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAFFIC, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_RESTRICTED, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_VIOLATED, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAIL_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAIL, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_MAIN, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAFFIC, any()) }

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-width",
                options.resourceProvider.routeTrafficLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-width",
                options.resourceProvider.alternativeRouteTrafficLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-width",
                options.resourceProvider.alternativeRouteTrafficLineScaleExpression
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-width",
                options.resourceProvider.routeTrafficLineScaleExpression
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

        @Test
    fun `renderRouteDrawData with custom expressions`() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)

        val routeTrafficLineScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 1.5f),
                RouteLineScaleValue(10f, 4f, 1.5f),
                RouteLineScaleValue(13f, 6f, 1.5f),
                RouteLineScaleValue(16f, 10f, 1.5f),
                RouteLineScaleValue(19f, 14f, 1.5f),
                RouteLineScaleValue(22f, 18f, 1.5f)
            )
        )

        val alternativeRouteTrafficLineScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.5f),
                RouteLineScaleValue(10f, 4f, 0.5f),
                RouteLineScaleValue(13f, 6f, 0.5f),
                RouteLineScaleValue(16f, 10f, 0.5f),
                RouteLineScaleValue(19f, 14f, 0.5f),
                RouteLineScaleValue(22f, 18f, 0.5f)
            )
        )

        val routeLineCasingScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.9f),
                RouteLineScaleValue(10f, 4f, 0.9f),
                RouteLineScaleValue(13f, 6f, 0.9f),
                RouteLineScaleValue(16f, 10f, 0.9f),
                RouteLineScaleValue(19f, 14f, 0.9f),
                RouteLineScaleValue(22f, 18f, 0.9f)
            )
        )
        val alternativeRouteLineCasingScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.2f),
                RouteLineScaleValue(10f, 4f, 0.2f),
                RouteLineScaleValue(13f, 6f, 0.2f),
                RouteLineScaleValue(16f, 10f, 0.2f),
                RouteLineScaleValue(19f, 14f, 0.2f),
                RouteLineScaleValue(22f, 18f, 0.2f)
            )
        )

        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder()
                            .routeUnknownCongestionColor(Color.CYAN)
                            .alternativeRouteCasingColor(Color.parseColor("#179C6A"))
                            .alternativeRouteDefaultColor(Color.parseColor("#17E899"))
                            .alternativeRouteUnknownCongestionColor(Color.parseColor("#F5AC00"))
                            .alternativeRouteLowCongestionColor(Color.parseColor("#FF9A00"))
                            .alternativeRouteModerateCongestionColor(Color.parseColor("#E8720C"))
                            .alternativeRouteHeavyCongestionColor(Color.parseColor("#FF5200"))
                            .alternativeRouteSevereCongestionColor(Color.parseColor("#F52B00"))
                            .build()
                    )
                    .routeLineScaleExpression(routeTrafficLineScaleExpression)
                    .routeTrafficLineScaleExpression(routeTrafficLineScaleExpression)
                    .routeCasingLineScaleExpression(routeLineCasingScaleExpression)
                    .alternativeRouteTrafficLineScaleExpression(
                        alternativeRouteTrafficLineScaleExpression
                    )
                    .alternativeRouteLineScaleExpression(alternativeRouteTrafficLineScaleExpression)
                    .alternativeRouteCasingLineScaleExpression(
                        alternativeRouteLineCasingScaleExpression
                    )
                    .build()
            )
            .build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("1")))
        val alternativeRoute1FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("2")))
        val alternativeRoute2FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("3")))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val layerGroup1Expression = mockk<Expression>()
        val layerGroup2Expression = mockk<Expression>()
        val layerGroup3Expression = mockk<Expression>()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val bottomLevelLayer = mockk<BackgroundLayer>(relaxed = true)
        val topLevelLayer = mockk<BackgroundLayer>(relaxed = true)
        val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
        val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
        val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
        val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
        val maskingExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteFeatureCollection,
                    RouteLineDynamicData(
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        RouteLineTrimOffset(9.9),
                        { layerGroup1Expression },
                        { layerGroup1Expression }
                    )
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            RouteLineTrimOffset(0.0),
                            { layerGroup2Expression },
                            { layerGroup2Expression }
                        )
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            RouteLineTrimOffset(0.1),
                            { layerGroup3Expression },
                            { layerGroup3Expression }
                        )
                    )
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    trimOffset = RouteLineTrimOffset(9.9),
                    trailExpressionProvider = { maskingExpression },
                    trailCasingExpressionProvider = { maskingExpression }
                )
            )
        )
        val style = getMockedStyle(
            route1TrailCasing,
            route1Trail,
            route1Casing,
            route1Main,
            route1Traffic,
            route1Restricted,
            route1Violated,
            route2TrailCasing,
            route2Trail,
            route2Casing,
            route2Main,
            route2Traffic,
            route2Restricted,
            route2Violated,
            route3TrailCasing,
            route3Trail,
            route3Casing,
            route3Main,
            route3Traffic,
            route3Restricted,
            route3Violated,
            maskingTrailCasing,
            maskingTrail,
            maskingCasing,
            maskingMain,
            maskingTraffic,
            topLevelLayer,
            bottomLevelLayer,
            primaryRouteSource,
            altRoute1Source,
            altRoute2Source,
            wayPointSource
        )
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs
        every {
            style.setStyleLayerProperty(any(), any(), any())
        } returns ExpectedFactory.createNone()
        every {
            style.getStyleLayerProperty(MASKING_LAYER_TRAFFIC, "source")
        } returns mockk {
            every { value } returns com.mapbox.bindgen.Value.valueOf("foobar")
        }
        every {
            MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style)
        } returns LAYER_GROUP_1_MAIN
        val primaryDataId = 2
        val alt1DataId = 4
        val alt2DataId = 7
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID)
        } returns primaryDataId
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID)
        } returns alt1DataId
        every {
            dataIdHolder.incrementDataId(LAYER_GROUP_3_SOURCE_ID)
        } returns alt2DataId

        MapboxRouteLineView(options, routesExpector, dataIdHolder).renderRouteDrawData(style, state)

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-width",
                options.resourceProvider.routeTrafficLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-width",
                options.resourceProvider.alternativeRouteTrafficLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-width",
                options.resourceProvider.alternativeRouteCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-width",
                options.resourceProvider.alternativeRouteLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-width",
                options.resourceProvider.alternativeRouteTrafficLineScaleExpression
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-width",
                options.resourceProvider.routeCasingLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-width",
                options.resourceProvider.routeLineScaleExpression
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-width",
                options.resourceProvider.routeTrafficLineScaleExpression
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun `renderRouteDrawData when routes rearranged moves layers up`() {
            mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
            mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
            mockkObject(MapboxRouteLineUtils)
            val options = MapboxRouteLineOptions.Builder(ctx).build()
            val primaryRouteFeatureCollection =
                FeatureCollection.fromFeatures(listOf(getEmptyFeature("1")))
            val alternativeRoute1FeatureCollection =
                FeatureCollection.fromFeatures(listOf(getEmptyFeature("2")))
            val alternativeRoute2FeatureCollection =
                FeatureCollection.fromFeatures(listOf(getEmptyFeature("3")))
            val waypointsFeatureCollection =
                FeatureCollection.fromFeatures(listOf(getEmptyFeature("foobar")))
            val layerGroup1Expression = mockk<Expression>()
            val layerGroup2Expression = mockk<Expression>()
            val layerGroup3Expression = mockk<Expression>()
            val route1TrailCasing = mockk<LineLayer>(relaxed = true)
            val route1Trail = mockk<LineLayer>(relaxed = true)
            val route1Casing = mockk<LineLayer>(relaxed = true)
            val route1Main = mockk<LineLayer>(relaxed = true)
            val route1Traffic = mockk<LineLayer>(relaxed = true)
            val route1Restricted = mockk<LineLayer>(relaxed = true)
            val route1Violated = mockk<LineLayer>(relaxed = true)
            val route2TrailCasing = mockk<LineLayer>(relaxed = true)
            val route2Trail = mockk<LineLayer>(relaxed = true)
            val route2Casing = mockk<LineLayer>(relaxed = true)
            val route2Main = mockk<LineLayer>(relaxed = true)
            val route2Traffic = mockk<LineLayer>(relaxed = true)
            val route2Restricted = mockk<LineLayer>(relaxed = true)
            val route2Violated = mockk<LineLayer>(relaxed = true)
            val route3TrailCasing = mockk<LineLayer>(relaxed = true)
            val route3Trail = mockk<LineLayer>(relaxed = true)
            val route3Casing = mockk<LineLayer>(relaxed = true)
            val route3Main = mockk<LineLayer>(relaxed = true)
            val route3Traffic = mockk<LineLayer>(relaxed = true)
            val route3Restricted = mockk<LineLayer>(relaxed = true)
            val route3Violated = mockk<LineLayer>(relaxed = true)
            val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
            val maskingTrail = mockk<LineLayer>(relaxed = true)
            val maskingCasing = mockk<LineLayer>(relaxed = true)
            val maskingMain = mockk<LineLayer>(relaxed = true)
            val maskingTraffic = mockk<LineLayer>(relaxed = true)
            val bottomLevelLayer = mockk<BackgroundLayer>(relaxed = true)
            val topLevelLayer = mockk<BackgroundLayer>(relaxed = true)
            val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
            val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
            val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
            val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
            val primaryRouteLine = RouteLineData(
                primaryRouteFeatureCollection,
                RouteLineDynamicData(
                    { layerGroup1Expression },
                    { layerGroup1Expression },
                    { layerGroup1Expression },
                    { layerGroup1Expression },
                    { layerGroup1Expression },
                    RouteLineTrimOffset(9.9),
                    { layerGroup1Expression },
                    { layerGroup1Expression }
                )
            )
            val atlRouteLine1 = RouteLineData(
                alternativeRoute1FeatureCollection,
                RouteLineDynamicData(
                    { layerGroup2Expression },
                    { layerGroup2Expression },
                    { layerGroup2Expression },
                    { layerGroup2Expression },
                    { layerGroup2Expression },
                    RouteLineTrimOffset(0.0),
                    { layerGroup2Expression },
                    { layerGroup2Expression }
                )
            )
            val atlRouteLine2 = RouteLineData(
                alternativeRoute2FeatureCollection,
                RouteLineDynamicData(
                    { layerGroup3Expression },
                    { layerGroup3Expression },
                    { layerGroup3Expression },
                    { layerGroup3Expression },
                    { layerGroup3Expression },
                    RouteLineTrimOffset(0.1),
                    { layerGroup3Expression },
                    { layerGroup3Expression }
                )
            )
            val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
                RouteSetValue(
                    primaryRouteLineData = primaryRouteLine,
                    alternativeRouteLinesData = listOf(atlRouteLine1, atlRouteLine2),
                    waypointsFeatureCollection
                )
            )
            val state2: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
                RouteSetValue(
                    primaryRouteLineData = atlRouteLine1,
                    alternativeRouteLinesData = listOf(primaryRouteLine, atlRouteLine2),
                    waypointsFeatureCollection
                )
            )
            val style = getMockedStyle(
                route1TrailCasing,
                route1Trail,
                route1Casing,
                route1Main,
                route1Traffic,
                route1Restricted,
                route1Violated,
                route2TrailCasing,
                route2Trail,
                route2Casing,
                route2Main,
                route2Traffic,
                route2Restricted,
                route2Violated,
                route3TrailCasing,
                route3Trail,
                route3Casing,
                route3Main,
                route3Traffic,
                route3Restricted,
                route3Violated,
                maskingTrailCasing,
                maskingTrail,
                maskingCasing,
                maskingMain,
                maskingTraffic,
                topLevelLayer,
                bottomLevelLayer,
                primaryRouteSource,
                altRoute1Source,
                altRoute2Source,
                wayPointSource
            )
            every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs
            val style2 = getMockedStyle(
                route1TrailCasing,
                route1Trail,
                route1Casing,
                route1Main,
                route1Traffic,
                route1Restricted,
                route1Violated,
                route2TrailCasing,
                route2Trail,
                route2Casing,
                route2Main,
                route2Traffic,
                route2Restricted,
                route2Violated,
                route3TrailCasing,
                route3Trail,
                route3Casing,
                route3Main,
                route3Traffic,
                route3Restricted,
                route3Violated,
                maskingTrailCasing,
                maskingTrail,
                maskingCasing,
                maskingMain,
                maskingTraffic,
                topLevelLayer,
                bottomLevelLayer,
                primaryRouteSource,
                altRoute1Source,
                altRoute2Source,
                wayPointSource
            )
            every { MapboxRouteLineUtils.initializeLayers(style2, options) } just Runs
            every {
                MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style)
            } returns LAYER_GROUP_1_MAIN
            every {
                MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style2)
            } returns LAYER_GROUP_2_MAIN

        val primaryDataId = 3
        val altDataId = 5
        every { dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID) } returns primaryDataId
        every { dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID) } returns altDataId

        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder)
        view.renderRouteDrawData(style, state)
        view.renderRouteDrawData(style2, state2)

            verify(exactly = 1) { MapboxRouteLineUtils.initializeLayers(style, options) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL_CASING, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_CASING, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_MAIN, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAFFIC, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_RESTRICTED, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_VIOLATED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAIL_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAIL, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_MAIN, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAFFIC, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_RESTRICTED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_VIOLATED, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAIL_CASING, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAIL, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_CASING, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_MAIN, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAFFIC, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_RESTRICTED, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_VIOLATED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAIL_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAIL, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_MAIN, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAFFIC, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_RESTRICTED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_VIOLATED, any()) }

        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAIL_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAIL, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_CASING, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_MAIN, any()) }
        verify(exactly = 1) { style.moveStyleLayer(MASKING_LAYER_TRAFFIC, any()) }
        verify(exactly = 1) { style2.moveStyleLayer(MASKING_LAYER_TRAIL_CASING, any()) }
        verify(exactly = 1) { style2.moveStyleLayer(MASKING_LAYER_TRAIL, any()) }
        verify(exactly = 1) { style2.moveStyleLayer(MASKING_LAYER_CASING, any()) }
        verify(exactly = 1) { style2.moveStyleLayer(MASKING_LAYER_MAIN, any()) }
        verify(exactly = 1) { style2.moveStyleLayer(MASKING_LAYER_TRAFFIC, any()) }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun showPrimaryRoute() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every { getLayer(any()) } returns mockk(relaxed = true)
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every { getLayer(LAYER_GROUP_1_MAIN) } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every { getLayer(LAYER_GROUP_1_TRAIL) } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showPrimaryRoute(style)

        verify { route1TrailCasing.visibility(Visibility.VISIBLE) }
        verify { route1Trail.visibility(Visibility.VISIBLE) }
        verify { route1Casing.visibility(Visibility.VISIBLE) }
        verify { route1Main.visibility(Visibility.VISIBLE) }
        verify { route1Traffic.visibility(Visibility.VISIBLE) }
        verify { route1Restricted.visibility(Visibility.VISIBLE) }
        verify { route1Violated.visibility(Visibility.VISIBLE) }
        verify { maskingTrailCasing.visibility(Visibility.VISIBLE) }
        verify { maskingTrail.visibility(Visibility.VISIBLE) }
        verify { maskingCasing.visibility(Visibility.VISIBLE) }
        verify { maskingMain.visibility(Visibility.VISIBLE) }
        verify { maskingTraffic.visibility(Visibility.VISIBLE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun hidePrimaryRoute() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every { getLayer(any()) } returns mockk<LineLayer>(relaxed = true)
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every { getLayer(LAYER_GROUP_1_MAIN) } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every { getLayer(LAYER_GROUP_1_TRAIL) } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).hidePrimaryRoute(style)

        verify { route1TrailCasing.visibility(Visibility.NONE) }
        verify { route1Trail.visibility(Visibility.NONE) }
        verify { route1Casing.visibility(Visibility.NONE) }
        verify { route1Main.visibility(Visibility.NONE) }
        verify { route1Traffic.visibility(Visibility.NONE) }
        verify { route1Restricted.visibility(Visibility.NONE) }
        verify { route1Violated.visibility(Visibility.NONE) }
        verify { maskingTrailCasing.visibility(Visibility.NONE) }
        verify { maskingTrail.visibility(Visibility.NONE) }
        verify { maskingCasing.visibility(Visibility.NONE) }
        verify { maskingMain.visibility(Visibility.NONE) }
        verify { maskingTraffic.visibility(Visibility.NONE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun showAlternativeRoutes() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every {
                getLayer(LAYER_GROUP_1_TRAIL)
            } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_MAIN)
            } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every {
                getLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns route2TrailCasing
            every {
                getLayer(LAYER_GROUP_2_TRAIL)
            } returns route2Trail
            every {
                getLayer(LAYER_GROUP_2_CASING)
            } returns route2Casing
            every {
                getLayer(LAYER_GROUP_2_MAIN)
            } returns route2Main
            every {
                getLayer(LAYER_GROUP_2_TRAFFIC)
            } returns route2Traffic
            every {
                getLayer(LAYER_GROUP_2_RESTRICTED)
            } returns route2Restricted
            every {
                getLayer(LAYER_GROUP_2_VIOLATED)
            } returns route2Violated
            every {
                getLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns route3TrailCasing
            every {
                getLayer(LAYER_GROUP_3_TRAIL)
            } returns route3Trail
            every {
                getLayer(LAYER_GROUP_3_CASING)
            } returns route3Casing
            every {
                getLayer(LAYER_GROUP_3_MAIN)
            } returns route3Main
            every {
                getLayer(LAYER_GROUP_3_TRAFFIC)
            } returns route3Traffic
            every {
                getLayer(LAYER_GROUP_3_RESTRICTED)
            } returns route3Restricted
            every {
                getLayer(LAYER_GROUP_3_VIOLATED)
            } returns route3Violated
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showAlternativeRoutes(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Violated.visibility(Visibility.VISIBLE) }
        verify { route2TrailCasing.visibility(Visibility.VISIBLE) }
        verify { route2Trail.visibility(Visibility.VISIBLE) }
        verify { route2Casing.visibility(Visibility.VISIBLE) }
        verify { route2Main.visibility(Visibility.VISIBLE) }
        verify { route2Traffic.visibility(Visibility.VISIBLE) }
        verify { route2Restricted.visibility(Visibility.VISIBLE) }
        verify { route2Violated.visibility(Visibility.VISIBLE) }
        verify { route3TrailCasing.visibility(Visibility.VISIBLE) }
        verify { route3Trail.visibility(Visibility.VISIBLE) }
        verify { route3Casing.visibility(Visibility.VISIBLE) }
        verify { route3Main.visibility(Visibility.VISIBLE) }
        verify { route3Traffic.visibility(Visibility.VISIBLE) }
        verify { route3Restricted.visibility(Visibility.VISIBLE) }
        verify { route3Violated.visibility(Visibility.VISIBLE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun hideAlternativeRoutes() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every {
                getLayer(LAYER_GROUP_1_TRAIL)
            } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_MAIN)
            } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every {
                getLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns route2TrailCasing
            every {
                getLayer(LAYER_GROUP_2_TRAIL)
            } returns route2Trail
            every {
                getLayer(LAYER_GROUP_2_CASING)
            } returns route2Casing
            every {
                getLayer(LAYER_GROUP_2_MAIN)
            } returns route2Main
            every {
                getLayer(LAYER_GROUP_2_TRAFFIC)
            } returns route2Traffic
            every {
                getLayer(LAYER_GROUP_2_RESTRICTED)
            } returns route2Restricted
            every {
                getLayer(LAYER_GROUP_2_VIOLATED)
            } returns route2Violated
            every {
                getLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns route3TrailCasing
            every {
                getLayer(LAYER_GROUP_3_TRAIL)
            } returns route3Trail
            every {
                getLayer(LAYER_GROUP_3_CASING)
            } returns route3Casing
            every {
                getLayer(LAYER_GROUP_3_MAIN)
            } returns route3Main
            every {
                getLayer(LAYER_GROUP_3_TRAFFIC)
            } returns route3Traffic
            every {
                getLayer(LAYER_GROUP_3_RESTRICTED)
            } returns route3Restricted
            every {
                getLayer(LAYER_GROUP_3_VIOLATED)
            } returns route3Violated
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).hideAlternativeRoutes(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Violated.visibility(Visibility.NONE) }
        verify { route2TrailCasing.visibility(Visibility.NONE) }
        verify { route2Trail.visibility(Visibility.NONE) }
        verify { route2Casing.visibility(Visibility.NONE) }
        verify { route2Main.visibility(Visibility.NONE) }
        verify { route2Traffic.visibility(Visibility.NONE) }
        verify { route2Restricted.visibility(Visibility.NONE) }
        verify { route2Violated.visibility(Visibility.NONE) }
        verify { route3TrailCasing.visibility(Visibility.NONE) }
        verify { route3Trail.visibility(Visibility.NONE) }
        verify { route3Casing.visibility(Visibility.NONE) }
        verify { route3Main.visibility(Visibility.NONE) }
        verify { route3Traffic.visibility(Visibility.NONE) }
        verify { route3Restricted.visibility(Visibility.NONE) }
        verify { route3Violated.visibility(Visibility.NONE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun hideTraffic() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every {
                getLayer(LAYER_GROUP_1_TRAIL)
            } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_MAIN)
            } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every {
                getLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns route2TrailCasing
            every {
                getLayer(LAYER_GROUP_2_TRAIL)
            } returns route2Trail
            every {
                getLayer(LAYER_GROUP_2_CASING)
            } returns route2Casing
            every {
                getLayer(LAYER_GROUP_2_MAIN)
            } returns route2Main
            every {
                getLayer(LAYER_GROUP_2_TRAFFIC)
            } returns route2Traffic
            every {
                getLayer(LAYER_GROUP_2_RESTRICTED)
            } returns route2Restricted
            every {
                getLayer(LAYER_GROUP_2_VIOLATED)
            } returns route2Violated
            every {
                getLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns route3TrailCasing
            every {
                getLayer(LAYER_GROUP_3_TRAIL)
            } returns route3Trail
            every {
                getLayer(LAYER_GROUP_3_CASING)
            } returns route3Casing
            every {
                getLayer(LAYER_GROUP_3_MAIN)
            } returns route3Main
            every {
                getLayer(LAYER_GROUP_3_TRAFFIC)
            } returns route3Traffic
            every {
                getLayer(LAYER_GROUP_3_RESTRICTED)
            } returns route3Restricted
            every {
                getLayer(LAYER_GROUP_3_VIOLATED)
            } returns route3Violated
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).hideTraffic(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.NONE) }
        verify(exactly = 1) { route1Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Violated.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Main.visibility(Visibility.NONE) }
        verify(exactly = 1) { route2Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Restricted.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Violated.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Main.visibility(Visibility.NONE) }
        verify(exactly = 1) { route3Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Restricted.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Violated.visibility(Visibility.NONE) }
        verify(exactly = 0) { maskingTrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { maskingTrail.visibility(Visibility.NONE) }
        verify(exactly = 0) { maskingCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { maskingMain.visibility(Visibility.NONE) }
        verify(exactly = 1) { maskingTraffic.visibility(Visibility.NONE) }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun showTraffic() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route1Violated = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route2Violated = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val route3Violated = mockk<LineLayer>(relaxed = true)
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every {
                getLayer(LAYER_GROUP_1_TRAIL)
            } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_MAIN)
            } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every {
                getLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns route2TrailCasing
            every {
                getLayer(LAYER_GROUP_2_TRAIL)
            } returns route2Trail
            every {
                getLayer(LAYER_GROUP_2_CASING)
            } returns route2Casing
            every {
                getLayer(LAYER_GROUP_2_MAIN)
            } returns route2Main
            every {
                getLayer(LAYER_GROUP_2_TRAFFIC)
            } returns route2Traffic
            every {
                getLayer(LAYER_GROUP_2_RESTRICTED)
            } returns route2Restricted
            every {
                getLayer(LAYER_GROUP_2_VIOLATED)
            } returns route2Violated
            every {
                getLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns route3TrailCasing
            every {
                getLayer(LAYER_GROUP_3_TRAIL)
            } returns route3Trail
            every {
                getLayer(LAYER_GROUP_3_CASING)
            } returns route3Casing
            every {
                getLayer(LAYER_GROUP_3_MAIN)
            } returns route3Main
            every {
                getLayer(LAYER_GROUP_3_TRAFFIC)
            } returns route3Traffic
            every {
                getLayer(LAYER_GROUP_3_RESTRICTED)
            } returns route3Restricted
            every {
                getLayer(LAYER_GROUP_3_VIOLATED)
            } returns route3Violated
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showTraffic(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { route1Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Violated.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { route2Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Violated.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { route3Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Violated.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { maskingTrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { maskingTrail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { maskingCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { maskingMain.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { maskingTraffic.visibility(Visibility.VISIBLE) }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun getTrafficVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_1_TRAFFIC
            )
        } returns Visibility.VISIBLE
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val result = MapboxRouteLineView(options).getTrafficVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun showOriginAndDestinationPoints() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val waypointLayer = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every { getLayer(WAYPOINT_LAYER_ID) } returns waypointLayer
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showOriginAndDestinationPoints(style)

        verify { waypointLayer.visibility(Visibility.VISIBLE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun hideOriginAndDestinationPoints() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val waypointLayer = mockk<LineLayer>(relaxed = true)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
            every { getLayer(WAYPOINT_LAYER_ID) } returns waypointLayer
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).hideOriginAndDestinationPoints(style)

        verify { waypointLayer.visibility(Visibility.NONE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun getPrimaryRouteVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_1_MAIN
            )
        } returns Visibility.VISIBLE
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val result = MapboxRouteLineView(options).getPrimaryRouteVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun getAlternativeRoutesVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val layerGroup1SourceLayerIds = setOf(
            LAYER_GROUP_1_TRAIL_CASING,
            LAYER_GROUP_1_TRAIL,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
            LAYER_GROUP_1_VIOLATED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_2_MAIN
            )
        } returns Visibility.VISIBLE
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val result = MapboxRouteLineView(options).getAlternativeRoutesVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun routesRenderedNotification() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")

        val mapboxMap = mockk<MapboxMap>(relaxed = true)
        val callback = mockk<RoutesRenderedCallback>(relaxed = true)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        every { MapboxRouteLineUtils.initializeLayers(any(), options) } just Runs
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder)

        val primaryDataId = 3
        val alt1DataId = 5
        val alt2DataId = 9
        every { dataIdHolder.incrementDataId(LAYER_GROUP_1_SOURCE_ID) } returns primaryDataId
        every { dataIdHolder.incrementDataId(LAYER_GROUP_2_SOURCE_ID) } returns alt1DataId
        every { dataIdHolder.incrementDataId(LAYER_GROUP_3_SOURCE_ID) } returns alt2DataId
        renderRoutes(
            view,
            mapboxMap,
            callback,
            "routeId#0",
            "routeId#1",
            "routeId#2"
        )
        val style = mapboxMap.getStyle()!!
        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#2"),
                emptySet(),
                match(
                    matchExpectedRoutes(
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2"),
                        ),
                        emptySet()
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verify {
            routesExpector.expectRoutes(
                emptySet(),
                setOf("routeId#0", "routeId#1", "routeId#2"),
                match(
                    matchExpectedRoutes(
                        emptySet(),
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2"),
                        )
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#2", style)

        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#2"),
                emptySet(),
                match(
                    matchExpectedRoutes(
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2"),
                        ),
                        emptySet()
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#2", style)

        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#2"),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#3", style)

        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#3"),
                setOf("routeId#2"),
                match(
                    matchExpectedRoutes(
                        setOf(Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#3")),
                        setOf(Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2")),
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verify {
            routesExpector.expectRoutes(
                emptySet(),
                setOf("routeId#0", "routeId#1", "routeId#3"),
                match(
                    matchExpectedRoutes(
                        emptySet(),
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#3"),
                        )
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }

        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verify {
            routesExpector.expectRoutes(
                emptySet(),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        view.hidePrimaryRoute(style)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#3", style)

        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#3"),
                emptySet(),
                match(
                    matchExpectedRoutes(
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#3"),
                        ),
                        emptySet(),
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        view.showPrimaryRoute(style)
        view.hideAlternativeRoutes(style)
        clearRoutes(view, mapboxMap, callback, style)
        verify {
            routesExpector.expectRoutes(
                emptySet(),
                setOf("routeId#0", "routeId#1", "routeId#3"),
                match(
                    matchExpectedRoutes(
                        emptySet(),
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#3"),
                        )
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }
        clearAllMocks(answers = false)

        val primaryRouteFeatureCollection = FeatureCollection.fromFeatures(emptyList())
        val altRoute1FeatureCollection = FeatureCollection.fromFeatures(emptyList())
        val altRoute2FeatureCollection = FeatureCollection.fromFeatures(emptyList())

        renderRoutes(
            view,
            mapboxMap,
            callback,
            primaryRouteFeatureCollection,
            altRoute1FeatureCollection,
            altRoute2FeatureCollection,
            style
        )
        verify {
            routesExpector.expectRoutes(
                emptySet(),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#2", style)
        clearAllMocks(answers = false)
        renderRoutes(
            view,
            mapboxMap,
            callback,
            primaryRouteFeatureCollection,
            altRoute1FeatureCollection,
            altRoute2FeatureCollection,
            style
        )
        verify {
            routesExpector.expectRoutes(
                emptySet(),
                setOf("routeId#0", "routeId#1", "routeId#2"),
                match(
                    matchExpectedRoutes(
                        emptySet(),
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2"),
                        ),
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }

        clearRoutes(
            view,
            mapboxMap,
            callback,
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#0"))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#1"))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#2"))),
            style
        )
        verify {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#2"),
                emptySet(),
                match(
                    matchExpectedRoutes(
                        setOf(
                            Triple(LAYER_GROUP_1_SOURCE_ID, primaryDataId, "routeId#0"),
                            Triple(LAYER_GROUP_2_SOURCE_ID, alt1DataId, "routeId#1"),
                            Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2"),
                        ),
                        emptySet(),
                    )
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, callback)
            )
        }

        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        unmockkObject(MapboxRouteLineUtils)
    }

    private fun matchExpectedRoutes(
        expectedRendered: Set<Triple<String, Int, String>>,
        expectedCleared: Set<Triple<String, Int, String>>
    ): (ExpectedRoutesToRenderData) -> Boolean {
        return { actual ->
            val renderMatch = actual.getSourceAndDataIds().mapNotNull { pair ->
                actual.getRenderedRouteId(pair.first)?.let { Triple(pair.first, pair.second, it) }
            }.toSet() == expectedRendered
            val clearMatch = actual.getSourceAndDataIds().mapNotNull { pair ->
                actual.getClearedRouteId(pair.first)?.let { Triple(pair.first, pair.second, it) }
            }.toSet() == expectedCleared
            renderMatch && clearMatch
        }
    }

    private fun renderRoutes(
        mapboxRouteLineView: MapboxRouteLineView,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
        primaryRouteId: String,
        alternative1RouteId: String,
        alternative2RouteId: String,
        style: Style? = null
    ) {
        renderRoutes(
            mapboxRouteLineView,
            map,
            callback,
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(primaryRouteId))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(alternative1RouteId))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(alternative2RouteId))),
            style,
        )
    }

    private fun renderRoutes(
        mapboxRouteLineView: MapboxRouteLineView,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
        primaryRouteFeatureCollection: FeatureCollection,
        alternativeRoute1FeatureCollection: FeatureCollection,
        alternativeRoute2FeatureCollection: FeatureCollection,
        style: Style? = null
    ) {
        val style = if (style == null) {
            val route1TrailCasing = mockk<LineLayer>(relaxed = true)
            val route1Trail = mockk<LineLayer>(relaxed = true)
            val route1Casing = mockk<LineLayer>(relaxed = true)
            val route1Main = mockk<LineLayer>(relaxed = true)
            val route1Traffic = mockk<LineLayer>(relaxed = true)
            val route1Restricted = mockk<LineLayer>(relaxed = true)
            val route1Violated = mockk<LineLayer>(relaxed = true)
            val route2TrailCasing = mockk<LineLayer>(relaxed = true)
            val route2Trail = mockk<LineLayer>(relaxed = true)
            val route2Casing = mockk<LineLayer>(relaxed = true)
            val route2Main = mockk<LineLayer>(relaxed = true)
            val route2Traffic = mockk<LineLayer>(relaxed = true)
            val route2Restricted = mockk<LineLayer>(relaxed = true)
            val route2Violated = mockk<LineLayer>(relaxed = true)
            val route3TrailCasing = mockk<LineLayer>(relaxed = true)
            val route3Trail = mockk<LineLayer>(relaxed = true)
            val route3Casing = mockk<LineLayer>(relaxed = true)
            val route3Main = mockk<LineLayer>(relaxed = true)
            val route3Traffic = mockk<LineLayer>(relaxed = true)
            val route3Restricted = mockk<LineLayer>(relaxed = true)
            val route3Violated = mockk<LineLayer>(relaxed = true)
            val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
            val maskingTrail = mockk<LineLayer>(relaxed = true)
            val maskingCasing = mockk<LineLayer>(relaxed = true)
            val maskingMain = mockk<LineLayer>(relaxed = true)
            val maskingTraffic = mockk<LineLayer>(relaxed = true)
            val bottomLevelLayer = mockk<BackgroundLayer>(relaxed = true)
            val topLevelLayer = mockk<BackgroundLayer>(relaxed = true)
            val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
            val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
            val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
            val wayPointSource = mockk<GeoJsonSource>(relaxed = true)
            getMockedStyle(
                route1TrailCasing,
                route1Trail,
                route1Casing,
                route1Main,
                route1Traffic,
                route1Restricted,
                route1Violated,
                route2TrailCasing,
                route2Trail,
                route2Casing,
                route2Main,
                route2Traffic,
                route2Restricted,
                route2Violated,
                route3TrailCasing,
                route3Trail,
                route3Casing,
                route3Main,
                route3Traffic,
                route3Restricted,
                route3Violated,
                maskingTrailCasing,
                maskingTrail,
                maskingCasing,
                maskingMain,
                maskingTraffic,
                topLevelLayer,
                bottomLevelLayer,
                primaryRouteSource,
                altRoute1Source,
                altRoute2Source,
                wayPointSource
            )
        } else {
            style
        }
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val layerGroup1Expression = mockk<Expression>()
        val layerGroup2Expression = mockk<Expression>()
        val layerGroup3Expression = mockk<Expression>()
        val maskingExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteFeatureCollection,
                    RouteLineDynamicData(
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        { layerGroup1Expression },
                        RouteLineTrimOffset(9.9),
                        { layerGroup1Expression },
                        { layerGroup1Expression }
                    )
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            { layerGroup2Expression },
                            RouteLineTrimOffset(0.0),
                            { layerGroup2Expression },
                            { layerGroup2Expression }
                        )
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            { layerGroup3Expression },
                            RouteLineTrimOffset(0.1),
                            { layerGroup3Expression },
                            { layerGroup3Expression }
                        )
                    )
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    { maskingExpression },
                    trimOffset = RouteLineTrimOffset(9.9),
                    trailExpressionProvider = { maskingExpression },
                    trailCasingExpressionProvider = { maskingExpression }
                )
            )
        )
        every { map.getStyle() } returns style
        every {
            style.setStyleLayerProperty(any(), any(), any())
        } returns ExpectedFactory.createNone()
        every {
            style.getStyleLayerProperty(MASKING_LAYER_TRAFFIC, "source")
        } returns mockk {
            every { value } returns Value.valueOf("foobar")
        }
        every {
            MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style)
        } returns LAYER_GROUP_1_MAIN

        mapboxRouteLineView.renderRouteDrawData(style, state, map, callback)
    }

    private fun clearRoutes(
        mapboxRouteLineView: MapboxRouteLineView,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
        style: Style? = null
    ) {
        clearRoutes(
            mapboxRouteLineView,
            map,
            callback,
            FeatureCollection.fromFeatures(emptyList()),
            FeatureCollection.fromFeatures(emptyList()),
            FeatureCollection.fromFeatures(emptyList()),
            style
        )
    }

    private fun clearRoutes(
        mapboxRouteLineView: MapboxRouteLineView,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
        primaryRouteFeatureCollection: FeatureCollection,
        alternativeRoute1FeatureCollection: FeatureCollection,
        alternativeRoute2FeatureCollection: FeatureCollection,
        style: Style? = null
    ) {
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(emptyList())
        val style = if (style == null) {
            val topLevelRouteLayer = StyleObjectInfo(
                TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                "background"
            )
            val bottomLevelRouteLayer = StyleObjectInfo(
                BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                "background"
            )
            val mainLayer = StyleObjectInfo(
                LAYER_GROUP_1_MAIN,
                "line"
            )
            mockk<Style> {
                every { getSource(any()) } returns mockk<GeoJsonSource>(relaxed = true)
                every { styleLayers } returns listOf(
                    bottomLevelRouteLayer,
                    mainLayer,
                    topLevelRouteLayer
                )
            }.also {
                mockCheckForLayerInitialization(it)
            }
        } else {
            style
        }

        val state: Expected<RouteLineError, RouteLineClearValue> = ExpectedFactory.createValue(
            RouteLineClearValue(
                primaryRouteFeatureCollection,
                listOf(alternativeRoute1FeatureCollection, alternativeRoute2FeatureCollection),
                waypointsFeatureCollection
            )
        )

        mapboxRouteLineView.renderClearRouteLineValue(style, state, map, callback)
    }

    private fun getEmptyFeature(featureId: String): Feature {
        return Feature.fromJson(
            "{\"type\":\"Feature\",\"id\":\"${featureId}\"," +
                "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]}}"
        )
    }

    private fun getMockedStyle(): Style = mockk(relaxed = true) {
        every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
        every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
        every { setStyleLayerProperty(any(), any(), any()) } returns ExpectedFactory.createNone()
    }

    private fun getMockedStyle(
        route1TrailCasing: LineLayer,
        route1Trail: LineLayer,
        route1Casing: LineLayer,
        route1Main: LineLayer,
        route1Traffic: LineLayer,
        route1Restricted: LineLayer,
        route1Violated: LineLayer,
        route2TrailCasing: LineLayer,
        route2Trail: LineLayer,
        route2Casing: LineLayer,
        route2Main: LineLayer,
        route2Traffic: LineLayer,
        route2Restricted: LineLayer,
        route2Violated: LineLayer,
        route3TrailCasing: LineLayer,
        route3Trail: LineLayer,
        route3Casing: LineLayer,
        route3Main: LineLayer,
        route3Traffic: LineLayer,
        route3Restricted: LineLayer,
        route3Violated: LineLayer,
        maskingTrailCasing: LineLayer,
        maskingTrail: LineLayer,
        maskingCasing: LineLayer,
        maskingMain: LineLayer,
        maskingTraffic: LineLayer,
        topLevelLayer: BackgroundLayer,
        bottomLevelLayer: BackgroundLayer,
        primaryRouteSource: GeoJsonSource,
        altRoute1Source: GeoJsonSource,
        altRoute2Source: GeoJsonSource,
        wayPointSource: GeoJsonSource
    ): Style {
        return mockk<Style>(relaxed = true) {
            every { getLayer(any()) } returns mockk<LineLayer>(relaxed = true)
            every {
                getLayer(LAYER_GROUP_1_TRAIL_CASING)
            } returns route1TrailCasing
            every {
                getLayer(LAYER_GROUP_1_TRAIL)
            } returns route1Trail
            every {
                getLayer(LAYER_GROUP_1_CASING)
            } returns route1Casing
            every {
                getLayer(LAYER_GROUP_1_MAIN)
            } returns route1Main
            every {
                getLayer(LAYER_GROUP_1_TRAFFIC)
            } returns route1Traffic
            every {
                getLayer(LAYER_GROUP_1_RESTRICTED)
            } returns route1Restricted
            every {
                getLayer(LAYER_GROUP_1_VIOLATED)
            } returns route1Violated
            every {
                getLayer(LAYER_GROUP_2_TRAIL_CASING)
            } returns route2TrailCasing
            every {
                getLayer(LAYER_GROUP_2_TRAIL)
            } returns route2Trail
            every {
                getLayer(LAYER_GROUP_2_CASING)
            } returns route2Casing
            every {
                getLayer(LAYER_GROUP_2_MAIN)
            } returns route2Main
            every {
                getLayer(LAYER_GROUP_2_TRAFFIC)
            } returns route2Traffic
            every {
                getLayer(LAYER_GROUP_2_RESTRICTED)
            } returns route2Restricted
            every {
                getLayer(LAYER_GROUP_2_VIOLATED)
            } returns route2Violated
            every {
                getLayer(LAYER_GROUP_3_TRAIL_CASING)
            } returns route3TrailCasing
            every {
                getLayer(LAYER_GROUP_3_TRAIL)
            } returns route3Trail
            every {
                getLayer(LAYER_GROUP_3_CASING)
            } returns route3Casing
            every {
                getLayer(LAYER_GROUP_3_MAIN)
            } returns route3Main
            every {
                getLayer(LAYER_GROUP_3_TRAFFIC)
            } returns route3Traffic
            every {
                getLayer(LAYER_GROUP_3_RESTRICTED)
            } returns route3Restricted
            every {
                getLayer(LAYER_GROUP_3_VIOLATED)
            } returns route3Violated
            every {
                getLayer(MASKING_LAYER_TRAIL_CASING)
            } returns maskingTrailCasing
            every {
                getLayer(MASKING_LAYER_TRAIL)
            } returns maskingTrail
            every {
                getLayer(MASKING_LAYER_CASING)
            } returns maskingCasing
            every {
                getLayer(MASKING_LAYER_MAIN)
            } returns maskingMain
            every {
                getLayer(MASKING_LAYER_TRAFFIC)
            } returns maskingTraffic
            every {
                getLayer(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns topLevelLayer
            every {
                getLayer(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns bottomLevelLayer
            every { getSource(LAYER_GROUP_1_SOURCE_ID) } returns primaryRouteSource
            every { getSource(LAYER_GROUP_2_SOURCE_ID) } returns altRoute1Source
            every { getSource(LAYER_GROUP_3_SOURCE_ID) } returns altRoute2Source
            every { getSource(WAYPOINT_SOURCE_ID) } returns wayPointSource
            every {
                styleLayerExists(LAYER_GROUP_1_RESTRICTED)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_2_RESTRICTED)
            } returns true
            every {
                styleLayerExists(LAYER_GROUP_3_RESTRICTED)
            } returns true
            every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
            every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
            every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
    }

    private fun loadedData(sourceId: String, type: SourceDataType): SourceDataLoadedEventData =
        SourceDataLoadedEventData(0, 0, sourceId, type, null, null, null)
}
