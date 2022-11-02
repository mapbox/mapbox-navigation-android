package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logE
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class EvReroute : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var offRouteIdlingResource: RouteProgressStateIdlingResource
    private lateinit var locationTrackingIdlingResource: RouteProgressStateIdlingResource

    @Before
    fun setup() {
        Espresso.onIdle()

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
                    .build()
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
        locationTrackingIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.TRACKING
        )
        offRouteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.OFF_ROUTE
        )
    }

    override fun setupMockLocation(): Location {
        val mockRoute = RoutesProvider.california_ev_route(activity)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun ev_reroute() {
        val mockRoute = RoutesProvider.california_ev_route(activity)
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.005
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(
                    activity,
                    R.raw.ev_route_response_reroute_ca_2_ev_waypoints
                ),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude,
                        offRouteLocationUpdate.latitude
                    ),
                    mockRoute.routeWaypoints.last()
                ),
                relaxedExpectedCoordinates = true
            )
        )
        locationTrackingIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.OFF_ROUTE)
            optionalState(RouteProgressState.INITIALIZED)
            requiredState(RouteProgressState.TRACKING)
        }

        // start a route
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints)
                    .unrecognizedProperties(
                        mapOf(
                            "engine" to "electric",
                            "ev_max_charge" to "8000",
                            "ev_connector_types" to "tesla",
                            "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                            "ev_charging_curve" to "40000,70000",
                        )
                    )
                    .build(),
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        logE("onFailure reasons=$reasons", "DEBUG")
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        logE("onCanceled", "DEBUG")
                    }
                }
            )
        }

        // wait for tracking to start
        mapboxHistoryTestRule.stopRecordingOnCrash("no location tracking") {
            Espresso.onIdle()
        }
        locationTrackingIdlingResource.unregister()

        // push off route location and wait for the off route event
        offRouteIdlingResource.register()
        runOnMainSync {
            mockLocationReplayerRule.loopUpdate(offRouteLocationUpdate, times = 5)
        }

        mapboxHistoryTestRule.stopRecordingOnCrash("no off route") {
            Espresso.onIdle()
        }
        offRouteIdlingResource.unregister()

        // wait for tracking to start again
        locationTrackingIdlingResource.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("no tracking") {
            Espresso.onIdle()
        }
        locationTrackingIdlingResource.unregister()

        val countDownLatch = CountDownLatch(1)
        runOnMainSync {
            mapboxNavigation.historyRecorder.stopRecording {
                logE("history path=$it", "DEBUG")
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()

        // assert results
        expectedStates.assert()

        runOnMainSync {
            val routeOptions = mapboxNavigation.getNavigationRoutes().first().routeOptions
            val newWaypoints = routeOptions.coordinatesList()

            check(newWaypoints.size == 2) {
                "Expected 2 waypoints in the route after reroute but was ${newWaypoints.size}"
            }
            check(newWaypoints[1] == mockRoute.routeWaypoints.last())
            with(routeOptions.unrecognizedJsonProperties!!){
                check(get("engine")!!.asString == "electric")
                check(get("ev_max_charge")!!.asString == "8000")
                check(get("ev_connector_types")!!.asString == "tesla")
                check(get("energy_consumption_curve")!!.asString == "0,300;20,160;80,140;120,180")
                check(get("ev_charging_curve")!!.asString == "40000,70000")
            }
        }
    }
}
