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
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.buildScalingExpression
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.internal.route.line.toStylePropertyValue
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.line.RouteLineHistoryRecordingViewSender
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleExpressions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.util.sdkStyleManager
import com.mapbox.navigation.ui.maps.util.toDelayedRoutesRenderedCallback
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.UUID

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@ExperimentalCoroutinesApi
class MapboxRouteLineViewTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val routesExpector = mockk<RoutesExpector>(relaxed = true)
    private val dataIdHolder = mockk<DataIdHolder>(relaxed = true)
    private val sender = mockk<RouteLineHistoryRecordingViewSender>(relaxed = true)

    private val ctx: Context = mockk()
    private val scope = coroutineRule.createTestScope()
    private val styleId = "some-style-id"
    private val style = mockk<Style>(relaxed = true)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(AppCompatResources::class)
        mockkStatic(Style::sdkStyleManager)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk<Drawable>()
        mockkObject(InternalJobControlFactory)
        every { InternalJobControlFactory.createImmediateMainScopeJobControl() } returns JobControl(
            mockk(),
            scope,
        )
        mockkStatic("com.mapbox.navigation.ui.maps.internal.extensions.MapboxStyleEx")
        every { style.getStyleId() } returns styleId
        every { style.sdkStyleManager } returns mockk(relaxed = true)
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
        unmockkStatic(Style::sdkStyleManager)
        unmockkObject(InternalJobControlFactory)
        unmockkStatic("com.mapbox.navigation.ui.maps.internal.extensions.MapboxStyleEx")
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
            every { sdkStyleManager } returns mockk(relaxed = true) {
                every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
                every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
            }
            every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
            every { getStyleLayerProperty(any(), any()) } returns mockk(relaxed = true)
        }
    }

    @Test
    fun initializeLayers() {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
            every { sdkStyleManager } returns mockk(relaxed = true) {
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val altRoutesFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))
        val routeCalloutData = RouteCalloutData(emptyList())
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
            "background",
        )
        val bottomLevelRouteLayer = StyleObjectInfo(
            BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
            "background",
        )
        val mainLayer = StyleObjectInfo(
            LAYER_GROUP_1_MAIN,
            "line",
        )
        val style = getMockedStyle().apply {
            every { getSource(LAYER_GROUP_1_SOURCE_ID) } returns primaryRouteSource
            every { getSource(LAYER_GROUP_2_SOURCE_ID) } returns altRoute1Source
            every { getSource(LAYER_GROUP_3_SOURCE_ID) } returns altRoute2Source
            every { getSource(WAYPOINT_SOURCE_ID) } returns wayPointSource
            every { styleLayers } returns listOf(
                bottomLevelRouteLayer,
                mainLayer,
                topLevelRouteLayer,
            )
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val state: Expected<RouteLineError, RouteLineClearValue> = ExpectedFactory.createValue(
            RouteLineClearValue(
                primaryRouteFeatureCollection,
                listOf(altRoutesFeatureCollection, altRoutesFeatureCollection),
                waypointsFeatureCollection,
                routeCalloutData,
            ),
        )

        MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            .renderClearRouteLineValue(style, state)
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        verify {
            primaryRouteSource.featureCollection(
                primaryRouteFeatureCollection,
                primaryDataId.toString(),
            )
        }
        verify {
            altRoute1Source.featureCollection(
                altRoutesFeatureCollection,
                altDataId1.toString(),
            )
        }
        verify {
            altRoute2Source.featureCollection(
                altRoutesFeatureCollection,
                altDataId2.toString(),
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val routeCalloutData = RouteCalloutData(emptyList())
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
                waypointsFeatureCollection,
                routeCalloutData,
            ),
        )

        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { routeLineExp.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        casingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { casingLineEx.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        trafficExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { trafficLineExp.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        restrictedSectionExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                restrictedRoadExp.toStylePropertyValue()
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { trailExpression.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                trailCasingExpression.toStylePropertyValue()
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { routeLineExp.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { casingLineEx.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { trafficLineExp.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                restrictedRoadExp.toStylePropertyValue()
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { trailExpression.toStylePropertyValue() },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                trailCasingExpression.toStylePropertyValue()
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
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
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                restrictedRoadExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                trailExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                trailExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression,
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate_executionOrder() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExpression = mockk<Expression>()
        val trailCasingExpression = mockk<Expression>()
        val primaryBaseExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
        }
        val primaryTrafficExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trafficLineExp, StylePropertyValueKind.EXPRESSION)
        }
        val primaryCasingExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
        }
        val primaryRestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxed = true) {
                coEvery {
                    generateCommand(any(), any())
                } returns StylePropertyValue(restrictedRoadExp, StylePropertyValueKind.EXPRESSION)
            }
        val primaryTrailExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trailExpression, StylePropertyValueKind.EXPRESSION)
        }
        val primaryTrailCasingExpressionProvider = mockk<RouteLineValueProvider>(
            relaxed = true,
        ) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trailCasingExpression, StylePropertyValueKind.EXPRESSION)
        }
        val primaryBaseApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val primaryTrafficApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val primaryCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val primaryRestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val primaryTrailApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val primaryTrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)

        val maskingBaseExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
        }
        val maskingTrafficExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trafficLineExp, StylePropertyValueKind.EXPRESSION)
        }
        val maskingCasingExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
        }
        val maskingRestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxed = true) {
                coEvery {
                    generateCommand(any(), any())
                } returns StylePropertyValue(restrictedRoadExp, StylePropertyValueKind.EXPRESSION)
            }
        val maskingTrailExpressionProvider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trailExpression, StylePropertyValueKind.EXPRESSION)
        }
        val maskingTrailCasingExpressionProvider = mockk<RouteLineValueProvider>(
            relaxed = true,
        ) {
            coEvery {
                generateCommand(any(), any())
            } returns StylePropertyValue(trailCasingExpression, StylePropertyValueKind.EXPRESSION)
        }
        val maskingBaseApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrafficApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingRestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrailApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            primaryBaseExpressionProvider,
                            primaryBaseApplier,
                        ),
                        casingExpressionCommandHolder = RouteLineValueCommandHolder(
                            primaryCasingExpressionProvider,
                            primaryCasingApplier,
                        ),
                        trafficExpressionCommandHolder = RouteLineValueCommandHolder(
                            primaryTrafficExpressionProvider,
                            primaryTrafficApplier,
                        ),
                        restrictedSectionExpressionCommandHolder = RouteLineValueCommandHolder(
                            primaryRestrictedExpressionProvider,
                            primaryRestrictedApplier,
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            primaryTrailExpressionProvider,
                            primaryTrailApplier,
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            primaryTrailCasingExpressionProvider,
                            primaryTrailCasingApplier,
                        ),
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            maskingBaseExpressionProvider,
                            maskingBaseApplier,
                        ),
                        RouteLineValueCommandHolder(
                            maskingCasingExpressionProvider,
                            maskingCasingApplier,
                        ),
                        RouteLineValueCommandHolder(
                            maskingTrafficExpressionProvider,
                            maskingTrafficApplier,
                        ),
                        RouteLineValueCommandHolder(
                            maskingRestrictedExpressionProvider,
                            maskingRestrictedApplier,
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            maskingTrailExpressionProvider,
                            maskingTrailApplier,
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            maskingTrailCasingExpressionProvider,
                            maskingTrailCasingApplier,
                        ),
                    ),
                ),
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val view = MapboxRouteLineView(options)
        view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
        view.renderRouteLineUpdate(style, state)

        coVerifyOrder {
            primaryTrailCasingExpressionProvider.generateCommand(any(), any())
            primaryTrailExpressionProvider.generateCommand(any(), any())
            primaryCasingExpressionProvider.generateCommand(any(), any())
            primaryBaseExpressionProvider.generateCommand(any(), any())
            primaryTrafficExpressionProvider.generateCommand(any(), any())
            primaryRestrictedExpressionProvider.generateCommand(any(), any())

            maskingRestrictedExpressionProvider.generateCommand(any(), any())
            maskingTrafficExpressionProvider.generateCommand(any(), any())
            maskingBaseExpressionProvider.generateCommand(any(), any())
            maskingCasingExpressionProvider.generateCommand(any(), any())
            maskingTrailExpressionProvider.generateCommand(any(), any())
            maskingTrailCasingExpressionProvider.generateCommand(any(), any())

            primaryTrailCasingApplier.applyCommand(any(), any(), any())
            primaryTrailApplier.applyCommand(any(), any(), any())
            primaryCasingApplier.applyCommand(any(), any(), any())
            primaryBaseApplier.applyCommand(any(), any(), any())
            primaryTrafficApplier.applyCommand(any(), any(), any())
            primaryRestrictedApplier.applyCommand(any(), any(), any())

            maskingRestrictedApplier.applyCommand(any(), any(), any())
            maskingTrafficApplier.applyCommand(any(), any(), any())
            maskingBaseApplier.applyCommand(any(), any(), any())
            maskingCasingApplier.applyCommand(any(), any(), any())
            maskingTrailApplier.applyCommand(any(), any(), any())
            maskingTrailCasingApplier.applyCommand(any(), any(), any())
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate_whenPrimaryRouteLineDataIsNull() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val trafficLineExp = mockk<Expression>()
        val routeLineExp = mockk<Expression>()
        val casingLineEx = mockk<Expression>()
        val restrictedRoadExp = mockk<Expression>()
        val trailExpression = mockk<Expression>()
        val trailCasingExpression = mockk<Expression>()
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = null,
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trafficLineExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    restrictedRoadExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailExpression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailCasingExpression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
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
                trafficLineExp,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                restrictedRoadExp,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                trailExpression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                trailExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression,
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineTrimUpdate() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineTrimCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
                            },
                            LineTrimCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trafficLineExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineTrimCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    restrictedRoadExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineTrimCommandApplier(),
                        ),
                        RouteLineTrimOffset(.5),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(trailExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineTrimCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailCasingExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineTrimCommandApplier(),
                        ),
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                    ),
                    routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trafficLineExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    restrictedRoadExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(trailExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailCasingExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
            )
        val style = getMockedStyle()
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val view = MapboxRouteLineView(options)
        view.initPrimaryRouteLineLayerGroup(MapboxRouteLineUtils.layerGroup1SourceLayerIds)
        view.renderRouteLineUpdate(style, state)

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-trim-end",
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-trim-end",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-trim-end",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-trim-end",
                restrictedRoadExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-trim-end",
                trailExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-trim-end",
                trailCasingExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                trailExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                trailCasingExp,
            )
        }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun renderTraveledRouteLineUpdate_maskingDataIsNull() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(routeLineExp, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        casingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(casingLineEx, StylePropertyValueKind.EXPRESSION)
                            },
                            LineGradientCommandApplier(),
                        ),
                        trafficExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trafficLineExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        restrictedSectionExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    restrictedRoadExp,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailExpression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    trailCasingExpression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                        RouteLineDynamicData(
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                            unsupportedRouteLineCommandHolder(),
                        ),
                    ),
                    routeLineMaskingLayerDynamicData = null,
                ),
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
                trafficLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                routeLineExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                casingLineEx,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                restrictedRoadExp,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                trailExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                trailCasingExpression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                any(),
                any(),
            )
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                any(),
                any(),
            )
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                any(),
                any(),
            )
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                any(),
                any(),
            )
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                any(),
                any(),
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineTrimOffset(9.9),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.0),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.1),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineTrimOffset(9.9),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                ),
                callouts = RouteCalloutData(emptyList()),
            ),
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
            wayPointSource,
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

        MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            .renderRouteDrawData(style, state)
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        verify(exactly = 1) {
            primaryRouteSource.featureCollection(
                primaryRouteFeatureCollection,
                primaryDataId.toString(),
            )
        }
        verify(exactly = 1) {
            altRoute1Source.featureCollection(
                alternativeRoute1FeatureCollection,
                alt1DataId.toString(),
            )
        }
        verify(exactly = 1) {
            altRoute2Source.featureCollection(
                alternativeRoute2FeatureCollection,
                alt2DataId.toString(),
            )
        }
        verify(exactly = 1) { wayPointSource.featureCollection(waypointsFeatureCollection) }

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-gradient",
                layerGroup1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-gradient",
                layerGroup2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-gradient",
                layerGroup2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-gradient",
                layerGroup2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-gradient",
                layerGroup2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-gradient",
                layerGroup2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_RESTRICTED,
                "line-gradient",
                layerGroup2Expression,
            )
        }

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-gradient",
                layerGroup3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-gradient",
                layerGroup3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-gradient",
                layerGroup3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-gradient",
                layerGroup3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-gradient",
                layerGroup3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_RESTRICTED,
                "line-gradient",
                layerGroup3Expression,
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-gradient",
                maskingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-gradient",
                maskingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-gradient",
                maskingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-gradient",
                maskingExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-gradient",
                maskingExpression,
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_RESTRICTED,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_RESTRICTED,
                "line-trim-offset",
                expectedRoute2Expression,
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_RESTRICTED,
                "line-trim-offset",
                expectedRoute3Expression,
            )
        }

        verify(exactly = 0) {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify(exactly = 0) {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-trim-offset",
                expectedRoute1Expression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-trim-offset",
                expectedRoute1Expression,
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
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-width",
                options.scaleExpressions.routeTrafficLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-width",
                options.scaleExpressions.alternativeRouteTrafficLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-width",
                options.scaleExpressions.alternativeRouteTrafficLineScaleExpression,
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-width",
                options.scaleExpressions.routeTrafficLineScaleExpression,
            )
        }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun renderRouteDrawDataExecutionOrder() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val primaryRouteFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("1")))
        val alternativeRoute1FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("2")))
        val alternativeRoute2FeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("3")))
        val waypointsFeatureCollection =
            FeatureCollection.fromFeatures(listOf(getEmptyFeature(UUID.randomUUID().toString())))

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

        val primaryBaseExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val primaryTrafficExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val primaryCasingExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val primaryRestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val primaryTrailExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val primaryTrailCasingExpressionProvider = mockk<RouteLineValueProvider>(
            relaxUnitFun = true,
        ) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val primaryBaseApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)
        val primaryTrafficApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)
        val primaryCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)
        val primaryRestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)
        val primaryTrailApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)
        val primaryTrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxUnitFun = true)

        val alt1BaseExpressionProvider = mockk<RouteLineValueProvider>(relaxUnitFun = true) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val alt1TrafficExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt1CasingExpressionProvider = mockk<RouteLineValueProvider>(relaxUnitFun = true) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val alt1RestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt1TrailExpressionProvider = mockk<RouteLineValueProvider>(relaxUnitFun = true) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val alt1TrailCasingExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt1BaseApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt1TrafficApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt1CasingApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt1RestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt1TrailApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt1TrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)

        val alt2BaseExpressionProvider = mockk<RouteLineValueProvider>(relaxUnitFun = true) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val alt2TrafficExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt2CasingExpressionProvider = mockk<RouteLineValueProvider>(relaxUnitFun = true) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val alt2RestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt2TrailExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val alt2TrailCasingExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }

        val alt2BaseApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt2TrafficApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt2CasingApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt2RestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt2TrailApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val alt2TrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)

        val maskingBaseExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val maskingTrafficExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val maskingCasingExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val maskingRestrictedExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val maskingTrailExpressionProvider =
            mockk<RouteLineValueProvider>(relaxUnitFun = true) {
                coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
            }
        val maskingTrailCasingExpressionProvider = mockk<RouteLineValueProvider>(
            relaxUnitFun = true,
        ) {
            coEvery { generateCommand(any(), any()) } returns mockk(relaxed = true)
        }
        val maskingBaseApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrafficApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingRestrictedApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrailApplier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)
        val maskingTrailCasingApplier =
            mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true)

        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteFeatureCollection,
                    RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            primaryBaseExpressionProvider,
                            primaryBaseApplier,
                        ),
                        RouteLineValueCommandHolder(
                            primaryCasingExpressionProvider,
                            primaryCasingApplier,
                        ),
                        RouteLineValueCommandHolder(
                            primaryTrafficExpressionProvider,
                            primaryTrafficApplier,
                        ),
                        RouteLineValueCommandHolder(
                            primaryRestrictedExpressionProvider,
                            primaryRestrictedApplier,
                        ),
                        RouteLineTrimOffset(9.9),
                        RouteLineValueCommandHolder(
                            primaryTrailExpressionProvider,
                            primaryTrailApplier,
                        ),
                        RouteLineValueCommandHolder(
                            primaryTrailCasingExpressionProvider,
                            primaryTrailCasingApplier,
                        ),
                    ),
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                alt1BaseExpressionProvider,
                                alt1BaseApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt1CasingExpressionProvider,
                                alt1CasingApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt1TrafficExpressionProvider,
                                alt1TrafficApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt1RestrictedExpressionProvider,
                                alt1RestrictedApplier,
                            ),
                            RouteLineTrimOffset(0.0),
                            RouteLineValueCommandHolder(
                                alt1TrailExpressionProvider,
                                alt1TrailApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt1TrailCasingExpressionProvider,
                                alt1TrailCasingApplier,
                            ),
                        ),
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                alt2BaseExpressionProvider,
                                alt2BaseApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt2CasingExpressionProvider,
                                alt2CasingApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt2TrafficExpressionProvider,
                                alt2TrafficApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt2RestrictedExpressionProvider,
                                alt2RestrictedApplier,
                            ),
                            RouteLineTrimOffset(0.0),
                            RouteLineValueCommandHolder(
                                alt2TrailExpressionProvider,
                                alt2TrailApplier,
                            ),
                            RouteLineValueCommandHolder(
                                alt2TrailCasingExpressionProvider,
                                alt2TrailCasingApplier,
                            ),
                        ),
                    ),
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    RouteLineValueCommandHolder(
                        maskingBaseExpressionProvider,
                        maskingBaseApplier,
                    ),
                    RouteLineValueCommandHolder(
                        maskingCasingExpressionProvider,
                        maskingCasingApplier,
                    ),
                    RouteLineValueCommandHolder(
                        maskingTrafficExpressionProvider,
                        maskingTrafficApplier,
                    ),
                    RouteLineValueCommandHolder(
                        maskingRestrictedExpressionProvider,
                        maskingRestrictedApplier,
                    ),
                    RouteLineTrimOffset(9.9),
                    RouteLineValueCommandHolder(
                        maskingTrailExpressionProvider,
                        maskingTrailApplier,
                    ),
                    RouteLineValueCommandHolder(
                        maskingTrailCasingExpressionProvider,
                        maskingTrailCasingApplier,
                    ),
                ),
                callouts = RouteCalloutData(emptyList()),
            ),
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
            wayPointSource,
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

        MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            .renderRouteDrawData(style, state)
        verify { MapboxRouteLineUtils.initializeLayers(style, options) }

        coVerifyOrder {
            primaryTrailCasingExpressionProvider.generateCommand(any(), any())
            primaryTrailExpressionProvider.generateCommand(any(), any())
            primaryCasingExpressionProvider.generateCommand(any(), any())
            primaryBaseExpressionProvider.generateCommand(any(), any())
            primaryTrafficExpressionProvider.generateCommand(any(), any())
            primaryRestrictedExpressionProvider.generateCommand(any(), any())

            maskingTrailCasingExpressionProvider.generateCommand(any(), any())
            maskingTrailExpressionProvider.generateCommand(any(), any())
            maskingCasingExpressionProvider.generateCommand(any(), any())
            maskingBaseExpressionProvider.generateCommand(any(), any())
            maskingTrafficExpressionProvider.generateCommand(any(), any())
            maskingRestrictedExpressionProvider.generateCommand(any(), any())

            alt1TrailCasingExpressionProvider.generateCommand(any(), any())
            alt1TrailExpressionProvider.generateCommand(any(), any())
            alt1CasingExpressionProvider.generateCommand(any(), any())
            alt1BaseExpressionProvider.generateCommand(any(), any())
            alt1TrafficExpressionProvider.generateCommand(any(), any())
            alt1RestrictedExpressionProvider.generateCommand(any(), any())

            alt2TrailCasingExpressionProvider.generateCommand(any(), any())
            alt2TrailExpressionProvider.generateCommand(any(), any())
            alt2CasingExpressionProvider.generateCommand(any(), any())
            alt2BaseExpressionProvider.generateCommand(any(), any())
            alt2TrafficExpressionProvider.generateCommand(any(), any())
            alt2RestrictedExpressionProvider.generateCommand(any(), any())

            wayPointSource.featureCollection(any(), any())
            primaryRouteSource.featureCollection(any(), any())
            altRoute1Source.featureCollection(any(), any())
            altRoute2Source.featureCollection(any(), any())

            primaryRestrictedApplier.applyCommand(any(), any(), any())
            primaryTrafficApplier.applyCommand(any(), any(), any())
            primaryBaseApplier.applyCommand(any(), any(), any())
            primaryCasingApplier.applyCommand(any(), any(), any())
            primaryTrailApplier.applyCommand(any(), any(), any())
            primaryTrailCasingApplier.applyCommand(any(), any(), any())

            maskingTrailCasingApplier.applyCommand(any(), any(), any())
            maskingTrailApplier.applyCommand(any(), any(), any())
            maskingCasingApplier.applyCommand(any(), any(), any())
            maskingBaseApplier.applyCommand(any(), any(), any())
            maskingTrafficApplier.applyCommand(any(), any(), any())
            maskingRestrictedApplier.applyCommand(any(), any(), any())

            alt1RestrictedApplier.applyCommand(any(), any(), any())
            alt1TrafficApplier.applyCommand(any(), any(), any())
            alt1BaseApplier.applyCommand(any(), any(), any())
            alt1CasingApplier.applyCommand(any(), any(), any())
            alt1TrailApplier.applyCommand(any(), any(), any())
            alt1TrailCasingApplier.applyCommand(any(), any(), any())

            alt2RestrictedApplier.applyCommand(any(), any(), any())
            alt2TrafficApplier.applyCommand(any(), any(), any())
            alt2BaseApplier.applyCommand(any(), any(), any())
            alt2CasingApplier.applyCommand(any(), any(), any())
            alt2TrailApplier.applyCommand(any(), any(), any())
            alt2TrailCasingApplier.applyCommand(any(), any(), any())
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
                RouteLineScaleValue(22f, 18f, 1.5f),
            ),
        )

        val alternativeRouteTrafficLineScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.5f),
                RouteLineScaleValue(10f, 4f, 0.5f),
                RouteLineScaleValue(13f, 6f, 0.5f),
                RouteLineScaleValue(16f, 10f, 0.5f),
                RouteLineScaleValue(19f, 14f, 0.5f),
                RouteLineScaleValue(22f, 18f, 0.5f),
            ),
        )

        val routeLineCasingScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.9f),
                RouteLineScaleValue(10f, 4f, 0.9f),
                RouteLineScaleValue(13f, 6f, 0.9f),
                RouteLineScaleValue(16f, 10f, 0.9f),
                RouteLineScaleValue(19f, 14f, 0.9f),
                RouteLineScaleValue(22f, 18f, 0.9f),
            ),
        )
        val alternativeRouteLineCasingScaleExpression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 0.2f),
                RouteLineScaleValue(10f, 4f, 0.2f),
                RouteLineScaleValue(13f, 6f, 0.2f),
                RouteLineScaleValue(16f, 10f, 0.2f),
                RouteLineScaleValue(19f, 14f, 0.2f),
                RouteLineScaleValue(22f, 18f, 0.2f),
            ),
        )

        val options = MapboxRouteLineViewOptions.Builder(ctx)
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
                    .build(),
            )
            .scaleExpressions(
                RouteLineScaleExpressions.Builder()
                    .routeLineScaleExpression(routeTrafficLineScaleExpression)
                    .routeTrafficLineScaleExpression(routeTrafficLineScaleExpression)
                    .routeCasingLineScaleExpression(routeLineCasingScaleExpression)
                    .alternativeRouteTrafficLineScaleExpression(
                        alternativeRouteTrafficLineScaleExpression,
                    )
                    .alternativeRouteLineScaleExpression(alternativeRouteTrafficLineScaleExpression)
                    .alternativeRouteCasingLineScaleExpression(
                        alternativeRouteLineCasingScaleExpression,
                    )
                    .build(),
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineTrimOffset(9.9),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.0),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.1),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineTrimOffset(9.9),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                ),
                callouts = RouteCalloutData(emptyList()),
            ),
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
            wayPointSource,
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

        MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            .renderRouteDrawData(style, state)

        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAIL,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_MAIN,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_1_TRAFFIC,
                "line-width",
                options.scaleExpressions.routeTrafficLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAIL,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_MAIN,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_2_TRAFFIC,
                "line-width",
                options.scaleExpressions.alternativeRouteTrafficLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAIL,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_CASING,
                "line-width",
                options.scaleExpressions.alternativeRouteCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_MAIN,
                "line-width",
                options.scaleExpressions.alternativeRouteLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                LAYER_GROUP_3_TRAFFIC,
                "line-width",
                options.scaleExpressions.alternativeRouteTrafficLineScaleExpression,
            )
        }

        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAIL,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_CASING,
                "line-width",
                options.scaleExpressions.routeCasingLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_MAIN,
                "line-width",
                options.scaleExpressions.routeLineScaleExpression,
            )
        }
        verify {
            style.setStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "line-width",
                options.scaleExpressions.routeTrafficLineScaleExpression,
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineTrimOffset(9.9),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup1Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
            ),
        )
        val atlRouteLine1 = RouteLineData(
            alternativeRoute1FeatureCollection,
            RouteLineDynamicData(
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineTrimOffset(0.0),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup2Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
            ),
        )
        val atlRouteLine2 = RouteLineData(
            alternativeRoute2FeatureCollection,
            RouteLineDynamicData(
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineTrimOffset(0.1),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
                RouteLineValueCommandHolder(
                    LightRouteLineValueProvider {
                        StylePropertyValue(
                            layerGroup3Expression,
                            StylePropertyValueKind.EXPRESSION,
                        )
                    },
                    LineGradientCommandApplier(),
                ),
            ),
        )
        val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = primaryRouteLine,
                alternativeRouteLinesData = listOf(atlRouteLine1, atlRouteLine2),
                waypointsFeatureCollection,
                callouts = RouteCalloutData(emptyList()),
            ),
        )
        val state2: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = atlRouteLine1,
                alternativeRouteLinesData = listOf(primaryRouteLine, atlRouteLine2),
                waypointsFeatureCollection,
                callouts = RouteCalloutData(emptyList()),
            ),
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
            wayPointSource,
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
            wayPointSource,
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

        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
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
            LAYER_GROUP_1_BLUR,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
            LAYER_GROUP_1_RESTRICTED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
            LAYER_GROUP_1_BLUR,
            LAYER_GROUP_1_CASING,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
        val route1Blur = mockk<LineLayer>(relaxed = true)
        val route2Blur = mockk<LineLayer>(relaxed = true)
        val route3Blur = mockk<LineLayer>(relaxed = true)
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
            every {
                getLayer(LAYER_GROUP_1_BLUR)
            } returns route1Blur
            every {
                getLayer(LAYER_GROUP_2_BLUR)
            } returns route2Blur
            every {
                getLayer(LAYER_GROUP_3_BLUR)
            } returns route3Blur
        }
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        MapboxRouteLineView(options).showAlternativeRoutes(style)

        verify(exactly = 0) { route1TrailCasing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Trail.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Casing.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Main.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Traffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Restricted.visibility(Visibility.VISIBLE) }
        verify(exactly = 0) { route1Blur.visibility(Visibility.VISIBLE) }
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
        verify { route3Blur.visibility(Visibility.VISIBLE) }
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
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
        val blur1 = mockk<LineLayer>(relaxed = true)
        val blur2 = mockk<LineLayer>(relaxed = true)
        val blur3 = mockk<LineLayer>(relaxed = true)
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
            every {
                getLayer(LAYER_GROUP_1_BLUR)
            } returns blur1
            every {
                getLayer(LAYER_GROUP_2_BLUR)
            } returns blur2
            every {
                getLayer(LAYER_GROUP_3_BLUR)
            } returns blur3
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
        verify { blur3.visibility(Visibility.NONE) }
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun hideTraffic() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineBlurEnabled(true)
            .applyTrafficColorsToRouteLineBlur(true)
            .build()
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
        val blurLine1 = mockk<LineLayer>(relaxed = true)
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
                getLayer(LAYER_GROUP_1_BLUR)
            } returns blurLine1
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_1_BLUR) } returns true
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
        verify(exactly = 1) { blurLine1.visibility(Visibility.NONE) }

        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun showTraffic() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineBlurEnabled(true)
            .applyTrafficColorsToRouteLineBlur(true)
            .build()
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
        val blurLine1 = mockk<LineLayer>(relaxed = true)
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
                getLayer(LAYER_GROUP_1_BLUR)
            } returns blurLine1
            every { getLayer(MASKING_LAYER_TRAIL_CASING) } returns maskingTrailCasing
            every { getLayer(MASKING_LAYER_TRAIL) } returns maskingTrail
            every { getLayer(MASKING_LAYER_CASING) } returns maskingCasing
            every { getLayer(MASKING_LAYER_MAIN) } returns maskingMain
            every { getLayer(MASKING_LAYER_TRAFFIC) } returns maskingTraffic
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_1_BLUR) } returns true
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
        verify(exactly = 2) { maskingTraffic.visibility(Visibility.VISIBLE) }
        verify(exactly = 1) { blurLine1.visibility(Visibility.VISIBLE) }

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
            LAYER_GROUP_1_BLUR,
            LAYER_GROUP_1_MAIN,
            LAYER_GROUP_1_TRAFFIC,
            LAYER_GROUP_1_RESTRICTED,
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_1_TRAFFIC,
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
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
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_1_MAIN,
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
        )
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any())
        } returns layerGroup1SourceLayerIds

        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val style = getMockedStyle()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                LAYER_GROUP_2_MAIN,
            )
        } returns Visibility.VISIBLE
        every { MapboxRouteLineUtils.initializeLayers(style, options) } just Runs

        val result = MapboxRouteLineView(options).getAlternativeRoutesVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun routesRenderedNotification() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(MapboxRouteLineUtils)
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.navigation.ui.maps.util.RoutesRenderedCallbackExtensionsKt")

        val mapboxMap = mockk<MapboxMap>(relaxed = true)
        val delayedCallback = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
        val callback = mockk<RoutesRenderedCallback>(relaxed = true) {
            every { toDelayedRoutesRenderedCallback() } returns delayedCallback
        }
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        every { MapboxRouteLineUtils.initializeLayers(any(), options) } just Runs
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

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
            "routeId#2",
        )
        val style = mapboxMap.getStyle()!!
        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#2", style)

        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#2", style)

        verifyOrder {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#2"),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#3", style)

        verifyOrder {
            routesExpector.expectRoutes(
                setOf("routeId#0", "routeId#1", "routeId#3"),
                setOf("routeId#2"),
                match(
                    matchExpectedRoutes(
                        setOf(Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#3")),
                        setOf(Triple(LAYER_GROUP_3_SOURCE_ID, alt2DataId, "routeId#2")),
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verifyOrder {
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
                        ),
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }

        clearAllMocks(answers = false)

        clearRoutes(view, mapboxMap, callback, style)

        verifyOrder {
            routesExpector.expectRoutes(
                emptySet(),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        view.hidePrimaryRoute(style)

        renderRoutes(view, mapboxMap, callback, "routeId#0", "routeId#1", "routeId#3", style)

        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }
        clearAllMocks(answers = false)

        view.showPrimaryRoute(style)
        view.hideAlternativeRoutes(style)
        clearRoutes(view, mapboxMap, callback, style)
        verifyOrder {
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
                        ),
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
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
            style,
        )
        verifyOrder {
            routesExpector.expectRoutes(
                emptySet(),
                emptySet(),
                match(matchExpectedRoutes(emptySet(), emptySet())),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
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
            style,
        )
        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }

        clearRoutes(
            view,
            mapboxMap,
            callback,
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#0"))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#1"))),
            FeatureCollection.fromFeatures(listOf(getEmptyFeature("routeId#2"))),
            style,
        )
        verifyOrder {
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
                    ),
                ),
                RoutesRenderedCallbackWrapper(mapboxMap, delayedCallback),
            )
            delayedCallback.unlock()
        }

        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        unmockkObject(MapboxRouteLineUtils)
        unmockkStatic("com.mapbox.navigation.ui.maps.util.RoutesRenderedCallbackExtensionsKt")
    }

    @Test
    fun cancel() = runBlocking {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
        val job = scope.launch { delay(10000) }
        assertTrue(scope.isActive)

        view.cancel()

        assertTrue(scope.isActive)
        assertTrue(job.isCancelled)
    }

    @Test
    fun updateDynamicOptionsSame() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils") {
            mockkObject(MapboxRouteLineUtils) {
                every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
                val style = getMockedStyle()
                val options = MapboxRouteLineViewOptions.Builder(ctx).build()
                val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

                view.updateDynamicOptions(
                    style,
                    {
                        softGradientTransition(30.0)
                    },
                )

                verify(exactly = 0) {
                    MapboxRouteLineUtils.updateLayersStyling(any(), any())
                }

                view.initializeLayers(style)
                verify {
                    MapboxRouteLineUtils.initializeLayers(style, options)
                }
            }
        }
    }

    @Test
    fun updateDynamicOptionsChanged() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils") {
            mockkObject(MapboxRouteLineUtils) {
                every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
                every { MapboxRouteLineUtils.updateLayersStyling(any(), any()) } answers {}
                val style = getMockedStyle()
                val options = MapboxRouteLineViewOptions.Builder(ctx).build()
                val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

                view.updateDynamicOptions(
                    style,
                    {
                        softGradientTransition(40.0)
                    },
                )

                val expectedOptions = options.toBuilder()
                    .softGradientTransition(40.0)
                    .build()

                verify(exactly = 1) {
                    MapboxRouteLineUtils.updateLayersStyling(style, expectedOptions)
                }

                view.initializeLayers(style)
                verify {
                    MapboxRouteLineUtils.initializeLayers(style, expectedOptions)
                }
            }
        }
    }

    @Test
    fun initSendsOptionsEvent() {
        mockkObject(MapboxNavigationProvider) {
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            verify { sender.sendInitialOptionsEvent(options.toData()) }
        }
    }

    @Test
    fun updateDynamicOptionsSendsEvent() {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.updateLayersStyling(any(), any()) } answers { }
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            view.updateDynamicOptions(style) {
                softGradientTransition(40.0)
            }

            val newOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .softGradientTransition(40.0)
                .build()
                .toData()

            verify { sender.sendUpdateDynamicOptionsEvent(styleId, newOptions) }
        }
    }

    @Test
    fun initializeLayersSendsEvent() {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

            view.initializeLayers(style)

            verify { sender.sendInitializeLayersEvent(styleId) }
        }
    }

    @Test
    fun renderRouteDrawDataValueSendsEvents() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
            every { MapboxRouteLineUtils.getLayerVisibility(any(), any()) } returns null
            val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createValue(
                RouteSetValue(
                    primaryRouteLineData = RouteLineData(
                        FeatureCollection.fromFeatures(listOf(getEmptyFeature("1"))),
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(9.9),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider { mockk(relaxed = true) },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                    alternativeRouteLinesData = emptyList(),
                    FeatureCollection.fromFeatures(listOf(getEmptyFeature("1"))),
                    routeLineMaskingLayerDynamicData = null,
                    callouts = RouteCalloutData(emptyList()),
                ),
            )

            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            view.renderRouteDrawData(style, state)

            verifyOrder {
                sender.sendInitialOptionsEvent(options.toData())
                sender.sendRenderRouteDrawDataEvent(styleId, state)
            }
        }
    }

    @Test
    fun renderRouteDrawDataErrorSendsEvent() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
            every { MapboxRouteLineUtils.getLayerVisibility(any(), any()) } returns null
            val state: Expected<RouteLineError, RouteSetValue> = ExpectedFactory.createError(
                RouteLineError("some error", null),
            )

            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            view.renderRouteDrawData(style, state)

            verify { sender.sendRenderRouteDrawDataEvent(styleId, state) }
        }
    }

    @Test
    fun renderRouteLineUpdateValueSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                        casingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                        trafficExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                        restrictedSectionExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                        trailExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                        trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                            LightRouteLineValueProvider { mockk() },
                            LineGradientCommandApplier(),
                        ),
                    ),
                    alternativeRouteLinesDynamicData = emptyList(),
                    routeLineMaskingLayerDynamicData = null,
                ),
            )
        view.renderRouteLineUpdate(style, state)

        verify { sender.sendRenderRouteLineUpdateEvent(styleId, state) }
    }

    @Test
    fun renderRouteLineUpdateErrorSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
        val state: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createError(RouteLineError("some error", null))
        view.renderRouteLineUpdate(style, state)

        verify(exactly = 1) { sender.sendRenderRouteLineUpdateEvent(styleId, state) }
    }

    @Test
    fun renderRouteLineClearValueSendsEvent() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            val state: Expected<RouteLineError, RouteLineClearValue> =
                ExpectedFactory.createValue(
                    RouteLineClearValue(
                        FeatureCollection.fromFeatures(listOf(getEmptyFeature("1"))),
                        emptyList(),
                        FeatureCollection.fromFeatures(listOf(getEmptyFeature("2"))),
                        RouteCalloutData(emptyList()),
                    ),
                )
            view.renderClearRouteLineValue(style, state)

            verify { sender.sendClearRouteLineValueEvent(styleId, state) }
        }
    }

    @Test
    fun renderRouteLineClearErrorSendsEvent() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils) {
            every { MapboxRouteLineUtils.initializeLayers(any(), any()) } answers {}
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)
            val state: Expected<RouteLineError, RouteLineClearValue> =
                ExpectedFactory.createError(
                    RouteLineError("some error", null),
                )
            view.renderClearRouteLineValue(style, state)

            verify { sender.sendClearRouteLineValueEvent(styleId, state) }
        }
    }

    @Test
    fun cancelSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.cancel()
        verify { sender.sendCancelEvent() }
    }

    @Test
    fun showPrimaryRouteSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.showPrimaryRoute(style)
        verify { sender.sendShowPrimaryRouteEvent(styleId) }
    }

    @Test
    fun hidePrimaryRouteSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.hidePrimaryRoute(style)
        verify { sender.sendHidePrimaryRouteEvent(styleId) }
    }

    @Test
    fun showAlternativeRoutesSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.showAlternativeRoutes(style)
        verify { sender.sendShowAlternativeRoutesEvent(styleId) }
    }

    @Test
    fun hideAlternativeRoutesSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.hideAlternativeRoutes(style)
        verify { sender.sendHideAlternativeRoutesEvent(styleId) }
    }

    @Test
    fun showTrafficSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.showTraffic(style)
        verify { sender.sendShowTrafficEvent(styleId) }
    }

    @Test
    fun hideTrafficSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.hideTraffic(style)
        verify { sender.sendHideTrafficEvent(styleId) }
    }

    @Test
    fun showOriginAndDestinationSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.showOriginAndDestinationPoints(style)
        verify { sender.sendShowOriginAndDestinationPointsEvent(styleId) }
    }

    @Test
    fun hideOriginAndDestinationSendsEvent() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val view = MapboxRouteLineView(options, routesExpector, dataIdHolder, sender)

        view.hideOriginAndDestinationPoints(style)
        verify { sender.sendHideOriginAndDestinationPointsEvent(styleId) }
    }

    @Test
    fun referencedOptionsAreNotLeaked() = coroutineRule.runBlockingTest {
        var options: MapboxRouteLineViewOptions? = MapboxRouteLineViewOptions.Builder(ctx).build()
        val weakOptions: WeakReference<MapboxRouteLineViewOptions> = WeakReference(options!!)
        var view: MapboxRouteLineView? = MapboxRouteLineView(options)

        options = null
        view = null
        System.gc()

        Assert.assertNull(weakOptions.get())
    }

    private fun matchExpectedRoutes(
        expectedRendered: Set<Triple<String, Int, String>>,
        expectedCleared: Set<Triple<String, Int, String>>,
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
        style: Style? = null,
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
        style: Style? = null,
    ) {
        val style = if (style == null) {
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
            getMockedStyle(
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
                wayPointSource,
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
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineTrimOffset(9.9),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                        RouteLineValueCommandHolder(
                            LightRouteLineValueProvider {
                                StylePropertyValue(
                                    layerGroup1Expression,
                                    StylePropertyValueKind.EXPRESSION,
                                )
                            },
                            LineGradientCommandApplier(),
                        ),
                    ),
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.0),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup2Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineTrimOffset(0.1),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                            RouteLineValueCommandHolder(
                                LightRouteLineValueProvider {
                                    StylePropertyValue(
                                        layerGroup3Expression,
                                        StylePropertyValueKind.EXPRESSION,
                                    )
                                },
                                LineGradientCommandApplier(),
                            ),
                        ),
                    ),
                ),
                waypointsFeatureCollection,
                routeLineMaskingLayerDynamicData = RouteLineDynamicData(
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineTrimOffset(9.9),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(
                                maskingExpression,
                                StylePropertyValueKind.EXPRESSION,
                            )
                        },
                        LineGradientCommandApplier(),
                    ),
                ),
                callouts = RouteCalloutData(emptyList()),
            ),
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
        style: Style? = null,
    ) {
        clearRoutes(
            mapboxRouteLineView,
            map,
            callback,
            FeatureCollection.fromFeatures(emptyList()),
            FeatureCollection.fromFeatures(emptyList()),
            FeatureCollection.fromFeatures(emptyList()),
            style,
        )
    }

    private fun clearRoutes(
        mapboxRouteLineView: MapboxRouteLineView,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
        primaryRouteFeatureCollection: FeatureCollection,
        alternativeRoute1FeatureCollection: FeatureCollection,
        alternativeRoute2FeatureCollection: FeatureCollection,
        style: Style? = null,
    ) {
        val waypointsFeatureCollection = FeatureCollection.fromFeatures(emptyList())
        val style = if (style == null) {
            val topLevelRouteLayer = StyleObjectInfo(
                TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                "background",
            )
            val bottomLevelRouteLayer = StyleObjectInfo(
                BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                "background",
            )
            val mainLayer = StyleObjectInfo(
                LAYER_GROUP_1_MAIN,
                "line",
            )
            mockk<Style> {
                every { getSource(any()) } returns mockk<GeoJsonSource>(relaxed = true)
                every { styleLayers } returns listOf(
                    bottomLevelRouteLayer,
                    mainLayer,
                    topLevelRouteLayer,
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
                waypointsFeatureCollection,
                RouteCalloutData(emptyList()),
            ),
        )

        mapboxRouteLineView.renderClearRouteLineValue(style, state, map, callback)
    }

    private fun getEmptyFeature(featureId: String): Feature {
        return Feature.fromJson(
            "{\"type\":\"Feature\",\"id\":\"${featureId}\"," +
                "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]}}",
        )
    }

    private fun getMockedStyle(): Style = mockk(relaxed = true) {
        every { sdkStyleManager } returns mockk(relaxed = true) {
            every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
            every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
        }
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
        wayPointSource: GeoJsonSource,
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
            every { sdkStyleManager } returns mockk(relaxed = true) {
                every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
                every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
            }
        }.also {
            mockCheckForLayerInitialization(it)
        }
    }
}
