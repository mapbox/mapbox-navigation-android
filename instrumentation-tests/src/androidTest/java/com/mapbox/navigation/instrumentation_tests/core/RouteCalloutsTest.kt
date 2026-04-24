package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.times
import kotlin.time.toDuration

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalMapboxNavigationAPI::class)
class RouteCalloutsTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val clearFilesRule = ClearFilesRule()

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

    /**
     * Validates callouts at the start of a route with an initial alternative.
     *
     * Confirms metadata from Nav SDK produces one callout per route with a single primary and
     * that passing empty metadata with alternatives present returns no callouts (guard behavior).
     */
    @Test
    fun callouts_at_start_of_route_with_initial_alternative() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            assertTrue(
                "test expects at least one alternative in the initial response",
                testRoutes.size > 1,
            )
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.setNavigationRoutes(testRoutes)
            mapboxNavigation.routeProgressUpdates().first()

            val metadata = collectAlternativeMetadata(mapboxNavigation, testRoutes)
            val calloutsApi = MapboxRouteCalloutsApi()

            val callouts = calloutsApi.setNavigationRoutes(testRoutes, metadata).callouts
            assertEquals(testRoutes.size, callouts.size)
            assertEquals(1, callouts.count { it.isPrimary })

            val guarded = calloutsApi.setNavigationRoutes(testRoutes, emptyList()).callouts
            assertTrue(
                "callouts must be empty when metadata is missing and alternatives are present",
                guarded.isEmpty(),
            )
        }
    }

    /**
     * Validates callouts while driving mid-route on the initial alternative set.
     *
     * Confirms metadata remains usable after some progress, so callouts still resolve with a
     * single primary and the full alternative count.
     */
    @Test
    fun callouts_mid_route_with_initial_alternative() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.setNavigationRoutes(testRoutes)

            mapboxNavigation.routeProgressUpdates().first { it.distanceTraveled >= 50f }

            val metadata = collectAlternativeMetadata(mapboxNavigation, testRoutes)
            assertEquals(
                "metadata is expected for every alternative mid-route",
                testRoutes.size - 1,
                metadata.size,
            )

            val callouts = MapboxRouteCalloutsApi()
                .setNavigationRoutes(testRoutes, metadata)
                .callouts
            assertEquals(testRoutes.size, callouts.size)
            assertEquals(1, callouts.count { it.isPrimary })
        }
    }

    /**
     * Validates callouts after Nav SDK receives a new continuous alternative from NN.
     *
     * Confirms the callouts API accepts the updated route list and metadata produced by NN after
     * the fork, keeping one primary and emitting callouts for every route.
     */
    @Test
    fun callouts_after_receiving_new_continuous_alternative() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.setNavigationRoutes(testRoutes)

            val updatedRoutes = mapboxNavigation.routesUpdates()
                .filter { it.navigationRoutes != testRoutes }
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
                .filter { it.navigationRoutes.size > 1 }
                .first()
                .navigationRoutes

            val metadata = collectAlternativeMetadata(mapboxNavigation, updatedRoutes)
            val callouts = MapboxRouteCalloutsApi()
                .setNavigationRoutes(updatedRoutes, metadata)
                .callouts

            assertEquals(updatedRoutes.size, callouts.size)
            assertEquals(1, callouts.count { it.isPrimary })
        }
    }

    /**
     * Validates the duration difference shown by callouts after driving on a CA for some time.
     */
    @Test
    fun callouts_while_driving_on_continuous_alternative_for_some_time() = sdkTest {
        setupMockRequestHandlers()
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { mapboxNavigation ->
            val testRoutes = mapboxNavigation.requestNavigationRoutes(startCoordinates)
            mockLocationReplayerRule.playRoute(testRoutes.first().directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.flowLocationMatcherResult().first()
            mapboxNavigation.setNavigationRoutes(testRoutes)

            val updatedRoutes = mapboxNavigation.routesUpdates()
                .filter { it.navigationRoutes != testRoutes }
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE }
                .filter { it.navigationRoutes.size > 1 }
                .first()
                .navigationRoutes

            val distanceAtCa = mapboxNavigation.routeProgressUpdates().first().distanceTraveled
            mapboxNavigation.routeProgressUpdates().first {
                it.distanceTraveled >= distanceAtCa + 30f
            }

            val metadata = collectAlternativeMetadata(mapboxNavigation, updatedRoutes)
            val callouts = MapboxRouteCalloutsApi()
                .setNavigationRoutes(updatedRoutes, metadata)
                .callouts
            assertEquals(updatedRoutes.size, callouts.size)

            val primaryDuration = updatedRoutes.first().directionsRoute.duration().seconds
            val primaryCallout = callouts.single { it.isPrimary }
            assertEquals(
                "primary callout must have zero duration difference",
                Duration.ZERO,
                primaryCallout.durationDifferenceWithPrimary,
            )

            val metadataById = metadata.associateBy { it.navigationRoute.id }
            callouts.filter { !it.isPrimary }.forEach { callout ->
                val altMetadata = metadataById[callout.route.id]
                assertNotNull("metadata missing for ${callout.route.id}", altMetadata)
                val altDuration = altMetadata!!.infoFromStartOfPrimary.duration.seconds
                val expected = calculateDurationDifference(primaryDuration, altDuration)

                assertEquals(
                    "unexpected duration difference for ${callout.route.id}",
                    expected,
                    callout.durationDifferenceWithPrimary,
                )
            }
        }
    }

    private fun collectAlternativeMetadata(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
    ): List<AlternativeRouteMetadata> {
        return routes.drop(1).map { alternative ->
            val md = mapboxNavigation.getAlternativeMetadataFor(alternative)
            assertNotNull("alternative $alternative must have metadata", md)
            md!!
        }
    }

    private fun setupMockRequestHandlers() {
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
            .baseUrl(mockWebServerRule.baseUrl)
            .build()
        return requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
    }

    private fun calculateDurationDifference(
        primaryDuration: Duration,
        alternativeDuration: Duration,
    ): Duration {
        val durationDiff =
            (primaryDuration - alternativeDuration).toDouble(DurationUnit.MINUTES)

        return sign(durationDiff) * ceil(abs(durationDiff))
            .toDuration(DurationUnit.MINUTES)
    }
}
