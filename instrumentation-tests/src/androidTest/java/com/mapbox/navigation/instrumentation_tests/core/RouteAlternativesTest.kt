package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.http.MockDynamicDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.openRawResource
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.InputStreamReader
import java.net.URL

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalMapboxNavigationAPI::class)
class RouteAlternativesTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private val startCoordinates = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895),
    )
    private val continueCoordinates = listOf(
        Point.fromLngLat(-122.275220, 37.805862),
        Point.fromLngLat(-122.2647245, 37.8138895),
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = startCoordinates[0].latitude()
        longitude = startCoordinates[0].longitude()
    }

    @Test
    fun expect_initial_alternative_route_removed_after_passing_the_fork_point() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            // make sure that new alternatives won't be returned
            mockWebServerRule.requestHandlers.add(
                0,
                MockRequestHandler {
                    MockResponse().setResponseCode(500).setBody("")
                },
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
                emptyList<NavigationRoute>(),
                firstAlternativesCallback.navigationRoutes.drop(1),
            )
        }
    }

    @Test
    fun alternatives_are_updated_after_passing_fork_point() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
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
                    mapboxNavigation.getAlternativeMetadataFor(it),
                )
            }

            val mockedAlternativesResponse = InputStreamReader(
                openRawResource(context, R.raw.route_response_alternative_continue),
            ).use {
                DirectionsResponse.fromJson(it)
            }
            newAlternatives.forEach {
                assertEquals(
                    "some info was lost during NN -> Nav SDK transition",
                    it.directionsRoute.toBuilder().routeOptions(null).build(),
                    mockedAlternativesResponse.routes()[it.routeIndex].toBuilder()
                        .legs(
                            mockedAlternativesResponse.routes()[it.routeIndex].legs()
                                ?.map { originalLeg ->
                                    originalLeg.toBuilder()
                                        .incidents(originalLeg.incidents().orEmpty())
                                        .build()
                                },
                        )
                        .build(),
                )
            }
        }
    }

    @Test
    fun invalid_continuous_alternatives_do_not_crash() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_start),
                startCoordinates,
            ),
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_continue_invalid),
                continueCoordinates,
            ),
        )

        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()

            mapboxNavigation.setNavigationRoutes(testRoutes)

            // no crash
            mapboxNavigation.routeProgressUpdates().first {
                it.distanceTraveled >= 150
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun refresh_alternatives_before_passing_a_fork_point() = sdkTest {
        val refreshedCongestionNumericValue = 20
        val freeFlowSpeedRefreshedValue = 60
        val currentSpeedRefreshedValue = 32
        val speedRefreshedValue = 77.2
        val refreshedDistanceValue = 1.0
        val refreshedDurationValue = 2.0
        val testRouteOptions = setup3AlternativesInParisWithRefresh(
            transformFreeFlowSpeed = {
                MutableList(it.size) { freeFlowSpeedRefreshedValue }
            },
            transformCongestionsNumeric = {
                MutableList(it.size) { refreshedCongestionNumericValue }
            },
            transformCurrentSpeed = {
                MutableList(it.size) { currentSpeedRefreshedValue }
            },
            transformSpeed = {
                MutableList(it.size) { speedRefreshedValue }
            },
            transformDistance = {
                MutableList(it.size) { refreshedDistanceValue }
            },
            transformDuration = {
                MutableList(it.size) { refreshedDurationValue }
            },
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestRoutes(testRouteOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            mapboxNavigation.setNavigationRoutesAsync(testRoutes)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

            val remainingAlternatives = mapboxNavigation.routesUpdates()
                .filter { it.navigationRoutes != testRoutes } // skip initial routes
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
                .filter { it.navigationRoutes.size > 1 }
                .first()
                .navigationRoutes
                .drop(1)

            remainingAlternatives.forEach {
                assertNotNull(
                    "alternative route $it doesn't have metadata",
                    mapboxNavigation.getAlternativeMetadataFor(it),
                )
            }

            remainingAlternatives.forEach {
                assertEquals(
                    "Test expects alternatives to be from the original repose",
                    testRoutes.first().responseUUID,
                    it.responseUUID,
                )
                // checking only the end because refresh happens during movement
                assertEquals(
                    MutableList(50) { refreshedCongestionNumericValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.congestionNumeric()
                        ?.takeLast(50),
                )
                assertEquals(
                    MutableList(50) { speedRefreshedValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.speed()
                        ?.takeLast(50),
                )
                assertEquals(
                    MutableList(50) { refreshedDistanceValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.distance()
                        ?.takeLast(50),
                )
                assertEquals(
                    MutableList(50) { refreshedDurationValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.duration()
                        ?.takeLast(50),
                )

                assertEquals(
                    MutableList(50) { freeFlowSpeedRefreshedValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.freeflowSpeed()
                        ?.takeLast(50),
                )
                assertEquals(
                    MutableList(50) { currentSpeedRefreshedValue },
                    it.directionsRoute.legs()
                        ?.first()
                        ?.annotation()
                        ?.currentSpeed()
                        ?.takeLast(50),
                )
            }
        }
    }

    @Test
    fun alternative_observer_is_not_called_with_current_alternatives_upon_subscription() =
        sdkTest {
            setupMockRequestHandlers()
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
            ) { mapboxNavigation ->
                val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
                val originOfTestRoute = testRoutes.first().routeOptions.coordinatesList().first()
                stayOnPosition(
                    latitude = originOfTestRoute.latitude(),
                    longitude = originOfTestRoute.longitude(),
                    bearing = 30.0f,
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
                        mapboxNavigation.getNavigationRoutes().size > 1,
                    )
                    assertTrue(alternativesCallbackResultBeforeSetRoute.isActive)
                    alternativesCallbackResultBeforeSetRoute.cancel()
                    assertTrue(alternativesCallbackResultAfterSetRoute.isActive)
                    alternativesCallbackResultAfterSetRoute.cancel()
                }
            }
        }

    @Test
    fun alternatives_observer_is_not_called_upon_subscription_if_route_was_set_before() =
        sdkTest {
            setupMockRequestHandlers()
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
            ) { mapboxNavigation ->
                val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
                val originOfTestRoute = testRoutes.first().routeOptions.coordinatesList().first()
                stayOnPosition(
                    latitude = originOfTestRoute.latitude(),
                    longitude = originOfTestRoute.longitude(),
                    bearing = 30.0f,
                ) {
                    mapboxNavigation.startTripSession()
                    mapboxNavigation.setNavigationRoutesAsync(testRoutes)
                    mapboxNavigation.routeProgressUpdates().first()

                    val alternativesCallbackResultAfterSetRoute =
                        firstAlternativesUpdateDeferred(mapboxNavigation)

                    assertTrue(
                        "the test expects that alternative routes are present",
                        mapboxNavigation.getNavigationRoutes().size > 1,
                    )
                    assertTrue(alternativesCallbackResultAfterSetRoute.isActive)
                    alternativesCallbackResultAfterSetRoute.cancel()
                }
            }
        }

    @Test
    fun alternative_requests_use_original_route_base_url() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val routes = mapboxNavigation.requestNavigationRoutes(startCoordinates)

            mockWebServerRule.requestHandlers.clear()
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRequestHandler(
                    "driving-traffic",
                    readRawFileText(context, R.raw.route_response_alternative_during_navigation),
                    startCoordinates,
                    relaxedExpectedCoordinates = true,
                ),
            )
            stayOnPosition(
                startCoordinates.first().latitude(),
                startCoordinates.first().longitude(),
                30f,
            ) {
                mapboxNavigation.startTripSession()
                mapboxNavigation.flowLocationMatcherResult().first()
            }
            mapboxNavigation.setNavigationRoutesAsync(routes)
            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)

            val alternativesUpdate = mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
                .filter {
                    val alternatives = it.navigationRoutes.drop(1)
                    alternatives.isNotEmpty() && alternatives.none {
                        it.id.startsWith("1SSd29ZxmjD7ELLqDJHRPPDP5W4wdh633IbGo41pJrL6wpJRmzNaMA==")
                    }
                }
                .first()

            assertEquals(
                "DD8MJ37zcI2gU4XXhtt-Gz1vdFShCMtf7AOyEHVylhqcEyreYNiT6Q==",
                alternativesUpdate.navigationRoutes.drop(1)
                    .firstOrNull()?.directionsRoute?.requestUuid(),
            )
        }
    }

    private fun setupMockRequestHandlers() {
        // Nav-native requests alternate routes, so we are only
        // ensuring the initial route has alternatives.
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_start),
                startCoordinates,
            ),
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternative_continue),
                continueCoordinates,
            ),
        )
    }

    private suspend fun MapboxNavigation.requestNavigationRoutes(
        coordinates: List<Point>,
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

    private fun setup3AlternativesInParisWithRefresh(
        transformFreeFlowSpeed: (List<Int>) -> List<Int> = { it },
        transformCurrentSpeed: (List<Int>) -> List<Int> = { it },
        transformCongestionsNumeric: (List<Int>) -> List<Int> = { it },
        transformDistance: (List<Double>) -> List<Double> = { it },
        transformDuration: (List<Double>) -> List<Double> = { it },
        transformSpeed: (List<Double>) -> List<Double> = { it },
    ): RouteOptions {
        val routeOptions = RouteOptions.fromUrl(
            URL(readRawFileText(context, R.raw.three_alternatives_paris_request)),
        ).toBuilder().baseUrl(mockWebServerRule.baseUrl).build()
        val fullResponse = readRawFileText(context, R.raw.three_alternatives_paris_response)
        val parsedResponse = DirectionsResponse.fromJson(fullResponse)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                routeOptions.profile(),
                fullResponse,
                routeOptions.coordinatesList(),
            ),
        )
        mockWebServerRule.requestHandlers.add(
            MockDynamicDirectionsRefreshHandler { params ->
                val leg = parsedResponse.routes()[params.routeIndex]
                    .legs()!![params.legIndex]
                val refreshAnnotations = leg.annotation()?.let { legAnnotation ->
                    legAnnotation.toBuilder()
                        .currentSpeed(
                            legAnnotation.currentSpeed()
                                ?.drop(params.geometryIndex)
                                ?.let(transformCurrentSpeed),
                        )
                        .freeflowSpeed(
                            legAnnotation.freeflowSpeed()
                                ?.drop(params.geometryIndex)
                                ?.let(transformFreeFlowSpeed),
                        )
                        .duration(
                            legAnnotation.duration()
                                ?.drop(params.geometryIndex)
                                ?.let(transformDuration),
                        )
                        .congestionNumeric(
                            legAnnotation.congestionNumeric()
                                ?.drop(params.geometryIndex)
                                ?.let(transformCongestionsNumeric),
                        )
                        .distance(
                            legAnnotation.distance()
                                ?.drop(params.geometryIndex)
                                ?.let(transformDistance),
                        )
                        .speed(
                            legAnnotation.speed()
                                ?.drop(params.geometryIndex)
                                ?.let(transformSpeed),
                        )
                        .build()
                }
                DirectionsRouteRefresh.builder().legs(
                    listOf(
                        RouteLegRefresh.builder()
                            .annotation(refreshAnnotations)
                            .incidents(leg.incidents())
                            .closures(leg.closures())
                            .build(),
                    ),
                ).build()
            },
        )
        return routeOptions
    }
}

private fun CoroutineScope.firstAlternativesUpdateDeferred(mapboxNavigation: MapboxNavigation) =
    async(start = CoroutineStart.UNDISPATCHED) {
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
            .first()
    }
