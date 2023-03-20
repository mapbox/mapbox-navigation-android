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
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class MapboxRouteLineViewTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val logRule = LoggingFrontendTestRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk<Drawable>()
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
        unmockkObject(InternalJobControlFactory)
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
    fun renderClearRouteLineValue() = coroutineRule.runBlockingTest {
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

        pauseDispatcher {
            MapboxRouteLineView(options).renderClearRouteLineValue(style, state)
            verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        }

        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
        verify { altRoute1Source.featureCollection(altRoutesFeatureCollection) }
        verify { altRoute2Source.featureCollection(altRoutesFeatureCollection) }
        verify { wayPointSource.featureCollection(waypointsFeatureCollection) }
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun renderClearRouteLineValue_noInitializeRepeat() = coroutineRule.runBlockingTest {
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

        pauseDispatcher {
            val view = MapboxRouteLineView(options)
            view.renderClearRouteLineValue(style, state)
            view.renderClearRouteLineValue(style, state)
        }

        verify(exactly = 1) { MapboxRouteLineUtils.initializeLayers(style, options) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExpression = mockk<Expression>()
        val trailCasingExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExpression },
                        trailCasingExpressionProvider = { trailCasingExpression }
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() }
                        ),
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() }
                        )
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExpression },
                        trailCasingExpressionProvider = { trailCasingExpression }
                    )
                )
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        pauseDispatcher {
            val view = MapboxRouteLineView(options)
            view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
            view.renderRouteLineUpdate(style, state)
        }

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
    fun renderTraveledRouteLineUpdate_whenIgnorePrimaryRouteLineData() =
        coroutineRule.runBlockingTest {
            mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
            mockkObject(MapboxRouteLineUtils)
            val options = MapboxRouteLineOptions.Builder(ctx).build()
            val trafficLineExp = mockk<Expression>()
            val routeLineExp = mockk<Expression>()
            val casingLineEx = mockk<Expression>()
            val restrictedRoadExp = mockk<Expression>()
            val trailExpression = mockk<Expression>()
            val trailCasingExpression = mockk<Expression>()
            val state: Expected<RouteLineError, RouteLineUpdateValue> =
                ExpectedFactory.createValue(
                    RouteLineUpdateValue(
                        primaryRouteLineDynamicData = RouteLineDynamicData(
                            { routeLineExp },
                            { casingLineEx },
                            { trafficLineExp },
                            { restrictedRoadExp },
                            trailExpressionProvider = { trailExpression },
                            trailCasingExpressionProvider = { trailCasingExpression }
                        ),
                        alternativeRouteLinesDynamicData = listOf(
                            RouteLineDynamicData(
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() }
                            ),
                            RouteLineDynamicData(
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() },
                                { throw UnsupportedOperationException() }
                            )
                        ),
                        routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                            { routeLineExp },
                            { casingLineEx },
                            { trafficLineExp },
                            { restrictedRoadExp },
                            trailExpressionProvider = { trailExpression },
                            trailCasingExpressionProvider = { trailCasingExpression }
                        )
                    ).also {
                        it.ignorePrimaryRouteLineData = true
                    }
                )
            val style = getMockedStyle()
            every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

            pauseDispatcher {
                val view = MapboxRouteLineView(options)
                view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
                view.renderRouteLineUpdate(style, state)
            }

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
    fun renderTraveledRouteLineTrimUpdate() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExp = mockk<Expression>()
        val trailCasingExp = mockk<Expression>()
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        RouteLineTrimExpressionProvider { routeLineExp },
                        RouteLineTrimExpressionProvider { casingLineEx },
                        RouteLineTrimExpressionProvider { trafficLineExp },
                        RouteLineTrimExpressionProvider { restrictedRoadExp },
                        RouteLineTrimOffset(.5),
                        RouteLineTrimExpressionProvider { trailExp },
                        RouteLineTrimExpressionProvider { trailCasingExp }
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() }
                        ),
                        RouteLineDynamicData(
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() },
                            { throw UnsupportedOperationException() }
                        )
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineEx },
                        { trafficLineExp },
                        { restrictedRoadExp },
                        trailExpressionProvider = { trailExp },
                        trailCasingExpressionProvider = { trailCasingExp }
                    )
                )
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        pauseDispatcher {
            val view = MapboxRouteLineView(options)
            view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
            view.renderRouteLineUpdate(style, state)
        }

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
    fun `renderRouteDrawData when new routes`() = coroutineRule.runBlockingTest {
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
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
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
            route2TrailCasing,
            route2Trail,
            route2Casing,
            route2Main,
            route2Traffic,
            route2Restricted,
            route3TrailCasing,
            route3Trail,
            route3Casing,
            route3Main,
            route3Traffic,
            route3Restricted,
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

        MapboxRouteLineView(options).renderRouteDrawData(style, state)
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        verify(exactly = 1) { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
        verify(exactly = 1) {
            altRoute1Source.featureCollection(alternativeRoute1FeatureCollection)
        }
        verify(exactly = 1) {
            altRoute2Source.featureCollection(alternativeRoute2FeatureCollection)
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
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAIL_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAIL, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_MAIN, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_TRAFFIC, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_2_RESTRICTED, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAIL_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAIL, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_CASING, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_MAIN, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_TRAFFIC, any()) }
        verify(exactly = 0) { style.moveStyleLayer(LAYER_GROUP_3_RESTRICTED, any()) }
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
    fun `renderRouteDrawData when routes rearranged moves layers up`() =
        coroutineRule.runBlockingTest {
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
            val route2TrailCasing = mockk<LineLayer>(relaxed = true)
            val route2Trail = mockk<LineLayer>(relaxed = true)
            val route2Casing = mockk<LineLayer>(relaxed = true)
            val route2Main = mockk<LineLayer>(relaxed = true)
            val route2Traffic = mockk<LineLayer>(relaxed = true)
            val route2Restricted = mockk<LineLayer>(relaxed = true)
            val route3TrailCasing = mockk<LineLayer>(relaxed = true)
            val route3Trail = mockk<LineLayer>(relaxed = true)
            val route3Casing = mockk<LineLayer>(relaxed = true)
            val route3Main = mockk<LineLayer>(relaxed = true)
            val route3Traffic = mockk<LineLayer>(relaxed = true)
            val route3Restricted = mockk<LineLayer>(relaxed = true)
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
                route2TrailCasing,
                route2Trail,
                route2Casing,
                route2Main,
                route2Traffic,
                route2Restricted,
                route3TrailCasing,
                route3Trail,
                route3Casing,
                route3Main,
                route3Traffic,
                route3Restricted,
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
                route2TrailCasing,
                route2Trail,
                route2Casing,
                route2Main,
                route2Traffic,
                route2Restricted,
                route3TrailCasing,
                route3Trail,
                route3Casing,
                route3Main,
                route3Traffic,
                route3Restricted,
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

            val view = MapboxRouteLineView(options)
            view.renderRouteDrawData(style, state)
            view.renderRouteDrawData(style2, state2)

            verify(exactly = 1) { MapboxRouteLineUtils.initializeLayers(style, options) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL_CASING, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAIL, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_CASING, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_MAIN, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_TRAFFIC, any()) }
            verify(exactly = 1) { style.moveStyleLayer(LAYER_GROUP_1_RESTRICTED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAIL_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAIL, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_MAIN, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_TRAFFIC, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_1_RESTRICTED, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAIL_CASING, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAIL, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_CASING, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_MAIN, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_TRAFFIC, any()) }
            verify(exactly = 1) { style2.moveStyleLayer(LAYER_GROUP_2_RESTRICTED, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAIL_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAIL, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_CASING, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_MAIN, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_TRAFFIC, any()) }
            verify(exactly = 0) { style2.moveStyleLayer(LAYER_GROUP_3_RESTRICTED, any()) }

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
    fun `renderRouteDrawData when primary route is not visible it should stay not visible`() =
        coroutineRule.runBlockingTest {
        }

    // @Test
    fun renderRouteDrawDataTest() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val alternativeRoute1FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val alternativeRoute2FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineExp = mockk<Expression>()
        val alternative1TrafficExp = mockk<Expression>()
        val alternative2TrafficExp = mockk<Expression>()
        val alternative1CasingExp = mockk<Expression>()
        val alternative2CasingExp = mockk<Expression>()
        val alternative1LineExp = mockk<Expression>()
        val alternative2LineExp = mockk<Expression>()
        val restrictedRouteExp = mockk<Expression>()
        val route1TrailCasing = mockk<LineLayer>(relaxed = true)
        val route1Trail = mockk<LineLayer>(relaxed = true)
        val route1Casing = mockk<LineLayer>(relaxed = true)
        val route1Main = mockk<LineLayer>(relaxed = true)
        val route1Traffic = mockk<LineLayer>(relaxed = true)
        val route1Restricted = mockk<LineLayer>(relaxed = true)
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
        val bottomLevelLayer = mockk<LineLayer>(relaxed = true)
        val topLevelLayer = mockk<LineLayer>(relaxed = true)
        val primaryRouteSource = mockk<GeoJsonSource>(relaxed = true)
        val altRoute1Source = mockk<GeoJsonSource>(relaxed = true)
        val altRoute2Source = mockk<GeoJsonSource>(relaxed = true)
        val wayPointSource = mockk<GeoJsonSource>(relaxed = true)

        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteFeatureCollection,
                    RouteLineDynamicData(
                        { routeLineExp },
                        { casingLineExp },
                        { trafficLineExp },
                        { restrictedRouteExp },
                        RouteLineTrimOffset(9.9),
                        { Expression.color(Color.TRANSPARENT) },
                        { Expression.color(Color.TRANSPARENT) }
                    )
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            { alternative1LineExp },
                            { alternative1CasingExp },
                            { alternative1TrafficExp },
                            { restrictedRouteExp },
                            RouteLineTrimOffset(0.0),
                            { Expression.color(Color.TRANSPARENT) },
                            { Expression.color(Color.TRANSPARENT) }
                        )
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            { alternative2LineExp },
                            { alternative2CasingExp },
                            { alternative2TrafficExp },
                            { restrictedRouteExp },
                            RouteLineTrimOffset(0.0),
                            { Expression.color(Color.TRANSPARENT) },
                            { Expression.color(Color.TRANSPARENT) }
                        )
                    )
                ),
                waypointsFeatureCollection
            )
        )
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
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        pauseDispatcher {
            MapboxRouteLineView(options).renderRouteDrawData(style, state)
            verify { MapboxRouteLineUtils.initializeLayers(style, options) }
        }

        verify { route1TrailCasing.lineTrimOffset(literal(listOf(0.0, 9.9))) }
        verify { route1Trail.lineTrimOffset(literal(listOf(0.0, 9.9))) }
        verify { route1Casing.lineTrimOffset(literal(listOf(0.0, 9.9))) }
        verify { route1Main.lineTrimOffset(literal(listOf(0.0, 9.9))) }
        verify { route1Traffic.lineTrimOffset(literal(listOf(0.0, 9.9))) }
        verify { route1Restricted.lineTrimOffset(literal(listOf(0.0, 9.9))) }

        verify { route2TrailCasing.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route2Trail.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route2Casing.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route2Main.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route2Traffic.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route2Restricted.lineTrimOffset(literal(listOf(0.0, 0.0))) }

        verify { route3TrailCasing.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route3Trail.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route3Casing.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route3Main.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route3Traffic.lineTrimOffset(literal(listOf(0.0, 0.0))) }
        verify { route3Restricted.lineTrimOffset(literal(listOf(0.0, 0.0))) }

        verify { route1TrailCasing.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route1Trail.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route1Casing.lineGradient(casingLineExp) }
        verify { route1Main.lineGradient(routeLineExp) }
        verify { route1Traffic.lineGradient(trafficLineExp) }
        verify { route1Restricted.lineGradient(restrictedRouteExp) }
        verify { route2TrailCasing.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route2Trail.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route2Casing.lineGradient(alternative1CasingExp) }
        verify { route2Main.lineGradient(alternative1LineExp) }
        verify { route2Traffic.lineGradient(alternative1TrafficExp) }
        verify { route2Restricted.lineGradient(restrictedRouteExp) }
        verify { route3TrailCasing.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route3Trail.lineGradient(Expression.color(Color.TRANSPARENT)) }
        verify { route3Casing.lineGradient(alternative2CasingExp) }
        verify { route3Main.lineGradient(alternative2LineExp) }
        verify { route3Traffic.lineGradient(alternative2TrafficExp) }
        verify { route3Restricted.lineGradient(restrictedRouteExp) }

        verify { primaryRouteSource.featureCollection(primaryRouteFeatureCollection) }
        verify { altRoute1Source.featureCollection(alternativeRoute1FeatureCollection) }
        verify { altRoute2Source.featureCollection(alternativeRoute2FeatureCollection) }
        verify { wayPointSource.featureCollection(waypointsFeatureCollection) }

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
            LAYER_GROUP_1_RESTRICTED
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
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
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
            LAYER_GROUP_1_RESTRICTED
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
        val maskingTrailCasing = mockk<LineLayer>(relaxed = true)
        val maskingTrail = mockk<LineLayer>(relaxed = true)
        val maskingCasing = mockk<LineLayer>(relaxed = true)
        val maskingMain = mockk<LineLayer>(relaxed = true)
        val maskingTraffic = mockk<LineLayer>(relaxed = true)
        val style = getMockedStyle().apply {
            every { styleLayerExists(any()) } returns true
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
            LAYER_GROUP_1_RESTRICTED
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
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
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
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showAlternativeRoutes(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.VISIBLE) }
        verify { route2TrailCasing.visibility(Visibility.VISIBLE) }
        verify { route2Trail.visibility(Visibility.VISIBLE) }
        verify { route2Casing.visibility(Visibility.VISIBLE) }
        verify { route2Main.visibility(Visibility.VISIBLE) }
        verify { route2Traffic.visibility(Visibility.VISIBLE) }
        verify { route2Restricted.visibility(Visibility.VISIBLE) }
        verify { route3TrailCasing.visibility(Visibility.VISIBLE) }
        verify { route3Trail.visibility(Visibility.VISIBLE) }
        verify { route3Casing.visibility(Visibility.VISIBLE) }
        verify { route3Main.visibility(Visibility.VISIBLE) }
        verify { route3Traffic.visibility(Visibility.VISIBLE) }
        verify { route3Restricted.visibility(Visibility.VISIBLE) }
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
            LAYER_GROUP_1_RESTRICTED
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
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
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
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).hideAlternativeRoutes(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.NONE) }
        verify { route2TrailCasing.visibility(Visibility.NONE) }
        verify { route2Trail.visibility(Visibility.NONE) }
        verify { route2Casing.visibility(Visibility.NONE) }
        verify { route2Main.visibility(Visibility.NONE) }
        verify { route2Traffic.visibility(Visibility.NONE) }
        verify { route2Restricted.visibility(Visibility.NONE) }
        verify { route3TrailCasing.visibility(Visibility.NONE) }
        verify { route3Trail.visibility(Visibility.NONE) }
        verify { route3Casing.visibility(Visibility.NONE) }
        verify { route3Main.visibility(Visibility.NONE) }
        verify { route3Traffic.visibility(Visibility.NONE) }
        verify { route3Restricted.visibility(Visibility.NONE) }
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
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
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
        verify(exactly = 0) { route2TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Main.visibility(Visibility.NONE) }
        verify(exactly = 1) { route2Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route2Restricted.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3TrailCasing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Trail.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Casing.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Main.visibility(Visibility.NONE) }
        verify(exactly = 1) { route3Traffic.visibility(Visibility.NONE) }
        verify(exactly = 0) { route3Restricted.visibility(Visibility.NONE) }
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
        val route2TrailCasing = mockk<LineLayer>(relaxed = true)
        val route2Trail = mockk<LineLayer>(relaxed = true)
        val route2Casing = mockk<LineLayer>(relaxed = true)
        val route2Main = mockk<LineLayer>(relaxed = true)
        val route2Traffic = mockk<LineLayer>(relaxed = true)
        val route2Restricted = mockk<LineLayer>(relaxed = true)
        val route3TrailCasing = mockk<LineLayer>(relaxed = true)
        val route3Trail = mockk<LineLayer>(relaxed = true)
        val route3Casing = mockk<LineLayer>(relaxed = true)
        val route3Main = mockk<LineLayer>(relaxed = true)
        val route3Traffic = mockk<LineLayer>(relaxed = true)
        val route3Restricted = mockk<LineLayer>(relaxed = true)
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
        verify(exactly = 0) { route2TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { route2Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route2Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { route3Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route3Restricted.visibility(Visibility.VISIBLE) }
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
            LAYER_GROUP_1_RESTRICTED
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
            LAYER_GROUP_1_RESTRICTED
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
            LAYER_GROUP_1_RESTRICTED
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
    fun cancel() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        every { InternalJobControlFactory.createDefaultScopeJobControl() } returns mockJobControl

        MapboxRouteLineView(options).cancel()

        verify { mockParentJob.cancelChildren() }
    }

    private fun getEmptyFeature(featureId: String): Feature {
        return Feature.fromJson(
            "{\"type\":\"Feature\",\"id\":\"${featureId}\"," +
                "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]}}"
        )
    }

    private fun getMockedStyle(): Style = mockk {
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
        route2TrailCasing: LineLayer,
        route2Trail: LineLayer,
        route2Casing: LineLayer,
        route2Main: LineLayer,
        route2Traffic: LineLayer,
        route2Restricted: LineLayer,
        route3TrailCasing: LineLayer,
        route3Trail: LineLayer,
        route3Casing: LineLayer,
        route3Main: LineLayer,
        route3Traffic: LineLayer,
        route3Restricted: LineLayer,
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
        return mockk(relaxed = true) {
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
        }
    }
}
