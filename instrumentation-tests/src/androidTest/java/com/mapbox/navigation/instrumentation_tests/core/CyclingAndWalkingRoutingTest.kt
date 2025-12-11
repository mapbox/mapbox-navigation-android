package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_CYCLING
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_WALKING
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.assertions.assertSuccessfulRerouteStateTransition
import com.mapbox.navigation.testing.utils.assertions.assertSuccessfulRouteAppliedRerouteStateTransition
import com.mapbox.navigation.testing.utils.assertions.recordRerouteStates
import com.mapbox.navigation.testing.utils.assertions.recordRerouteStatesV2
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.navigation.utils.internal.toPoint
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(Parameterized::class)
class CyclingAndWalkingRoutingTest(private val directionsProfile: String) :
    BaseCoreNoCleanUpTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Profile={0}")
        fun provideDirectionProfiles(): Collection<String> = listOf(
            PROFILE_CYCLING,
            PROFILE_WALKING,
        )
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    /**
     * Files cleanup is required because tests that use [withoutInternet] in this class
     * expect navigator to not have enough tiles to build an offline route and fail
     * building a route.
     */
    @get:Rule
    val clearFilesRule = ClearFilesRule()

    @Before
    fun setup() {
        Espresso.onIdle()
    }

    private val testInitialLocation by lazy {
        RoutesProvider.cycling_route_dc_very_short(context)
            .routeWaypoints.first()
    }

    override fun setupMockLocation(): Location {
        val initialLocation = testInitialLocation
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = initialLocation.latitude()
            longitude = initialLocation.longitude()
        }
    }

    @Test
    fun reroute_completes() = sdkTest {
        val mockRoute = retrieveMatchingRouteFor(
            directionsProfile,
            cyclingMockRoute = RoutesProvider.cycling_route_dc_very_short(context),
            walkingMockRoute = RoutesProvider.walking_route_dc_very_short(context),
        )
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = directionsProfile,
                jsonResponse = retrieveMatchingRouteJsonFor(
                    directionsProfile,
                    cyclingRouteJson = R.raw.cycling_route_response_dc_very_short,
                    walkingRouteJson = R.raw.walking_route_response_dc_very_short,
                ),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude,
                        offRouteLocationUpdate.latitude,
                    ),
                    mockRoute.routeWaypoints.last(),
                ),
                relaxedExpectedCoordinates = true,
            ),
        )

        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = navigation.recordRerouteStates()
            val rerouteStatesV2 = navigation.recordRerouteStatesV2()
            val routes = stayOnPosition(originLocation, bearing = 0.0f) {
                navigation.startTripSession()
                navigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions(directionsProfile)
                        .applyLanguageAndVoiceUnitOptions(context)
                        .baseUrl(mockWebServerRule.baseUrl)
                        .coordinatesList(mockRoute.routeWaypoints)
                        .build(),
                ).getSuccessfulResultOrThrowException().routes
            }
            navigation.setNavigationRoutesAsync(routes)
            navigation.moveAlongTheRouteUntilTracking(
                routes[0],
                mockLocationReplayerRule,
                3,
            )
            stayOnPosition(offRouteLocationUpdate) {
                val routesUpdate = navigation.routesUpdates().first {
                    it.reason == ROUTES_UPDATE_REASON_REROUTE
                }
                assertSuccessfulRerouteStateTransition(rerouteStates)
                assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
                val newWaypoints = routesUpdate.navigationRoutes.first()
                    .directionsRoute.routeOptions()!!.coordinatesList()
                assertEquals(2, newWaypoints.size)
                assertEquals(mockRoute.routeWaypoints.last(), newWaypoints[1])
                navigation.routeProgressUpdates().first {
                    it.currentState == RouteProgressState.TRACKING
                }
            }
        }
    }

    @Test
    fun reroute_on_multileg_route_without_alternatives() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = retrieveMatchingRouteFor(
                directionsProfile,
                cyclingMockRoute = RoutesProvider.cycling_dc_very_short_two_legs(context),
                walkingMockRoute = RoutesProvider.walking_route_dc_very_short_two_legs(context),
            )
            val originalLocation = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
            val secondLegLocation = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = mockRoute.routeWaypoints[1].latitude()
                longitude = mockRoute.routeWaypoints[1].longitude()
            }
            val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = secondLegLocation.latitude + 0.002
                longitude = secondLegLocation.longitude
            }

            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRequestHandler(
                    profile = directionsProfile,
                    jsonResponse = retrieveMatchingRouteJsonFor(
                        profile = directionsProfile,
                        cyclingRouteJson = R.raw.cycling_route_response_dc_very_short_two_legs,
                        walkingRouteJson = R.raw.walking_route_response_dc_very_short_two_legs,
                    ),
                    expectedCoordinates = listOf(
                        Point.fromLngLat(
                            offRouteLocationUpdate.longitude,
                            offRouteLocationUpdate.latitude,
                        ),
                        mockRoute.routeWaypoints.last(),
                    ),
                    relaxedExpectedCoordinates = true,
                ),
            )
            val rerouteStates = mapboxNavigation.recordRerouteStates()
            val rerouteStatesV2 = mapboxNavigation.recordRerouteStatesV2()

            mapboxNavigation.startTripSession()
            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions(directionsProfile)
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes
            mapboxNavigation.setNavigationRoutes(routes)

            mapboxNavigation.moveAlongTheRouteUntilTracking(
                routes[0],
                mockLocationReplayerRule,
            )
            mockLocationReplayerRule.loopUpdateUntil(originalLocation) {
                mapboxNavigation.routeProgressUpdates().first()
            }
            mapboxNavigation.routeProgressUpdates().first()
            mapboxNavigation.navigateNextRouteLeg()
            mockLocationReplayerRule.loopUpdateUntil(secondLegLocation) {
                mapboxNavigation.routeProgressUpdates()
                    .filter { it.currentLegProgress?.legIndex == 1 }
                    .first()
            }
            mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
                mapboxNavigation.routeProgressUpdates()
                    .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                    .first()
            }

            mapboxNavigation.routesUpdates().filter {
                (it.reason == ROUTES_UPDATE_REASON_REROUTE).also { didUpdate ->
                    if (didUpdate) {
                        assertEquals(0, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
            assertSuccessfulRerouteStateTransition(rerouteStates)
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    @Test
    fun reroute_on_multileg_route_first_leg_with_alternatives() = sdkTest(timeout = 40_000) {
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { mapboxNavigation ->
            // Skip this test for walking profile, as it doesn't support alternatives.
            assumeTrue(directionsProfile != PROFILE_WALKING)

            val rerouteStates = mapboxNavigation.recordRerouteStates()
            val rerouteStatesV2 = mapboxNavigation.recordRerouteStatesV2()
            val mockRoute = RoutesProvider.cycling_dc_short_two_legs_with_alternative(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions(directionsProfile)
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes

            mockLocationReplayerRule.playRoute(routes[1].directionsRoute)
            mapboxNavigation.startTripSession()
            // make sure new initial location is overridden by new location updates
            mapboxNavigation.flowLocationMatcherResult().first {
                TurfMeasurement.distance(
                    it.enhancedLocation.toPoint(),
                    testInitialLocation,
                ) > 0.1
            }
            mapboxNavigation.setNavigationRoutesAsync(routes)

            val rerouteResult = mapboxNavigation.routesUpdates().filter {
                (it.reason == ROUTES_UPDATE_REASON_REROUTE).also { didUpdate ->
                    if (didUpdate) {
                        assertEquals(0, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
            assertEquals(routes[1], rerouteResult.navigationRoutes.first())
            assertSuccessfulRerouteStateTransition(rerouteStates)
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    private fun retrieveMatchingRouteJsonFor(
        profile: String,
        cyclingRouteJson: Int,
        walkingRouteJson: Int,
    ): String =
        when (profile) {
            PROFILE_CYCLING -> {
                readRawFileText(
                    context,
                    cyclingRouteJson,
                )
            }

            PROFILE_WALKING -> {
                readRawFileText(
                    context,
                    walkingRouteJson,
                )
            }

            else -> {
                readRawFileText(context, walkingRouteJson)
            }
        }

    private fun retrieveMatchingRouteFor(
        profile: String,
        cyclingMockRoute: MockRoute,
        walkingMockRoute: MockRoute,
    ): MockRoute =
        when (profile) {
            PROFILE_CYCLING -> cyclingMockRoute
            PROFILE_WALKING -> walkingMockRoute
            else -> cyclingMockRoute
        }
}
