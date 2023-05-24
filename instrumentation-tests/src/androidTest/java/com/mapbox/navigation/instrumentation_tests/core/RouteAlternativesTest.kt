package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.NavigationRouteAlternativesResult
import com.mapbox.navigation.testing.ui.utils.coroutines.alternativesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
class RouteAlternativesTest : BaseCoreNoCleanUpTest() {

//    @get:Rule
//    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    // private lateinit var mapboxNavigation: MapboxNavigation
    private val startCoordinates = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )

    private companion object {
        private const val LOG_CATEGORY = "RouteAlternativesTest"
    }

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = startCoordinates[0].latitude()
        longitude = startCoordinates[0].longitude()
    }

    @Test
    fun expect_initial_alternative_route_removed_after_passing_the_fork_point() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { mapboxNavigation ->
            // Prepare with alternative routes.
            val routes = mapboxNavigation.requestNavigationRoutes(startCoordinates)

            // make sure that new alternatives won't be returned
            mockWebServerRule.requestHandlers.add(
                0,
                MockRequestHandler {
                    MockResponse().setResponseCode(500).setBody("")
                }
            )

            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
            mapboxNavigation.startTripSession()

            // Wait for enhanced locations to start and then set the routes.
            mapboxNavigation.flowLocationMatcherResult().first()

            // TODO: add case when the SDK subscribes after set routes
            val firstAlternative = async(start = CoroutineStart.UNDISPATCHED) {
                mapboxNavigation.alternativesUpdates()
                    .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                    .first()
            }

            mapboxNavigation.setNavigationRoutes(routes)

            val firstAlternativesCallback = firstAlternative.await()

            // Verify alternative routes events were triggered.
            assertEquals(2, routes.size)
            assertEquals(0, firstAlternativesCallback.alternatives.size)
        }
    }

//    /**
//     * The test verifies that if set routes to Navigation that are come from alternatives + n
//     * (where n >= 1) external routes, alternatives routes observer is not triggered with these routes.
//     * That was happening in legacy versions of NN.
//     */
//    @Test
//    fun additional_alternative_is_not_force_to_invoke_alternatives_observer() = sdkTest {
//        // Prepare with alternative routes.
//        setupMockRequestHandlers()
//        val routes = requestNavigationRoutes(startCoordinates)
//
//
//       // mapboxNavigation.historyRecorder.startRecording()
//        mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
//        mapboxNavigation.startTripSession()
//
//
//        // Wait for enhanced locations to start and then set the routes.
//        // Wait for enhanced locations to start and then set the routes.
//        mapboxNavigation.flowLocationMatcherResult().first()
//
//        // infinity subscription to avoid triggering NN on every new observer
//        // TODO: is that behaviour okay? I think no
//        mapboxNavigation.registerRouteAlternativesObserver(
//            object : NavigationRouteAlternativesObserver {
//                override fun onRouteAlternatives(
//                    routeProgress: RouteProgress,
//                    alternatives: List<NavigationRoute>,
//                    routerOrigin: RouterOrigin
//                ) = Unit
//
//                override fun onRouteAlternativesError(error: RouteAlternativesError) = Unit
//            }
//        )
//        mapboxNavigation.setNavigationRoutes(routes)
//
//
//        // Subscribing for alternatives
//        val firstAlternative = mapboxNavigation.alternativesUpdates()
//                .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
//                .filter { it.alternatives.isNotEmpty() } // TODO: why do we need to ignore empty callback in this case?
//                .first()
//
//        assertNotNull(firstAlternative.alternatives)
//
//        val nextAlternativeObserver = RouteAlternativesIdlingResource(
//            mapboxNavigation
//        ) { _, alternatives, _ ->
//            alternatives.isNotEmpty()
//        }
//        nextAlternativeObserver.register()
//
//        val externalAlternatives = DirectionsResponse.fromJson(
//            readRawFileText(context, R.raw.route_response_alternative_continue)
//        ).routes().also {
//            assertEquals(1, it.size)
//        }
//
//        lateinit var setRoutes: List<DirectionsRoute>
//        runOnMainSync {
//            setRoutes = (
//                mutableListOf(
//                    mapboxNavigation.getRoutes().first()
//                ) + firstAlternative.alternatives!! + externalAlternatives
//                ).also {
//                    assertTrue(
//                        "Primary route + >=1 alternatives + external alternatives",
//                        it.size >= 3
//                    )
//                }
//            mapboxNavigation.setRoutes(setRoutes)
//        }
//
//        mapboxHistoryTestRule.stopRecordingOnCrash("next alternatives failed") {
//            Espresso.onIdle()
//        }
//        nextAlternativeObserver.unregister()
//
//        runBlocking(Dispatchers.Main) {
//            mapboxNavigation.historyRecorder.stopRecording {
//                logE("history path=$it", LOG_CATEGORY)
//            }
//        }
//
//        // Verify alternative routes events were triggered.
//        firstAlternative.verifyOnRouteAlternativesAndProgressReceived()
//
//        // Verify next alternative routes events were triggered
//        nextAlternativeObserver.verifyOnRouteAlternativesAndProgressReceived()
//
//        // verify that set routes with  alternatives + 1 external alternatives is not triggered
//        // alternative observer
//        assertNotEquals(
//            "Alternative are not the same as setRoutes (alternatives might have " +
//                "additional routes or remove one or a few but not equal)",
//            setRoutes.drop(1).sortedBy { it.hashCode() }, // drop primary route
//            nextAlternativeObserver.alternatives!!.sortedBy { it.hashCode() }
//        )
//    }

    private fun setupMockRequestHandlers() {
        // Nav-native requests alternate routes, so we are only
        // ensuring the initial route has alternatives.
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_start),
                startCoordinates
            )
        )
    }

    private suspend fun MapboxNavigation.requestNavigationRoutes(coordinates: List<Point>): List<NavigationRoute> {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
        return requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
    }
}
