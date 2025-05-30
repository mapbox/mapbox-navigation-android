@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.SpeedLimit
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.SdkInformation
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.router.MapboxNavigationTestRouteRefresher
import com.mapbox.navigation.testing.router.MapboxNavigationTestRouter
import com.mapbox.navigation.testing.router.RefreshOptions
import com.mapbox.navigation.testing.router.RouteRefreshCallback
import com.mapbox.navigation.testing.router.RouterCallback
import com.mapbox.navigation.testing.router.TestRefresherFailure
import com.mapbox.navigation.testing.router.TestRouterFailure
import com.mapbox.navigation.testing.router.createNavigationRouterRule
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.refreshStates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class CustomRouterTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val navigationRouterRule = createNavigationRouterRule()

    @get:Rule
    val historyRecorderRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        val mockRoute = RoutesProvider.dc_very_short(context)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun request_custom_route_and_refresh_without_changes() {
        val mockRoute = RoutesProvider.dc_very_short(context)
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(mockRoute.routeWaypoints)
            .build()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    if (routeOptions == testRouteOptions) {
                        callback.onRoutesReady(mockRoute.routeResponse)
                    } else {
                        callback.onFailure(TestRouterFailure.noRoutesFound())
                    }
                }
            },
        )
        navigationRouterRule.setRouteRefresher(
            object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    if (options.responseUUID == mockRoute.routeResponse.uuid()) {
                        callback.onRefresh(mockRoute.routeResponse.routes()[options.routeIndex])
                    } else {
                        callback.onFailure(TestRefresherFailure.serverError())
                    }
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val initialRoutesResponse = navigation.requestRoutes(testRouteOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.ONLINE, initialRoutesResponse.routerOrigin)
                assertEquals(
                    mockRoute.routeResponse.uuid(),
                    initialRoutesResponse.routes.first().responseUUID,
                )
                stayOnPosition(
                    latitude = mockRoute.routeWaypoints.first().latitude(),
                    longitude = mockRoute.routeWaypoints.first().longitude(),
                    bearing = mockRoute.routeResponse.routes().first().legs()!!.first()!!.steps()!!
                        .first().maneuver().bearingBefore()!!.toFloat(),
                ) {
                    navigation.startTripSession()
                    navigation.setNavigationRoutesAsync(initialRoutesResponse.routes)
                    navigation.routeProgressUpdates().first()
                    navigation.routeRefreshController.requestImmediateRouteRefresh()
                    val update = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
                    }
                    assertEquals(
                        initialRoutesResponse.routes.first().directionsRoute.legs()?.first()
                            ?.annotation(),
                        update.navigationRoutes.first().directionsRoute.legs()?.first()
                            ?.annotation(),
                    )
                    assertEquals(
                        initialRoutesResponse.routes.first().upcomingRoadObjects,
                        update.navigationRoutes.first().upcomingRoadObjects,
                    )
                }
            }
        }
    }

    @Test
    fun test_router_callback_is_not_called() {
        val mockRoute = RoutesProvider.dc_very_short(context)
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(mockRoute.routeWaypoints)
            .build()
        val testRouteRequested = CompletableDeferred<Unit>()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    // SDK's user doesn't return anything
                    if (routeOptions == testRouteOptions) {
                        testRouteRequested.complete(Unit)
                    }
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                navigation.requestRoutes(
                    testRouteOptions,
                    object : NavigationRouterCallback {
                        override fun onRoutesReady(
                            routes: List<NavigationRoute>,
                            @RouterOrigin routerOrigin: String,
                        ) {
                            fail(
                                "request shouldn't be completed successfully " +
                                    "if user doesn't call test router callback",
                            )
                        }

                        override fun onFailure(
                            reasons: List<RouterFailure>,
                            routeOptions: RouteOptions,
                        ) {
                            fail(
                                "request shouldn't fail" +
                                    "if user doesn't call test router callback",
                            )
                        }

                        override fun onCanceled(
                            routeOptions: RouteOptions,
                            @RouterOrigin routerOrigin: String,
                        ) {
                            fail(
                                "request shouldn't be cancelled" +
                                    "if user doesn't call test router callback",
                            )
                        }
                    },
                )
                testRouteRequested.await()
            }
        }
    }

    @Test
    fun refresh_annotations_from_a_middle_of_the_route_first_leg() {
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinates("-73.9777,40.762861;-73.976687,40.76322;-73.974737,40.761998")
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_DURATION,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_CONGESTION,
                    DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                    DirectionsCriteria.ANNOTATION_MAXSPEED,
                    DirectionsCriteria.ANNOTATION_CLOSURE,
                    DirectionsCriteria.ANNOTATION_TRAFFIC_TENDENCY,
                    "state_of_charge",
                ),
            )
            .unrecognizedProperties(
                mapOf(
                    "engine" to "electric",
                    "ev_initial_charge" to "1000",
                    "ev_max_charge" to "50000",
                    "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                    "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                    "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                    "ev_min_charge_at_charging_station" to "1",
                ),
            )
            .build()
        val testRouteResponse =
            DirectionsResponse.fromJson(readRawFileText(context, R.raw.ev_route_all_annotations))
        val firstLegNewIncidents = listOf(
            Incident.builder()
                .geometryIndexStart(0)
                .geometryIndexEnd(1)
                .id("0")
                .type(Incident.INCIDENT_LANE_RESTRICTION)
                .build(),
            Incident.builder()
                .geometryIndexStart(8)
                .geometryIndexEnd(9)
                .id("1")
                .type(Incident.INCIDENT_LANE_RESTRICTION)
                .build(),
        )
        val secondLegNewIncidents = listOf(
            Incident.builder()
                .geometryIndexStart(0)
                .geometryIndexEnd(1)
                .id("3")
                .type(Incident.INCIDENT_LANE_RESTRICTION)
                .build(),
            Incident.builder()
                .geometryIndexStart(3)
                .geometryIndexEnd(4)
                .id("4")
                .type(Incident.INCIDENT_LANE_RESTRICTION)
                .build(),
        )
        val firstLegNewClosures = listOf(
            Closure.builder()
                .geometryIndexEnd(1)
                .geometryIndexStart(0)
                .build(),
            Closure.builder()
                .geometryIndexEnd(9)
                .geometryIndexStart(8)
                .build(),
        )
        val secondLegNewClosures = listOf(
            Closure.builder()
                .geometryIndexEnd(1)
                .geometryIndexStart(0)
                .build(),
            Closure.builder()
                .geometryIndexEnd(3)
                .geometryIndexStart(5)
                .build(),
        )
        val testAnnotationsRefreshValues = TestAnnotationsRefreshValues()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    if (routeOptions == testRouteOptions) {
                        callback.onRoutesReady(testRouteResponse)
                    } else {
                        callback.onFailure(TestRouterFailure.noRoutesFound())
                    }
                }
            },
        )
        navigationRouterRule.setRouteRefresher(
            object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    if (options.responseUUID == testRouteResponse.uuid()) {
                        callback.onRefresh(
                            updateRouteUsingTestValues(
                                testRouteResponse.routes()[options.routeIndex],
                                testAnnotationsRefreshValues,
                                incidentsForLegs = listOf(
                                    firstLegNewIncidents,
                                    secondLegNewIncidents,
                                ),
                                closuresForLegs = listOf(
                                    firstLegNewClosures,
                                    secondLegNewClosures,
                                ),
                            ),
                        )
                    } else {
                        callback.onFailure(TestRefresherFailure.serverError())
                    }
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val maneuverInTheMiddleOfTheRoute = testRouteResponse.routes().first()
                    .legs()!!.first()!!
                    .steps()!!.let { it[it.size / 2] }
                    .maneuver()
                stayOnPosition(
                    latitude = maneuverInTheMiddleOfTheRoute.location().latitude(),
                    longitude = maneuverInTheMiddleOfTheRoute.location().longitude(),
                    bearing = maneuverInTheMiddleOfTheRoute.bearingBefore()!!.toFloat(),
                ) {
                    val initialRoutesResponse = navigation.requestRoutes(testRouteOptions)
                        .getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.ONLINE, initialRoutesResponse.routerOrigin)
                    assertEquals(
                        testRouteResponse.uuid(),
                        initialRoutesResponse.routes.first().responseUUID,
                    )
                    navigation.startTripSession()
                    navigation.setNavigationRoutesAsync(initialRoutesResponse.routes)
                    val routeIndexAtRefresh = navigation.routeProgressUpdates()
                        .first()
                        .currentLegProgress!!
                        .geometryIndex
                    navigation.routeRefreshController.requestImmediateRouteRefresh()
                    val update = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
                    }
                    val refreshedFirstLeg = update.navigationRoutes.first()
                        .directionsRoute.legs()!!.first()
                    checkIfAnnotationsUpdatedSinceIndex(
                        refreshedFirstLeg,
                        // annotations before geometry of refresh is expected to stay the same
                        routeIndexAtRefresh,
                        testAnnotationsRefreshValues,
                    )
                    assertEquals(
                        // the first incident is before the point of refresh
                        listOf(firstLegNewIncidents[1]),
                        refreshedFirstLeg.incidents(),
                    )
                    assertEquals(
                        // the first closure is before the point of refresh
                        listOf(firstLegNewClosures[1]),
                        refreshedFirstLeg.closures(),
                    )
                    val refreshedSecondLeg = update.navigationRoutes.first()
                        .directionsRoute.legs()!![1]
                    checkIfAnnotationsUpdatedSinceIndex(
                        refreshedSecondLeg,
                        0,
                        testAnnotationsRefreshValues,
                    )
                    assertEquals(
                        secondLegNewIncidents,
                        refreshedSecondLeg.incidents(),
                    )
                }
            }
        }
    }

    @Test
    fun request_route_failure() {
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(0.0, 0.0),
                    Point.fromLngLat(1.0, 1.0),
                ),
            )
            .build()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    callback.onFailure(TestRouterFailure.noRoutesFound())
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val testRouteResponse = navigation.requestRoutes(testRouteOptions)
                assertTrue(
                    "response is $testRouteResponse",
                    testRouteResponse is RouteRequestResult.Failure,
                )
                val failure = testRouteResponse as RouteRequestResult.Failure
                assertEquals(
                    failure.reasons.first().message,
                    "No route found",
                )
            }
        }
    }

    @Test
    fun fail_route_refresh() {
        val mockRoute = RoutesProvider.dc_very_short(context)
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(mockRoute.routeWaypoints)
            .build()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    if (routeOptions == testRouteOptions) {
                        callback.onRoutesReady(mockRoute.routeResponse)
                    } else {
                        callback.onFailure(TestRouterFailure.noRoutesFound())
                    }
                }
            },
        )
        navigationRouterRule.setRouteRefresher(
            object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onFailure(TestRefresherFailure.serverError())
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val initialRoutesResponse = navigation.requestRoutes(testRouteOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.ONLINE, initialRoutesResponse.routerOrigin)
                assertEquals(
                    mockRoute.routeResponse.uuid(),
                    initialRoutesResponse.routes.first().responseUUID,
                )
                stayOnPosition(
                    latitude = mockRoute.routeWaypoints.first().latitude(),
                    longitude = mockRoute.routeWaypoints.first().longitude(),
                    bearing = mockRoute.routeResponse.routes().first().legs()!!.first()!!.steps()!!
                        .first().maneuver().bearingBefore()!!.toFloat(),
                ) {
                    navigation.startTripSession()
                    navigation.setNavigationRoutesAsync(initialRoutesResponse.routes)
                    navigation.routeProgressUpdates().first()
                    navigation.routeRefreshController.requestImmediateRouteRefresh()
                    navigation.refreshStates().first {
                        it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
                    }
                }
            }
        }
    }

    @Test
    fun other_requests_keep_working() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = historyRecorderRule,
        ) {
            val testPath = "/api/test"
            val testResponse = "test123"
            mockWebServerRule.requestHandlers.add(
                MockRequestHandler {
                    if (it.path == testPath) {
                        MockResponse().setBody(testResponse).setResponseCode(200)
                    } else {
                        null
                    }
                },
            )

            val responseDeferred = CompletableDeferred<HttpResponse>()
            HttpServiceFactory
                .getInstance()
                .request(
                    HttpRequest.Builder()
                        .url("${mockWebServerRule.baseUrl}$testPath")
                        .sdkInformation(
                            SdkInformation(
                                "Nav SDK instrumentation tests",
                                "0.0.1",
                                null,
                            ),
                        )
                        .headers(HashMap())
                        .build(),
                ) {
                    responseDeferred.complete(it)
                }
            val response = responseDeferred.await()

            assertTrue(
                "response isn't successful: $response",
                response.result.isValue,
            )
            assertEquals(
                testResponse,
                String(response.result.value!!.data),
            )
        }
    }

    @Test
    fun other_wrong_requests_keep_working() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = historyRecorderRule,
        ) {
            val responseDeferred = CompletableDeferred<HttpResponse>()
            HttpServiceFactory
                .getInstance()
                .request(
                    HttpRequest.Builder()
                        .url("wrong-url")
                        .sdkInformation(
                            SdkInformation(
                                "Nav SDK instrumentation tests",
                                "0.0.1",
                                null,
                            ),
                        )
                        .headers(HashMap())
                        .build(),
                ) {
                    responseDeferred.complete(it)
                }
            val response = responseDeferred.await()

            assertTrue(
                "response hasn't failed: $response",
                response.result.isError,
            )
        }
    }

    @Test
    fun no_test_router_provided() {
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            // Route is requested from a location that is far away from the current one,
            // to make sure that the onboard router doesn't succeed in building a route,
            // due to the absence of tiles in ambient cache for a different region.
            .coordinates("11.9460162,57.7120152;-7.5830081,38.6731391")
            .build()
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val testRouteResponse = navigation.requestRoutes(testRouteOptions)
                assertTrue(
                    "response is $testRouteResponse",
                    testRouteResponse is RouteRequestResult.Failure,
                )
                val failure = testRouteResponse as RouteRequestResult.Failure
                assertEquals(
                    failure.reasons.first().message,
                    "No route found",
                )
            }
        }
    }

    @Test
    fun no_route_refresher_provided() {
        val mockRoute = RoutesProvider.dc_very_short(context)
        val testRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(mockRoute.routeWaypoints)
            .build()
        navigationRouterRule.setRouter(
            object : MapboxNavigationTestRouter {
                override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
                    if (routeOptions == testRouteOptions) {
                        callback.onRoutesReady(mockRoute.routeResponse)
                    } else {
                        callback.onFailure(TestRouterFailure.noRoutesFound())
                    }
                }
            },
        )
        sdkTest {
            withMapboxNavigation(
                historyRecorderRule = historyRecorderRule,
            ) { navigation ->
                val initialRoutesResponse = navigation.requestRoutes(testRouteOptions)
                    .getSuccessfulResultOrThrowException()
                stayOnPosition(
                    latitude = mockRoute.routeWaypoints.first().latitude(),
                    longitude = mockRoute.routeWaypoints.first().longitude(),
                    bearing = mockRoute.routeResponse.routes().first().legs()!!.first()!!.steps()!!
                        .first().maneuver().bearingBefore()!!.toFloat(),
                ) {
                    navigation.startTripSession()
                    navigation.setNavigationRoutesAsync(initialRoutesResponse.routes)
                    navigation.routeProgressUpdates().first()
                    navigation.routeRefreshController.requestImmediateRouteRefresh()
                    navigation.refreshStates().first {
                        it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
                    }
                }
            }
        }
    }
}

