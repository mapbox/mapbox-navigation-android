package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.location.Location
import android.os.CountDownTimer
import androidx.annotation.RawRes
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.ApproximateDouble
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RouteLineLayersTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java
) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private val route1 by lazy {
        getRoute(R.raw.basic_route4)
    }
    private val route2 by lazy {
        getRoute(R.raw.basic_route5)
    }
    private val route3 by lazy {
        getRoute(R.raw.basic_route6)
    }

    override fun setupMockLocation(): Location {
        val directionsResponse = RoutesProvider
            .loadDirectionsResponse(context, R.raw.multiple_routes)
        val origin = directionsResponse.waypoints()!!.map { it.location() }
            .first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    @Test
    fun basicLayerConfigurationTest() {
        val options = MapboxRouteLineOptions.Builder(activity)
            .displayRestrictedRoadSections(true)
            .build()
        val routeLineApi = MapboxRouteLineApi(options)
        val routeLineView = MapboxRouteLineView(options)
        runOnMainSync {
            val style = activity.mapboxMap.getStyle()!!
            val routeOrigin = getRouteOriginPoint(route1.directionsRoute)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(12.0).build()
            activity.mapboxMap.setCamera(cameraOptions)

            routeLineApi.setNavigationRoutes(
                listOf(
                    route1,
                    route2,
                    route3
                )
            ) {
                routeLineView.renderRouteDrawData(style, it)

                val topLevelRouteLayerIndex = style.styleLayers.indexOf(
                    StyleObjectInfo(
                        RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                        "background"
                    )
                )

                assertEquals(
                    "mapbox-masking-layer-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 1].id
                )
                assertEquals(
                    "mapbox-masking-layer-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 2].id
                )
                assertEquals(
                    "mapbox-masking-layer-main",
                    style.styleLayers[topLevelRouteLayerIndex - 3].id
                )
                assertEquals(
                    "mapbox-masking-layer-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 4].id
                )
                assertEquals(
                    "mapbox-masking-layer-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 5].id
                )
                assertEquals(
                    "mapbox-masking-layer-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 6].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 7].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 8].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-main",
                    style.styleLayers[topLevelRouteLayerIndex - 9].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 10].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 11].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 12].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 13].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 14].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-main",
                    style.styleLayers[topLevelRouteLayerIndex - 15].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 16].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 17].id
                )
                assertEquals(
                    "mapbox-layerGroup-2-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 18].id
                )

                assertEquals(
                    "mapbox-layerGroup-3-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 19].id
                )
                assertEquals(
                    "mapbox-layerGroup-3-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 20].id
                )
                assertEquals(
                    "mapbox-layerGroup-3-main",
                    style.styleLayers[topLevelRouteLayerIndex - 21].id
                )
                assertEquals(
                    "mapbox-layerGroup-3-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 22].id
                )
                assertEquals(
                    "mapbox-layerGroup-3-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 23].id
                )
                assertEquals(
                    "mapbox-layerGroup-3-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 24].id
                )
                assertEquals(
                    "mapbox-bottom-level-route-layer",
                    style.styleLayers[topLevelRouteLayerIndex - 25].id
                )
            }
        }
    }

    // When existing routes are re-rendered but in a different order the map layers
    // for the primary route should be moved to the top of the stack above the other
    // route line layers.
    @Test
    fun updateLayerElevationTest() {
        val countDownLatch = CountDownLatch(1)
        val options = MapboxRouteLineOptions.Builder(activity)
            .displayRestrictedRoadSections(true)
            .build()
        val routeLineApi = MapboxRouteLineApi(options)
        val routeLineView = MapboxRouteLineView(options)
        runOnMainSync {
            val style = activity.mapboxMap.getStyle()!!
            val routeOrigin = getRouteOriginPoint(route1.directionsRoute)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(12.0).build()
            activity.mapboxMap.setCamera(cameraOptions)
            routeLineApi.setNavigationRoutes(
                listOf(
                    route1,
                    route2,
                    route3,
                )
            ) { result ->
                routeLineView.renderRouteDrawData(style, result)
                val topLevelRouteLayerIndex = style.styleLayers.indexOf(
                    StyleObjectInfo(
                        RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                        "background"
                    )
                )
                assertEquals(
                    "mapbox-masking-layer-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 1].id
                )
                assertEquals(
                    "mapbox-masking-layer-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 2].id
                )
                assertEquals(
                    "mapbox-masking-layer-main",
                    style.styleLayers[topLevelRouteLayerIndex - 3].id
                )
                assertEquals(
                    "mapbox-masking-layer-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 4].id
                )
                assertEquals(
                    "mapbox-masking-layer-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 5].id
                )
                assertEquals(
                    "mapbox-masking-layer-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 6].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-restricted",
                    style.styleLayers[topLevelRouteLayerIndex - 7].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-traffic",
                    style.styleLayers[topLevelRouteLayerIndex - 8].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-main",
                    style.styleLayers[topLevelRouteLayerIndex - 9].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-casing",
                    style.styleLayers[topLevelRouteLayerIndex - 10].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-trail",
                    style.styleLayers[topLevelRouteLayerIndex - 11].id
                )
                assertEquals(
                    "mapbox-layerGroup-1-trailCasing",
                    style.styleLayers[topLevelRouteLayerIndex - 12].id
                )
                // This mimics selecting an alternative route by making the first
                // alternative the primary route and the original primary route one
                // of the alternatives.
                val mutableValue = result.value!!.toMutableValue()
                mutableValue.primaryRouteLineData = result.value!!.alternativeRouteLinesData.first()
                mutableValue.alternativeRouteLinesData = listOf(
                    result.value!!.primaryRouteLineData,
                    result.value!!.alternativeRouteLinesData[1]
                )
                val updatedValue = ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
                    mutableValue.toImmutableValue()
                )
                routeLineView.renderRouteDrawData(style, updatedValue)
                // renderRouteDrawData is asynchronous and the map needs some time
                // to do its work
                object : CountDownTimer(1000, 1000) {
                    override fun onFinish() {
                        assertEquals(
                            "mapbox-masking-layer-restricted",
                            style.styleLayers[topLevelRouteLayerIndex - 1].id
                        )
                        assertEquals(
                            "mapbox-masking-layer-traffic",
                            style.styleLayers[topLevelRouteLayerIndex - 2].id
                        )
                        assertEquals(
                            "mapbox-masking-layer-main",
                            style.styleLayers[topLevelRouteLayerIndex - 3].id
                        )
                        assertEquals(
                            "mapbox-masking-layer-casing",
                            style.styleLayers[topLevelRouteLayerIndex - 4].id
                        )
                        assertEquals(
                            "mapbox-masking-layer-trail",
                            style.styleLayers[topLevelRouteLayerIndex - 5].id
                        )
                        assertEquals(
                            "mapbox-masking-layer-trailCasing",
                            style.styleLayers[topLevelRouteLayerIndex - 6].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-restricted",
                            style.styleLayers[topLevelRouteLayerIndex - 7].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-traffic",
                            style.styleLayers[topLevelRouteLayerIndex - 8].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-main",
                            style.styleLayers[topLevelRouteLayerIndex - 9].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-casing",
                            style.styleLayers[topLevelRouteLayerIndex - 10].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-trail",
                            style.styleLayers[topLevelRouteLayerIndex - 11].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-2-trailCasing",
                            style.styleLayers[topLevelRouteLayerIndex - 12].id
                        )
                        countDownLatch.countDown()
                    }

                    override fun onTick(p0: Long) {}
                }.start()
            }
        }
        countDownLatch.await()
    }

    // If the primary route line is hidden, then an alternative route is selected to become
    // the primary route, the original primary route line should become visible and the newly
    // selected primary route should be hidden
    @Test
    fun hidePrimaryRouteAndRePositionTest() {
        val countDownLatch = CountDownLatch(1)
        val options = MapboxRouteLineOptions.Builder(activity).build()
        val routeLineApi = MapboxRouteLineApi(options)
        val routeLineView = MapboxRouteLineView(options)
        runOnMainSync {
            val style = activity.mapboxMap.getStyle()!!
            val routeOrigin = getRouteOriginPoint(route1.directionsRoute)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(12.0).build()
            activity.mapboxMap.setCamera(cameraOptions)
            routeLineApi.setNavigationRoutes(
                listOf(
                    route1,
                    route2,
                    route3,
                )
            ) { result ->
                routeLineView.renderRouteDrawData(style, result)
                val topLevelRouteLayerIndex = style.styleLayers.indexOf(
                    StyleObjectInfo(
                        RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                        "background"
                    )
                )
                assertEquals(
                    Visibility.VISIBLE,
                    style.getLayer(style.styleLayers[topLevelRouteLayerIndex - 1].id)?.visibility
                )

                // Hide the primary route line
                routeLineView.hidePrimaryRoute(style)

                object : CountDownTimer(750, 750) {
                    override fun onFinish() {
                        // Assert the primary route line layer group is hidden by checking the traffic layer
                        assertEquals(
                            Visibility.NONE,
                            style.getLayer(
                                style.styleLayers[topLevelRouteLayerIndex - 1].id
                            )?.visibility
                        )
                        assertEquals(
                            "mapbox-masking-layer-traffic",
                            style.styleLayers[topLevelRouteLayerIndex - 1].id
                        )
                        assertEquals(
                            "mapbox-layerGroup-1-traffic",
                            style.styleLayers[topLevelRouteLayerIndex - 6].id
                        )
                        // This mimics selecting an alternative route by making the first
                        // alternative the primary route and the original primary route one
                        // of the alternatives.
                        val mutableValue = result.value!!.toMutableValue()
                        mutableValue.primaryRouteLineData =
                            result.value!!.alternativeRouteLinesData.first()
                        mutableValue.alternativeRouteLinesData = listOf(
                            result.value!!.primaryRouteLineData,
                            result.value!!.alternativeRouteLinesData[1]
                        )
                        val updatedValue =
                            ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
                                mutableValue.toImmutableValue()
                            )
                        routeLineView.renderRouteDrawData(style, updatedValue)
                        object : CountDownTimer(500, 500) {
                            override fun onFinish() {
                                // Primary route group is now 2 and not visible
                                assertEquals(
                                    "mapbox-masking-layer-traffic",
                                    style.styleLayers[topLevelRouteLayerIndex - 1].id
                                )
                                assertEquals(
                                    "mapbox-layerGroup-2-traffic",
                                    style.styleLayers[topLevelRouteLayerIndex - 6].id
                                )
                                assertEquals(
                                    Visibility.NONE,
                                    style.getLayer(
                                        style.styleLayers[topLevelRouteLayerIndex - 1].id
                                    )?.visibility
                                )
                                assertEquals(
                                    Visibility.NONE,
                                    style.getLayer(
                                        style.styleLayers[topLevelRouteLayerIndex - 6].id
                                    )?.visibility
                                )
                                // Previously primary route group is 1 and is now visible
                                assertEquals(
                                    "mapbox-layerGroup-1-traffic",
                                    style.styleLayers[topLevelRouteLayerIndex - 11].id
                                )
                                assertEquals(
                                    Visibility.VISIBLE,
                                    style.getLayer(
                                        style.styleLayers[topLevelRouteLayerIndex - 11].id
                                    )?.visibility
                                )
                                countDownLatch.countDown()
                            }

                            override fun onTick(p0: Long) {}
                        }.start()
                    }

                    override fun onTick(p0: Long) {}
                }.start()
            }
        }
        countDownLatch.await()
    }

    @Test
    fun should_provide_valid_offset_for_alternative_route() = sdkTest {
        val primaryRoute = createRoute(
            responseJson = R.raw.route_response_japan_1,
            requestUrlJson = R.raw.route_response_japan_1_url,
        ).first()

        /**
         * this route contains a duplicate point somewhere in the middle of the first step.
         * Inspecting a decoded portion of the `LineString` presents this:
         * ```
         * ...
         *     [
         *       140.9184,
         *       37.718443
         *     ],
         *     [
         *       140.918069,
         *       37.719383
         *     ],
         *     [
         *       140.918069,
         *       37.719383
         *     ],
         *     [
         *       140.917924,
         *       37.719839
         *     ],
         * ...
         * ```
         */
        val alternativeRoute = createRoute(
            responseJson = R.raw.route_response_japan_2,
            requestUrlJson = R.raw.route_response_japan_2_url,
        ).first()
        val mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity.applicationContext)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )

        mapboxNavigation.setNavigationRoutes(
            listOf(
                primaryRoute,
                alternativeRoute
            )
        )
        val routesUpdate = mapboxNavigation.routesUpdates()
            .take(1)
            .map { it.navigationRoutes }
            .toList().first()

        val options = MapboxRouteLineOptions.Builder(activity)
            .build()
        val routeLineApi = MapboxRouteLineApi(options)
        val alternativeMetadata = mapboxNavigation.getAlternativeMetadataFor(routesUpdate)
        val result = routeLineApi.setNavigationRoutes(
            newRoutes = routesUpdate,
            alternativeRoutesMetadata = alternativeMetadata
        )

        assertEquals(1, alternativeMetadata.size)
        // the alternative route overlaps primary on ~92%
        assertEquals(
            0.9263153441670023,
            result.value!!.alternativeRouteLinesData[0].dynamicData.trimOffset!!.offset,
            0.0000000001
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun lineDepthOcclusionFactorIsAppliedForRouteLine() = sdkTest {
        val factor = 0.85
        val options =
            MapboxRouteLineOptions.Builder(activity).lineDepthOcclusionFactor(factor).build()
        val routeLineApi = MapboxRouteLineApi(options)
        val routeLineView = MapboxRouteLineView(options)
        val style = activity.mapboxMap.getStyle()!!
        val result = routeLineApi.setNavigationRoutes(
            listOf(
                route1,
                route2,
                route3,
            )
        )
        routeLineView.renderRouteDrawData(style, result)

        val lineLayers = withTimeoutOrNull(10000) {
            var lineLayers: List<LineLayer>
            do {
                delay(50)
                lineLayers = style.styleLayers.mapNotNull { style.getLayer(it.id) }
                    .filterIsInstance(LineLayer::class.java)
            } while (lineLayers.size < 3) // 3 routes
            lineLayers
        } ?: error("layer weren't initialised")
        val expected = lineLayers.map { it.layerId to ApproximateDouble(factor) }
        val actual = lineLayers.map {
            it.layerId to ApproximateDouble(
                style.getStyleLayerProperty(
                    it.layerId,
                    "line-depth-occlusion-factor"
                ).value.contents as Double
            )
        }
        assertEquals(expected, actual)
    }

    private fun getRoute(routeResourceId: Int): NavigationRoute {
        val routeAsString = readRawFileText(activity, routeResourceId)
        return DirectionsRoute.fromJson(routeAsString).toNavigationRoute(
            routerOrigin = RouterOrigin.Offboard
        )
    }

    private fun getRouteOriginPoint(route: DirectionsRoute): Point =
        route.completeGeometryToPoints().first()

    private fun createRoute(
        @RawRes responseJson: Int,
        @RawRes requestUrlJson: Int
    ): List<NavigationRoute> = NavigationRoute.create(
        directionsResponseJson = readRawFileText(
            activity,
            responseJson
        ),
        routeRequestUrl = RouteOptions.fromJson(
            readRawFileText(
                activity,
                requestUrlJson
            )
        ).toUrl("xyz").toString(),
        routerOrigin = RouterOrigin.Offboard
    )
}
