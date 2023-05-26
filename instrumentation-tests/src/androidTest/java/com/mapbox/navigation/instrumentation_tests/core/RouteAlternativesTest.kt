package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.NavigationRouteAlternativesResult
import com.mapbox.navigation.testing.ui.utils.coroutines.alternativesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
class RouteAlternativesTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private val startCoordinates = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )
    private val continueCoordinates = listOf(
        Point.fromLngLat(-122.275220, 37.805862),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )

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
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            // make sure that new alternatives won't be returned
            mockWebServerRule.requestHandlers.add(
                0,
                MockRequestHandler {
                    MockResponse().setResponseCode(500).setBody("")
                }
            )
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            val firstAlternative = firstAlternativesUpdateDeferred(mapboxNavigation)
            mapboxNavigation.setNavigationRoutes(testRoutes)

            val firstAlternativesCallback = firstAlternative.await()

            assertEquals(2, testRoutes.size)
            assertEquals(
                "Existing alternative should be remove after passing the fork point",
                0,
                firstAlternativesCallback.alternatives.size
            )
        }
    }

    @Ignore("NN-754")
    @Test
    fun alternatives_are_updated_after_passing_fork_point() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.registerRouteAlternativesObserver(
                object : NavigationRouteAlternativesObserver {
                    override fun onRouteAlternatives(
                        routeProgress: RouteProgress,
                        alternatives: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        val newRoutes = mutableListOf<NavigationRoute>().apply {
                            add(mapboxNavigation.getNavigationRoutes().first())
                            addAll(alternatives)
                        }
                        mapboxNavigation.setNavigationRoutes(newRoutes)
                    }

                    override fun onRouteAlternativesError(error: RouteAlternativesError) {
                    }
                }
            )
            mapboxNavigation.setNavigationRoutes(testRoutes)

            val newAlternatives = mapboxNavigation.routesUpdates()
                .filter { it.navigationRoutes != testRoutes } // skip initial routes
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
                .filter { it.navigationRoutes.size > 1 }
                .first()
                .navigationRoutes
                .drop(1)

            newAlternatives.forEach {
                assertNotNull(
                    "alternative route $it doesn't have metadata",
                    mapboxNavigation.getAlternativeMetadataFor(it)
                )
            }
        }
    }

    @Test
    fun alternative_observer_is_not_called_with_current_alternatives_upon_subscription() =
        sdkTest {
            setupMockRequestHandlers()
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule
            ) { mapboxNavigation ->
                val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
                val originOfTestRoute = testRoutes.first().routeOptions.coordinatesList().first()
                stayOnPosition(
                    latitude = originOfTestRoute.latitude(),
                    longitude = originOfTestRoute.longitude(),
                    bearing = 30.0f
                ) {
                    mapboxNavigation.startTripSession()

                    val alternativesCallbackResultBeforeSetRoute =
                        firstAlternativesUpdateDeferred(mapboxNavigation)
                    mapboxNavigation.setNavigationRoutesAsync(testRoutes)
                    mapboxNavigation.routeProgressUpdates().first()
                    val alternativesCallbackResultAfterSetRoute =
                        firstAlternativesUpdateDeferred(mapboxNavigation)

                    assertTrue(
                        "the test expects that alternative routes are present",
                        mapboxNavigation.getNavigationRoutes().size > 1
                    )
                    assertTrue(alternativesCallbackResultBeforeSetRoute.isActive)
                    alternativesCallbackResultBeforeSetRoute.cancel()
                    assertTrue(alternativesCallbackResultAfterSetRoute.isActive)
                    alternativesCallbackResultAfterSetRoute.cancel()
                }
            }
        }

    /**
     * The fact that the SDK triggers a callback on alternatives subscription I consider as a bug:
     * 1. We recommend users to set updated alternatives back, and it doesn't make sense to trigger
     * the callback with the same alternative as they are now.
     * 2. The SDK doesn't trigger callback if routes were set already, so in general it feels
     * inconsistent
     */
    @Test
    fun alternatives_observer_is_called_upon_subscription_if_route_was_set_without_subscription() =
        sdkTest {
            setupMockRequestHandlers()
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule
            ) { mapboxNavigation ->
                val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
                val originOfTestRoute = testRoutes.first().routeOptions.coordinatesList().first()
                stayOnPosition(
                    latitude = originOfTestRoute.latitude(),
                    longitude = originOfTestRoute.longitude(),
                    bearing = 30.0f
                ) {
                    mapboxNavigation.startTripSession()
                    mapboxNavigation.setNavigationRoutesAsync(testRoutes)
                    mapboxNavigation.routeProgressUpdates().first()

                    var firstSubscriberResult: List<NavigationRoute>? = null
                    mapboxNavigation.registerRouteAlternativesObserver(
                        object : NavigationRouteAlternativesObserver {
                            override fun onRouteAlternatives(
                                routeProgress: RouteProgress,
                                alternatives: List<NavigationRoute>,
                                routerOrigin: RouterOrigin
                            ) {
                                firstSubscriberResult = alternatives
                            }

                            override fun onRouteAlternativesError(error: RouteAlternativesError) {
                            }
                        }
                    )

                    val secondSubscriber = mapboxNavigation.alternativesUpdates()
                        .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                        .first()
                    assertEquals(testRoutes[1].id, firstSubscriberResult?.single()?.id)
                    assertEquals(testRoutes[1].id, secondSubscriber.alternatives.single().id)
                }
            }
        }

    @Test
    fun external_alternatives_set_do_not_affect_observers() =
        sdkTest {
            setupMockRequestHandlers()
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule
            ) { mapboxNavigation ->
                val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
                val originOfTestRoute = testRoutes.first().routeOptions.coordinatesList().first()
                stayOnPosition(
                    latitude = originOfTestRoute.latitude(),
                    longitude = originOfTestRoute.longitude(),
                    bearing = 30.0f
                ) {
                    mapboxNavigation.startTripSession()

                    // TODO remove non-empty filter after NN-757 is done
                    val alternativesCallbackResultBeforeSetRoute =
                        firstNonEmptyAlternativesUpdateDeferred(mapboxNavigation)
                    mapboxNavigation.setNavigationRoutesAsync(testRoutes)
                    mapboxNavigation.routeProgressUpdates().first()
                    // TODO remove non-empty filter after NN-757 is done
                    val alternativesCallbackResultAfterSetRoute =
                        firstNonEmptyAlternativesUpdateDeferred(mapboxNavigation)
                    val externalAlternatives = createExternalAlternatives()
                    mapboxNavigation.setNavigationRoutesAsync(
                        testRoutes + externalAlternatives
                    )
                    mapboxNavigation.routeProgressUpdates().first()

                    val currentRoutes = mapboxNavigation.getNavigationRoutes()
                    assertTrue(
                        "initial and one of external alternatives should be present," +
                            " actual ${currentRoutes.map { it.id }}",
                        mapboxNavigation.getNavigationRoutes().size > 2
                    )
                    assertTrue(alternativesCallbackResultBeforeSetRoute.isActive)
                    alternativesCallbackResultBeforeSetRoute.cancel()
                    assertTrue(alternativesCallbackResultAfterSetRoute.isActive)
                    alternativesCallbackResultAfterSetRoute.cancel()
                }
            }
        }

    private fun createExternalAlternatives(): List<NavigationRoute> {
        return NavigationRoute.create(
            readRawFileText(context, R.raw.route_response_alternative_continue),
            "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                "-122.27522%2C37.805862%3B-122.2647245%2C37.8138895" +
                "?alternatives=true&annotations=congestion" +
                "%2Ccongestion_numeric&banner_instructions=true&geometries=polyline6" +
                "&language=en&overview=full&steps=true&voice_instructions=true" +
                "&voice_units=metric&access_token=YOUR_MAPBOX_ACCESS_TOKEN",
            RouterOrigin.Offboard
        )
    }

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
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_continue),
                continueCoordinates,
            )
        )
    }

    private suspend fun MapboxNavigation.requestNavigationRoutes(
        coordinates: List<Point>
    ): List<NavigationRoute> {
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

private fun CoroutineScope.firstAlternativesUpdateDeferred(mapboxNavigation: MapboxNavigation) =
    async(start = CoroutineStart.UNDISPATCHED) {
        mapboxNavigation.alternativesUpdates()
            .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
            .first()
    }

private fun CoroutineScope.firstNonEmptyAlternativesUpdateDeferred(
    mapboxNavigation: MapboxNavigation
) =
    async(start = CoroutineStart.UNDISPATCHED) {
        mapboxNavigation.alternativesUpdates()
            .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
            .filterNot { it.alternatives.isEmpty() }
            .first()
    }