data class TestAnnotationsRefreshValues(
    val duration: Double = 3.0,
    val distance: Double = 5.0,
    val speed: Double = 8.0,
    val maxSpeed: MaxSpeed = MaxSpeed.builder().speed(20).unit(SpeedLimit.KMPH)
        .build(),
    val congestionsNumeric: Int = 77,
    val congestions: String = "heavy",
    val stateOfCharge: Int = 8,
)

private fun checkIfAnnotationsUpdatedSinceIndex(
    routeLeg: RouteLeg,
    legGeometryIndex: Int,
    testAnnotationsRefreshValues: TestAnnotationsRefreshValues,
) {
    val refreshedFirstLegAnnotations = routeLeg.annotation()!!
    val refreshedDuration =
        refreshedFirstLegAnnotations.duration()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedDuration.size) { testAnnotationsRefreshValues.duration },
        refreshedDuration,
    )
    val refreshedDistance =
        refreshedFirstLegAnnotations.distance()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedDistance.size) { testAnnotationsRefreshValues.distance },
        refreshedDistance,
    )
    val refreshedSpeed =
        refreshedFirstLegAnnotations.speed()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedSpeed.size) { testAnnotationsRefreshValues.speed },
        refreshedSpeed,
    )
    val refreshedMaxSpeed =
        refreshedFirstLegAnnotations.maxspeed()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedMaxSpeed.size) { testAnnotationsRefreshValues.maxSpeed },
        refreshedMaxSpeed,
    )
    val refreshedCongestions =
        refreshedFirstLegAnnotations.congestion()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedCongestions.size) { testAnnotationsRefreshValues.congestions },
        refreshedCongestions,
    )
    val refreshedCongestionNumeric =
        refreshedFirstLegAnnotations.congestionNumeric()!!.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedCongestionNumeric.size) {
            testAnnotationsRefreshValues.congestionsNumeric
        },
        refreshedCongestionNumeric,
    )
    val refreshedStateOfCharge =
        refreshedFirstLegAnnotations.unrecognizedJsonProperties!!
            .get("state_of_charge")!!
            .asJsonArray.drop(legGeometryIndex)
    assertEquals(
        MutableList(refreshedStateOfCharge.size) {
            JsonPrimitive(
                testAnnotationsRefreshValues.stateOfCharge,
            )
        },
        refreshedStateOfCharge,
    )
}

