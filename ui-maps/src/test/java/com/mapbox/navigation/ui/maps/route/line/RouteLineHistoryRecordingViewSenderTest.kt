package com.mapbox.navigation.ui.maps.route.line

import androidx.appcompat.content.res.AppCompatResources
import com.google.gson.JsonParser
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.registerObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineDynamicEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineProviderBasedExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewRenderRouteDrawDataInputValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewRenderRouteLineUpdateDataValue
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.internal.route.line.toEventValue
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.util.MutexBasedScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteLineHistoryRecordingViewSenderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var options: MapboxRouteLineViewOptions
    private lateinit var optionsData: RouteLineViewOptionsData
    private val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)

    private lateinit var pusher: RouteLineHistoryRecordingPusher
    private lateinit var sender: RouteLineHistoryRecordingViewSender
    private val observerSlot = slot<(MapboxHistoryRecorder?) -> Unit>()
    private val historyRecorderChooserFactory =
        mockk<HistoryRecorderChooserFactory>(relaxed = true) {
            every { create(any(), any()) } returns mockk(relaxed = true)
        }
    private val styleId = "someId"

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationProvider)
        pusher = RouteLineHistoryRecordingPusher(
            coroutineRule.testDispatcher,
            MutexBasedScope(coroutineRule.coroutineScope),
            historyRecorderChooserFactory,
        )

        mockkObject(RouteLineHistoryRecordingPusherProvider)
        every { RouteLineHistoryRecordingPusherProvider.instance } returns pusher
        sender = RouteLineHistoryRecordingViewSender()
        mockkStatic("com.mapbox.navigation.ui.maps.internal.route.line.RouteLineDataConverterKt")
        mockkStatic("androidx.appcompat.content.res.AppCompatResources")
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()
        options = MapboxRouteLineViewOptions.Builder(mockk(relaxed = true)).routeLineBelowLayerId(
            "some-layer",
        ).build()
        optionsData = options.toData()
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.internal.route.line.RouteLineDataConverterKt")
        unmockkStatic("androidx.appcompat.content.res.AppCompatResources")
        unmockkObject(RouteLineHistoryRecordingPusherProvider)
        unmockkObject(MapboxNavigationProvider)
    }

    @Test
    fun initRegistersObserver() {
        verify { MapboxNavigationProvider.registerObserver(any()) }
    }

    @Test
    fun pushInitialOptionsEventIsAddedToQueue() {
        mockkStatic("androidx.appcompat.content.res.AppCompatResources") {
            /* ktlint-disable max-line-length */
            val expected =
                """{"value":{"initialOptions":{"routeLineColorResources":{"routeDefaultColor":0,"routeLowCongestionColor":0,"routeModerateCongestionColor":0,"routeHeavyCongestionColor":0,"routeSevereCongestionColor":0,"routeUnknownCongestionColor":0,"inactiveRouteLegLowCongestionColor":0,"inactiveRouteLegModerateCongestionColor":0,"inactiveRouteLegHeavyCongestionColor":0,"inactiveRouteLegSevereCongestionColor":0,"inactiveRouteLegUnknownCongestionColor":0,"alternativeRouteDefaultColor":0,"alternativeRouteLowCongestionColor":0,"alternativeRouteModerateCongestionColor":0,"alternativeRouteHeavyCongestionColor":0,"alternativeRouteSevereCongestionColor":0,"alternativeRouteUnknownCongestionColor":0,"restrictedRoadColor":0,"routeClosureColor":0,"inactiveRouteLegRestrictedRoadColor":0,"inactiveRouteLegClosureColor":0,"alternativeRouteRestrictedRoadColor":0,"alternativeRouteClosureColor":0,"routeLineTraveledColor":0,"routeLineTraveledCasingColor":0,"routeCasingColor":0,"alternativeRouteCasingColor":0,"inactiveRouteLegCasingColor":0,"inActiveRouteLegsColor":0,"blurColor":0},"scaleExpressions":{"routeLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","routeCasingLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,1.0],14.0,[\"*\",10.5,1.0],16.5,[\"*\",15.5,1.0],19.0,[\"*\",24.0,1.0],22.0,[\"*\",29.0,1.0]]","routeTrafficLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","alternativeRouteLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","alternativeRouteCasingLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,1.0],14.0,[\"*\",10.5,1.0],16.5,[\"*\",15.5,1.0],19.0,[\"*\",24.0,1.0],22.0,[\"*\",29.0,1.0]]","alternativeRouteTrafficLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","routeBlurScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,2.0],14.0,[\"*\",10.5,2.0],16.5,[\"*\",15.5,2.0],19.0,[\"*\",24.0,2.0],22.0,[\"*\",29.0,2.0]]"},"restrictedRoadDashArray":[0.5,2.0],"restrictedRoadOpacity":1.0,"restrictedRoadLineWidth":7.0,"displaySoftGradientForTraffic":false,"softGradientTransition":30.0,"originIconId":${RouteLayerConstants.ORIGIN_WAYPOINT_ICON},"destinationIconId":${RouteLayerConstants.DESTINATION_WAYPOINT_ICON},"waypointLayerIconOffset":[0.0,0.0],"waypointLayerIconAnchor":{"value":"center"},"iconPitchAlignment":{"value":"map"},"displayRestrictedRoadSections":false,"tolerance":0.375,"shareLineGeometrySources":false,"lineDepthOcclusionFactor":0.0,"slotName":"middle","routeLineBlurWidth":5.0,"routeLineBlurEnabled":false,"applyTrafficColorsToRouteLineBlur":false,"routeLineBlurOpacity":0.4},"action":"initial_options"},"subtype":"view"}"""
            /* ktlint-enable max-line-length */
            every { AppCompatResources.getDrawable(any(), any()) } returns mockk()
            val options = MapboxRouteLineViewOptions.Builder(mockk()).build().toData()
            sender.sendInitialOptionsEvent(options)
            onRecorderEnabled()

            checkEvent(expected)
        }
    }

    @Test
    fun pushUpdateDynamicOptionsEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"newOptions":{"routeLineColorResources":{"routeDefaultColor":0,"routeLowCongestionColor":0,"routeModerateCongestionColor":0,"routeHeavyCongestionColor":0,"routeSevereCongestionColor":0,"routeUnknownCongestionColor":0,"inactiveRouteLegLowCongestionColor":0,"inactiveRouteLegModerateCongestionColor":0,"inactiveRouteLegHeavyCongestionColor":0,"inactiveRouteLegSevereCongestionColor":0,"inactiveRouteLegUnknownCongestionColor":0,"alternativeRouteDefaultColor":0,"alternativeRouteLowCongestionColor":0,"alternativeRouteModerateCongestionColor":0,"alternativeRouteHeavyCongestionColor":0,"alternativeRouteSevereCongestionColor":0,"alternativeRouteUnknownCongestionColor":0,"restrictedRoadColor":0,"routeClosureColor":0,"inactiveRouteLegRestrictedRoadColor":0,"inactiveRouteLegClosureColor":0,"alternativeRouteRestrictedRoadColor":0,"alternativeRouteClosureColor":0,"routeLineTraveledColor":0,"routeLineTraveledCasingColor":0,"routeCasingColor":0,"alternativeRouteCasingColor":0,"inactiveRouteLegCasingColor":0,"inActiveRouteLegsColor":0,"blurColor":0},"scaleExpressions":{"routeLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","routeCasingLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,1.0],14.0,[\"*\",10.5,1.0],16.5,[\"*\",15.5,1.0],19.0,[\"*\",24.0,1.0],22.0,[\"*\",29.0,1.0]]","routeTrafficLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","alternativeRouteLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","alternativeRouteCasingLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,1.0],14.0,[\"*\",10.5,1.0],16.5,[\"*\",15.5,1.0],19.0,[\"*\",24.0,1.0],22.0,[\"*\",29.0,1.0]]","alternativeRouteTrafficLineScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,1.0],10.0,[\"*\",4.0,1.0],13.0,[\"*\",6.0,1.0],16.0,[\"*\",10.0,1.0],19.0,[\"*\",14.0,1.0],22.0,[\"*\",18.0,1.0]]","routeBlurScaleExpression":"[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],10.0,[\"*\",7.0,2.0],14.0,[\"*\",10.5,2.0],16.5,[\"*\",15.5,2.0],19.0,[\"*\",24.0,2.0],22.0,[\"*\",29.0,2.0]]"},"restrictedRoadDashArray":[0.5,2.0],"restrictedRoadOpacity":1.0,"restrictedRoadLineWidth":7.0,"displaySoftGradientForTraffic":false,"softGradientTransition":30.0,"originIconId":${RouteLayerConstants.ORIGIN_WAYPOINT_ICON},"destinationIconId":${RouteLayerConstants.DESTINATION_WAYPOINT_ICON},"waypointLayerIconOffset":[0.0,0.0],"waypointLayerIconAnchor":{"value":"center"},"iconPitchAlignment":{"value":"map"},"displayRestrictedRoadSections":false,"tolerance":0.375,"shareLineGeometrySources":false,"lineDepthOcclusionFactor":0.0,"slotName":"middle","routeLineBlurWidth":5.0,"routeLineBlurEnabled":false,"applyTrafficColorsToRouteLineBlur":false,"routeLineBlurOpacity":0.4},"styleId":"someId","action":"update_dynamic_options"},"subtype":"view"}"""
        /* ktlint-enable max-line-length */
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()
        val options = MapboxRouteLineViewOptions.Builder(mockk()).build().toData()
        onRecorderEnabled()
        sender.sendUpdateDynamicOptionsEvent(styleId, options)

        checkEvent(expected)
    }

    @Test
    fun pushInitializeLayersEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"initialize_layers"},"subtype":"view","instanceId":"04583e43-031e-4e00-bd0b-747d0afa3f9e"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendInitializeLayersEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun pushInitializeLayersEventNullStyleId() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"action":"initialize_layers"},"subtype":"view","instanceId":"04583e43-031e-4e00-bd0b-747d0afa3f9e"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendInitializeLayersEvent(null)

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteDrawDataEventError() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"message":"Some error","type":"error"},"styleId":"someId","action":"render_route_draw_data"},"subtype":"view","instanceId":"710aab1f-508e-4f58-aa6c-4caeed7952ba"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendRenderRouteDrawDataEvent(
            styleId,
            ExpectedFactory.createError(RouteLineError("Some error", null)),
        )

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteDrawDataEventValueNoOptions() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"message":"NoOptions","type":"error"},"styleId":"someId","action":"render_route_draw_data"},"subtype":"view","instanceId":"cbe55dd4-3a15-4cfb-8d74-fbb5cbeb8479"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendRenderRouteDrawDataEvent(styleId, ExpectedFactory.createValue(mockk()))
        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteDrawDataEventValueHasOptions() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"primaryRouteLineData":{"featureCollection":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"1\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","dynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"primary-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"primary-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-gradient","expression":"primary-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"primary-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.1},"trailExpressionData":{"property":"line-trim-offset","expression":"primary-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"primary-trail-casing-exp","type":"provider_based"},"blurExpressionCommandData":{"property":"line-gradient","expression":"primary-blur-exp","type":"provider_based"}}},"alternativeRouteLinesData":[{"featureCollection":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"2\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","dynamicData":{"baseExpressionData":{"property":"line-trim-offset","expression":"alt-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"alt-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"alt-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"alt-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.2},"trailExpressionData":{"property":"line-gradient","expression":"alt-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"alt-trail-casing-exp","type":"provider_based"}}}],"waypointsSource":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"3\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","routeLineMaskingLayerDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"masking-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-trim-offset","expression":"masking-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"masking-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"masking-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.3},"trailExpressionData":{"property":"line-trim-offset","expression":"masking-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"masking-trail-casing-exp","type":"provider_based"}},"type":"value_render_route_draw_data"},"styleId":"someId","action":"render_route_draw_data"},"subtype":"view"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendInitialOptionsEvent(optionsData)
        clearMocks(recorder, answers = false)

        val input = ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
            mockk {
                coEvery {
                    toEventValue(any(), optionsData)
                } returns RouteLineViewRenderRouteDrawDataInputValue(
                    RouteLineEventData(
                        getFeatureCollection("1"),
                        getPrimaryRouteLineEventDynamicData(),
                    ),
                    listOf(
                        RouteLineEventData(
                            getFeatureCollection("2"),
                            getAlternativeRouteLineEventDynamicData(),
                        ),
                    ),
                    getFeatureCollection("3"),
                    getMaskingRouteLineEventDynamicData(),
                )
            },
        )

        sender.sendRenderRouteDrawDataEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteDrawDataEventValueOptionsChanged() {
        val newOptionsData = MapboxRouteLineViewOptions.Builder(mockk(relaxed = true))
            .routeLineBelowLayerId("another-layer")
            .build()
            .toData()
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"primaryRouteLineData":{"featureCollection":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"1\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","dynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"primary-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"primary-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-gradient","expression":"primary-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"primary-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.1},"trailExpressionData":{"property":"line-trim-offset","expression":"primary-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"primary-trail-casing-exp","type":"provider_based"},"blurExpressionCommandData":{"property":"line-gradient","expression":"primary-blur-exp","type":"provider_based"}}},"alternativeRouteLinesData":[{"featureCollection":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"2\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","dynamicData":{"baseExpressionData":{"property":"line-trim-offset","expression":"alt-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"alt-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"alt-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"alt-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.2},"trailExpressionData":{"property":"line-gradient","expression":"alt-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"alt-trail-casing-exp","type":"provider_based"}}}],"waypointsSource":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"3\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","routeLineMaskingLayerDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"masking-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-trim-offset","expression":"masking-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"masking-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"masking-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.3},"trailExpressionData":{"property":"line-trim-offset","expression":"masking-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"masking-trail-casing-exp","type":"provider_based"}},"type":"value_render_route_draw_data"},"styleId":"someId","action":"render_route_draw_data"},"subtype":"view"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendInitialOptionsEvent(optionsData)
        sender.sendUpdateDynamicOptionsEvent(styleId, newOptionsData)
        clearMocks(recorder, answers = false)

        val input = ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
            mockk {
                coEvery {
                    toEventValue(any(), newOptionsData)
                } returns RouteLineViewRenderRouteDrawDataInputValue(
                    RouteLineEventData(
                        getFeatureCollection("1"),
                        getPrimaryRouteLineEventDynamicData(),
                    ),
                    listOf(
                        RouteLineEventData(
                            getFeatureCollection("2"),
                            getAlternativeRouteLineEventDynamicData(),
                        ),
                    ),
                    getFeatureCollection("3"),
                    getMaskingRouteLineEventDynamicData(),
                )
            },
        )

        sender.sendRenderRouteDrawDataEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteLineUpdateEventError() {
        onRecorderEnabled()
        sender.sendRenderRouteLineUpdateEvent(
            styleId,
            ExpectedFactory.createError(RouteLineError("some error", null)),
        )

        verify(exactly = 0) { recorder.pushHistory("mbx.RouteLine", any()) }
    }

    @Test
    fun pushRenderRouteLineUpdateEventValueNoOptions() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"message":"NoOptions","type":"error"},"styleId":"someId","action":"render_route_line_update"},"subtype":"view","instanceId":"88fc5a95-b478-4923-9eb3-eda2a5cf8908"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(mockk())

        sender.sendRenderRouteLineUpdateEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteLineUpdateEventValueHasOptions() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"primaryRouteLineDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"primary-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"primary-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-gradient","expression":"primary-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"primary-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.1},"trailExpressionData":{"property":"line-trim-offset","expression":"primary-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"primary-trail-casing-exp","type":"provider_based"},"blurExpressionCommandData":{"property":"line-gradient","expression":"primary-blur-exp","type":"provider_based"}},"alternativeRouteLinesDynamicData":[{"baseExpressionData":{"property":"line-trim-offset","expression":"alt-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"alt-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"alt-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"alt-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.2},"trailExpressionData":{"property":"line-gradient","expression":"alt-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"alt-trail-casing-exp","type":"provider_based"}}],"routeLineMaskingLayerDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"masking-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-trim-offset","expression":"masking-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"masking-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"masking-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.3},"trailExpressionData":{"property":"line-trim-offset","expression":"masking-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"masking-trail-casing-exp","type":"provider_based"}},"type":"value_render_route_line_update"},"styleId":"someId","action":"render_route_line_update"},"subtype":"view"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            mockk<RouteLineUpdateValue> {
                coEvery {
                    toEventValue(any(), optionsData)
                } returns RouteLineViewRenderRouteLineUpdateDataValue(
                    getPrimaryRouteLineEventDynamicData(),
                    listOf(getAlternativeRouteLineEventDynamicData()),
                    getMaskingRouteLineEventDynamicData(),
                )
            },
        )
        sender.sendInitialOptionsEvent(optionsData)
        clearMocks(recorder, answers = false)

        sender.sendRenderRouteLineUpdateEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushRenderRouteLineUpdateEventValueOptionsChanged() {
        val newOptions = MapboxRouteLineViewOptions.Builder(mockk())
            .routeLineBelowLayerId("another-layer")
            .build()
            .toData()
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"primaryRouteLineDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"primary-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"primary-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-gradient","expression":"primary-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"primary-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.1},"trailExpressionData":{"property":"line-trim-offset","expression":"primary-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"primary-trail-casing-exp","type":"provider_based"},"blurExpressionCommandData":{"property":"line-gradient","expression":"primary-blur-exp","type":"provider_based"}},"alternativeRouteLinesDynamicData":[{"baseExpressionData":{"property":"line-trim-offset","expression":"alt-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-gradient","expression":"alt-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"alt-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"alt-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.2},"trailExpressionData":{"property":"line-gradient","expression":"alt-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"alt-trail-casing-exp","type":"provider_based"}}],"routeLineMaskingLayerDynamicData":{"baseExpressionData":{"property":"line-gradient","expression":"masking-base-exp","type":"provider_based"},"casingExpressionData":{"property":"line-trim-offset","expression":"masking-casing-exp","type":"provider_based"},"trafficExpressionData":{"property":"line-trim-offset","expression":"masking-traffic-exp","type":"provider_based"},"restrictedSectionExpressionData":{"property":"line-trim-offset","expression":"masking-restricted-exp","type":"provider_based"},"trimOffset":{"offset":0.3},"trailExpressionData":{"property":"line-trim-offset","expression":"masking-trail-exp","type":"provider_based"},"trailCasingExpressionData":{"property":"line-gradient","expression":"masking-trail-casing-exp","type":"provider_based"}},"type":"value_render_route_line_update"},"styleId":"someId","action":"render_route_line_update"},"subtype":"view"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            mockk<RouteLineUpdateValue> {
                coEvery {
                    toEventValue(any(), newOptions)
                } returns RouteLineViewRenderRouteLineUpdateDataValue(
                    getPrimaryRouteLineEventDynamicData(),
                    listOf(getAlternativeRouteLineEventDynamicData()),
                    getMaskingRouteLineEventDynamicData(),
                )
            },
        )
        sender.sendInitialOptionsEvent(optionsData)
        sender.sendUpdateDynamicOptionsEvent(styleId, newOptions)
        clearMocks(recorder, answers = false)

        sender.sendRenderRouteLineUpdateEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushClearRouteLineValueEventError() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"message":"Some error","type":"error"},"styleId":"someId","action":"render_route_line_clear_value"},"subtype":"view","instanceId":"5c2394b8-459a-48ee-8a94-c832cf6049f9"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val input = ExpectedFactory.createError<RouteLineError, RouteLineClearValue>(
            RouteLineError("Some error", null),
        )
        sender.sendClearRouteLineValueEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun pushClearRouteLineValueEventValue() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"input":{"primaryRouteSource":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"1\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","alternativeRoutesSources":["{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"2\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}"],"waypointsSource":"{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"3\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}]}","type":"value_render_route_line_clear"},"styleId":"someId","action":"render_route_line_clear_value"},"subtype":"view","instanceId":"adcaac11-243e-4d00-8e82-26970e272652"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineClearValue>(
            RouteLineClearValue(
                getFeatureCollection("1"),
                listOf(getFeatureCollection("2")),
                getFeatureCollection("3"),
                RouteCalloutData(emptyList()),
            ),
        )
        sender.sendClearRouteLineValueEvent(styleId, input)

        checkEvent(expected)
    }

    @Test
    fun showPrimaryRouteEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"show_primary_route"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendShowPrimaryRouteEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun hidePrimaryRouteEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"hide_primary_route"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendHidePrimaryRouteEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun showAlternativeRoutesEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"show_alternative_routes"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendShowAlternativeRoutesEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun hideAlternativeRoutesEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"hide_alternative_routes"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendHideAlternativeRoutesEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun showTrafficEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"show_traffic"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendShowTrafficEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun hideTrafficEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"hide_traffic"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendHideTrafficEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun pushShowOriginAndDestinationPointsEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"show_origin_and_destination"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendShowOriginAndDestinationPointsEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun pushHideOriginAndDestinationPointsEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"styleId":"someId","action":"hide_origin_and_destination"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendHideOriginAndDestinationPointsEvent(styleId)

        checkEvent(expected)
    }

    @Test
    fun pushCancelEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"action":"cancel"},"subtype":"view","instanceId":"04feb613-8258-4304-be3f-9a7be89b6106"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()

        sender.sendCancelEvent()

        checkEvent(expected)
    }

    @Test
    fun sameInstanceIdIsUsed() {
        onRecorderEnabled()

        sender.sendInitializeLayersEvent(styleId)
        val id1 = getInstanceId()
        clearMocks(recorder, answers = false)

        sender.sendCancelEvent()
        val id2 = getInstanceId()

        assertEquals(id1, id2)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun onRecorderEnabled() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { historyRecorder } returns recorder
            every { navigationOptions } returns mockk {
                every { copilotOptions } returns mockk {
                    every { shouldRecordRouteLineEvents } returns false
                }
                every { historyRecorderOptions } returns mockk {
                    every { shouldRecordRouteLineEvents } returns true
                }
            }
        }
        pusher.onAttached(mapboxNavigation)
        verify { historyRecorderChooserFactory.create(mapboxNavigation, capture(observerSlot)) }
        observerSlot.captured(recorder)
    }

    private fun checkEvent(expected: String) {
        val actualSlot = slot<String>()
        verify {
            recorder.pushHistory(
                "mbx.RouteLine",
                capture(actualSlot),
            )
        }
        val actualJson = JsonParser.parseString(actualSlot.captured).asJsonObject
        val expectedJson = JsonParser.parseString(expected).asJsonObject

        assertTrue(actualJson.getAsJsonPrimitive("instanceId").asString.isNotBlank())

        actualJson.remove("instanceId")
        expectedJson.remove("instanceId")

        assertEquals(expectedJson, actualJson)
    }

    private fun getFeatureCollection(featureId: String): FeatureCollection {
        return FeatureCollection.fromFeatures(listOf(getEmptyFeature(featureId)))
    }

    private fun getEmptyFeature(featureId: String): Feature {
        return Feature.fromJson(
            "{\"type\":\"Feature\",\"id\":\"${featureId}\"," +
                "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]}}",
        )
    }

    private fun getPrimaryRouteLineEventDynamicData(): RouteLineDynamicEventData {
        return RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "primary-base-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "primary-casing-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "primary-traffic-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "primary-restricted-exp"
                },
            ),
            RouteLineTrimOffset(0.1),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "primary-trail-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "primary-trail-casing-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "primary-blur-exp"
                },
            ),
        )
    }

    private fun getAlternativeRouteLineEventDynamicData(): RouteLineDynamicEventData {
        return RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "alt-base-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "alt-casing-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "alt-traffic-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "alt-restricted-exp"
                },
            ),
            RouteLineTrimOffset(0.2),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "alt-trail-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "alt-trail-casing-exp"
                },
            ),
            null,
        )
    }

    private fun getMaskingRouteLineEventDynamicData(): RouteLineDynamicEventData {
        return RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "masking-base-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "masking-casing-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "masking-traffic-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "masking-restricted-exp"
                },
            ),
            RouteLineTrimOffset(0.3),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                mockk {
                    every { toJson() } returns "masking-trail-exp"
                },
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                mockk {
                    every { toJson() } returns "masking-trail-casing-exp"
                },
            ),
            null,
        )
    }

    private fun getInstanceId(): String {
        val actualSlot = slot<String>()
        verify {
            recorder.pushHistory(
                "mbx.RouteLine",
                capture(actualSlot),
            )
        }
        val actualJson = JsonParser.parseString(actualSlot.captured).asJsonObject
        return actualJson.getAsJsonPrimitive("instanceId").asString
    }
}
