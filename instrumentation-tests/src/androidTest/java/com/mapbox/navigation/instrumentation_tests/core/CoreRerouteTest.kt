package com.mapbox.navigation.instrumentation_tests.core

import com.mapbox.navigation.instrumentation_tests.activity.CameraTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CoreRerouteTest : BaseTest<CameraTestActivity>(CameraTestActivity::class.java) {

    @Test
    fun ddtest() {
        runBlocking {
            runOnMainSync {
                activity.startTesting()
            }
            delay(900_000)
        }
    }

//    @Test
//    fun reroute_completes() {
//        // prepare
//        val mockRoute = RoutesProvider.dc_very_short(activity)
//        val originLocation = mockRoute.routeWaypoints.first()
//        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
//            latitude = originLocation.latitude() + 0.002
//            longitude = originLocation.longitude()
//        }
//
//        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
//        mockWebServerRule.requestHandlers.add(
//            MockDirectionsRequestHandler(
//                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
//                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
//                expectedCoordinates = listOf(
//                    Point.fromLngLat(
//                        offRouteLocationUpdate.longitude,
//                        offRouteLocationUpdate.latitude
//                    ),
//                    mockRoute.routeWaypoints.last()
//                ),
//                relaxedExpectedCoordinates = true
//            )
//        )
//        locationTrackingIdlingResource.register()
//
//        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
//            requiredState(RouteProgressState.TRACKING)
//            requiredState(RouteProgressState.OFF_ROUTE)
//            optionalState(RouteProgressState.INITIALIZED)
//            requiredState(RouteProgressState.TRACKING)
//        }
//
//        // start a route
//        runOnMainSync {
//            mapboxNavigation.historyRecorder.startRecording()
//            mapboxNavigation.startTripSession()
//            mapboxNavigation.requestRoutes(
//                RouteOptions.builder()
//                    .applyDefaultNavigationOptions()
//                    .applyLanguageAndVoiceUnitOptions(activity)
//                    .baseUrl(mockWebServerRule.baseUrl)
//                    .coordinatesList(mockRoute.routeWaypoints).build(),
//                object : RouterCallback {
//                    override fun onRoutesReady(
//                        routes: List<DirectionsRoute>,
//                        routerOrigin: RouterOrigin
//                    ) {
//                        mapboxNavigation.setRoutes(routes)
//                    }
//
//                    override fun onFailure(
//                        reasons: List<RouterFailure>,
//                        routeOptions: RouteOptions
//                    ) {
//                        logE("onFailure reasons=$reasons", "DEBUG")
//                    }
//
//                    override fun onCanceled(
//                        routeOptions: RouteOptions,
//                        routerOrigin: RouterOrigin
//                    ) {
//                        logE("onCanceled", "DEBUG")
//                    }
//                }
//            )
//        }
//
//        // wait for tracking to start
//        mapboxHistoryTestRule.stopRecordingOnCrash("no location tracking") {
//            Espresso.onIdle()
//        }
//        locationTrackingIdlingResource.unregister()
//
//        // push off route location and wait for the off route event
//        offRouteIdlingResource.register()
//        runOnMainSync {
//            mockLocationReplayerRule.loopUpdate(offRouteLocationUpdate, times = 5)
//        }
//
//        mapboxHistoryTestRule.stopRecordingOnCrash("no off route") {
//            Espresso.onIdle()
//        }
//        offRouteIdlingResource.unregister()
//
//        // wait for tracking to start again
//        locationTrackingIdlingResource.register()
//
//        mapboxHistoryTestRule.stopRecordingOnCrash("no tracking") {
//            Espresso.onIdle()
//        }
//        locationTrackingIdlingResource.unregister()
//
//        val countDownLatch = CountDownLatch(1)
//        runOnMainSync {
//            mapboxNavigation.historyRecorder.stopRecording {
//                logE("history path=$it", "DEBUG")
//                countDownLatch.countDown()
//            }
//        }
//        countDownLatch.await()
//
//        // assert results
//        expectedStates.assert()
//
//        runOnMainSync {
//            val newWaypoints =
//                mapboxNavigation.getRoutes().first().routeOptions()!!.coordinatesList()
//            check(newWaypoints.size == 2) {
//                "Expected 2 waypoints in the route after reroute but was ${newWaypoints.size}"
//            }
//            check(newWaypoints[1] == mockRoute.routeWaypoints.last())
//        }
//    }
}