private fun updateRouteUsingTestValues(
    it: DirectionsRoute,
    testAnnotationsRefreshValues: TestAnnotationsRefreshValues,
    incidentsForLegs: List<List<Incident>>,
    closuresForLegs: List<List<Closure>>,
) = it.toBuilder()
    .legs(
        it.legs()?.mapIndexed { legIndex, routeLeg ->
            val legAnnotations = routeLeg.annotation()
            routeLeg.toBuilder()
                .annotation(
                    legAnnotations?.toBuilder()
                        ?.duration(
                            legAnnotations.duration()
                                ?.let {
                                    MutableList(it.size) { testAnnotationsRefreshValues.duration }
                                },
                        )
                        ?.distance(
                            legAnnotations.distance()
                                ?.let {
                                    MutableList(it.size) { testAnnotationsRefreshValues.distance }
                                },
                        )
                        ?.speed(
                            legAnnotations.speed()
                                ?.let {
                                    MutableList(it.size) { testAnnotationsRefreshValues.speed }
                                },
                        )
                        ?.maxspeed(
                            legAnnotations.maxspeed()
                                ?.let {
                                    MutableList(it.size) { testAnnotationsRefreshValues.maxSpeed }
                                },
                        )
                        ?.congestionNumeric(
                            legAnnotations.congestionNumeric()
                                ?.let {
                                    MutableList(it.size) {
                                        testAnnotationsRefreshValues.congestionsNumeric
                                    }
                                },
                        )
                        ?.congestion(
                            legAnnotations.congestion()
                                ?.let {
                                    MutableList(it.size) {
                                        testAnnotationsRefreshValues.congestions
                                    }
                                },
                        )
                        ?.unrecognizedJsonProperties(
                            mapOf(
                                "state_of_charge" to legAnnotations.unrecognizedJsonProperties?.get(
                                    "state_of_charge",
                                )?.asJsonArray?.size()?.let { size ->
                                    JsonArray(size).apply {
                                        (0 until size).forEach { index ->
                                            add(
                                                JsonPrimitive(
                                                    testAnnotationsRefreshValues.stateOfCharge,
                                                ),
                                            )
                                        }
                                    }
                                },
                            ),
                        )
                        ?.build(),
                )
                .incidents(
                    incidentsForLegs[legIndex],
                )
                .closures(
                    closuresForLegs[legIndex],
                )
                .build()
        },
    ).build()
