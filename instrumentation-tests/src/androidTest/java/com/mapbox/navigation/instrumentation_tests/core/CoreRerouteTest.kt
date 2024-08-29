package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.os.Looper
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.reroute.RerouteOptionsAdapter
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.offRouteUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStates
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForAlternativesUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.DelayedResponseModifier
import com.mapbox.navigation.testing.utils.assertions.RerouteStateTransitionAssertion
import com.mapbox.navigation.testing.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMapboxNavigationAPI::class)
class CoreRerouteTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @Before
    fun setup() {
        Espresso.onIdle()
    }

    override fun setupMockLocation(): Location {
        val mockRoute = RoutesProvider.dc_very_short(context)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun reroute_completes() {
        // prepare
        val mapboxNavigation = createMapboxNavigation()
        val locationTrackingIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.TRACKING,
        )
        val offRouteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.OFF_ROUTE,
        )
        val mockRoute = RoutesProvider.dc_very_short(context)
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(context, R.raw.reroute_response_dc_very_short),
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
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions,
                    ) {
                        logE("onFailure reasons=$reasons", "DEBUG")
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        logE("onCanceled", "DEBUG")
                    }
                },
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

        runBlocking(Dispatchers.Main) {
            val historyPath = mapboxNavigation.historyRecorder.stopRecording()
            logE("history path=$historyPath", "DEBUG")
        }

        // assert results
        expectedStates.assert()

        runOnMainSync {
            val newWaypoints = mapboxNavigation.getNavigationRoutes().first()
                .directionsRoute.routeOptions()!!.coordinatesList()
            check(newWaypoints.size == 2) {
                "Expected 2 waypoints in the route after reroute but was ${newWaypoints.size}"
            }
            check(newWaypoints[1] == mockRoute.routeWaypoints.last())
        }
    }

    @Test
    fun reroute_is_cancelled_when_the_user_returns_to_route() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short(context)
        val originLocation = mockRoute.routeWaypoints.first()
        val initialLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude()
            longitude = originLocation.longitude()
        }
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val requestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = mockRoute.routeResponseJson,
            expectedCoordinates = mockRoute.routeWaypoints,
            relaxedExpectedCoordinates = false,
        )
        val rerouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(context, R.raw.empty_directions_response),
            expectedCoordinates = listOf(
                Point.fromLngLat(
                    offRouteLocationUpdate.longitude,
                    offRouteLocationUpdate.latitude,
                ),
                mockRoute.routeWaypoints.last(),
            ),
            relaxedExpectedCoordinates = true,
        )
        val responseModifier = DelayedResponseModifier(10000)
        rerouteRequestHandler.jsonResponseModifier = responseModifier
        mockWebServerRule.requestHandlers.add(requestHandler)
        mockWebServerRule.requestHandlers.add(rerouteRequestHandler)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!,
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.Interrupted)
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints)
                .build(),
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(originalRoutes)

        mockLocationReplayerRule.loopUpdate(offRouteLocationUpdate, times = 5)
        // wait for OFF_ROUTE
        mapboxNavigation.offRouteUpdates().filter { it }.first()
        mockLocationReplayerRule.loopUpdate(initialLocation, times = 120)
        // wait until the puck returns to the route
        mapboxNavigation.offRouteUpdates().filterNot { it }.first()
        delay(2000)
        responseModifier.interruptDelay()

        rerouteStateTransitionAssertion.assert()
    }

    @Test(timeout = 10_000)
    fun reroute_is_not_cancelled_when_alternatives_change() = sdkTest {
        // setting to 2s as NN router's default timeout at the time of creating the test is 5s
        val rerouteResponseDelay = 2_000L
        // delay before setting alternatives
        // to let the off-route process fully start, but less than reroute response delay
        val alternativesGenerationDelay = 1_000L

        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_with_alternative(context)
        val mockReroute = RoutesProvider.dc_short_with_alternative_reroute(context)
        val testData = prepareRouteAndRerouteWithAlternatives(
            mockRoute = mockRoute,
            mockReroute = mockReroute,
            // setting to 4s as NN router's default timeout at the time of creating the test is 5s
            rerouteResponseDelay = rerouteResponseDelay,
        )

        mockLocationReplayerRule.loopUpdate(testData.originLocation, times = 120)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!,
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.RouteFetched(RouterOrigin.ONLINE))
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            testData.originRouteOptions,
        ).getSuccessfulResultOrThrowException().routes

        // start session and set original route
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(originalRoutes.first()))

        // wait for tracking status
        mapboxNavigation.routeProgressUpdates().filter {
            it.currentState == RouteProgressState.TRACKING
        }.first()

        // wait for OFF_ROUTE
        mockLocationReplayerRule.stopAndClearEvents()
        mockLocationReplayerRule.loopUpdate(testData.offRouteLocation, times = 120)
        mapboxNavigation.offRouteUpdates().filter { it }.first()

        // add an alternative route while reroute is in progress
        delay(alternativesGenerationDelay)
        mapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(
            mapboxNavigation.getNavigationRoutes() + originalRoutes[1],
        )
        assertEquals(
            originalRoutes,
            mapboxNavigation.getNavigationRoutes(),
        )

        // wait for reroute to complete meaning that changing alternatives didn't cancel reroute
        val rerouteUpdate = mapboxNavigation.routesUpdates().first {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
        }

        // assert
        assertEquals(
            mockReroute.routeWaypoints,
            rerouteUpdate.navigationRoutes.first().waypoints!!.map { it.location() },
        )
        rerouteStateTransitionAssertion.assert()
    }

    @Test(timeout = 10_000)
    fun reroute_is_not_cancelled_when_route_refreshed() = sdkTest {
        // setting to 2s as NN router's default timeout at the time of creating the test is 5s
        val rerouteResponseDelay = 2_000L
        // setting to 1s to be less than reroute response delay
        // which should make refresh return before reroute if dispatched at a similar time
        val refreshInterval = 1_000L

        val mapboxNavigation = createMapboxNavigation(
            customRefreshInterval = refreshInterval,
        )
        val mockRoute = RoutesProvider.dc_short_with_alternative(context)
        val mockReroute = RoutesProvider.dc_short_with_alternative_reroute(context)
        val testData = prepareRouteAndRerouteWithAlternatives(
            mockRoute = mockRoute,
            mockReroute = mockReroute,
            rerouteResponseDelay = rerouteResponseDelay,
        )

        mockLocationReplayerRule.loopUpdate(testData.originLocation, times = 120)

        val refreshHandler = MockDirectionsRefreshHandler(
            testUuid = "jpCHHUC26qFOwISCNLjar2xmTfI6Dxd0qCHOoqwt_1VAlESNvsr7Zg==",
            readRawFileText(
                context,
                R.raw.route_response_dc_short_with_alternative_refresh_route_0,
            ),
            acceptedGeometryIndex = 0,
        )
        mockWebServerRule.requestHandlers.add(refreshHandler)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!,
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.RouteFetched(RouterOrigin.ONLINE))
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            testData.originRouteOptions,
        ).getSuccessfulResultOrThrowException().routes

        // start session and set original route
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(originalRoutes.first()))

        // wait for tracking status
        mapboxNavigation.routeProgressUpdates().filter {
            it.currentState == RouteProgressState.TRACKING
        }.first()

        // wait for OFF_ROUTE
        mockLocationReplayerRule.stopAndClearEvents()
        mockLocationReplayerRule.loopUpdate(testData.offRouteLocation, times = 120)
        mapboxNavigation.offRouteUpdates().filter { it }.first()

        // wait for refresh update,
        // based on delay configuration this should happen before reroute finishes
        mapboxNavigation.routesUpdates().first {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
        }

        // wait for reroute to complete meaning that refresh didn't cancel reroute
        val routesUpdate = mapboxNavigation.routesUpdates().first {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
        }

        // assert
        assertEquals(
            mockReroute.routeWaypoints,
            routesUpdate.navigationRoutes.first().waypoints!!.map { it.location() },
        )
        rerouteStateTransitionAssertion.assert()
    }

    @Test
    fun user_triggers_reroute_to_change_route_options_current_api() = sdkTest {
        val mockRoute = RoutesProvider.dc_very_short(context)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { navigation ->
            val routeOptions = RouteOptions.builder()
                .coordinatesList(
                    mockRoute.routeWaypoints,
                )
                // comment the next line to use a real server
                .baseUrl(
                    mockWebServerRule.baseUrl,
                )
                .applyDefaultNavigationOptions()
                .build()
            val routes = navigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            assertNotEquals(
                DirectionsCriteria.EXCLUDE_FERRY,
                routes.first().routeOptions.exclude(),
            )
            navigation.setRerouteOptionsAdapter(
                object : RerouteOptionsAdapter {
                    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
                        // user setup route options according current settings
                        return routeOptions.toBuilder()
                            .exclude(DirectionsCriteria.EXCLUDE_FERRY)
                            .build()
                    }
                },
            )
            stayOnPosition(
                mockRoute.routeWaypoints.first(),
                0.0f,
            ) {
                navigation.startTripSession()
                navigation.setNavigationRoutes(routes)
                navigation.routeProgressUpdates().first()
                suspendCoroutine { continuation ->
                    navigation.getRerouteController()!!.reroute { routes, _ ->
                        navigation.setNavigationRoutes(routes) {
                            continuation.resume(Unit)
                        }
                    }
                }
                val route = navigation.getNavigationRoutes().first()
                assertEquals(
                    DirectionsCriteria.EXCLUDE_FERRY,
                    route.routeOptions.exclude(),
                )
            }
        }
    }

    @Test
    fun user_triggers_reroute_to_change_route_options_new_api() = sdkTest {
        val mockRoute = RoutesProvider.dc_very_short(context)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { navigation ->
            val routeOptions = RouteOptions.builder()
                .coordinatesList(
                    mockRoute.routeWaypoints,
                )
                // comment the next line to use a real server
                .baseUrl(
                    mockWebServerRule.baseUrl,
                )
                .applyDefaultNavigationOptions()
                .build()
            val routes = navigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            assertNotEquals(
                DirectionsCriteria.EXCLUDE_FERRY,
                routes.first().routeOptions.exclude(),
            )
            navigation.setRerouteOptionsAdapter(
                object : RerouteOptionsAdapter {
                    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
                        // user setup route options according current settings
                        return routeOptions.toBuilder()
                            .exclude(DirectionsCriteria.EXCLUDE_FERRY)
                            .build()
                    }
                },
            )
            stayOnPosition(
                mockRoute.routeWaypoints.first(),
                0.0f,
            ) {
                navigation.startTripSession()
                navigation.setNavigationRoutes(routes)
                navigation.routeProgressUpdates().first()
                navigation.replanRoute()
                val routeUpdate = navigation.routesUpdates()
                    .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                val route = routeUpdate.navigationRoutes.first()
                assertEquals(
                    DirectionsCriteria.EXCLUDE_FERRY,
                    route.routeOptions.exclude(),
                )
            }
        }
    }

    @Test
    fun reroute_on_single_leg_route_without_alternatives() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short(context)
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(context, R.raw.reroute_response_dc_very_short),
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

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build(),
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(0, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
    }

    @Test
    fun reroute_on_multieg_route_without_alternatives() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_very_short_two_legs(context)
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
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = readRawFileText(
                        context,
                        R.raw.reroute_response_dc_very_short_two_legs,
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

            mapboxNavigation.startTripSession()
            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes
            mapboxNavigation.setNavigationRoutes(routes)

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
                (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                    if (it) {
                        assertEquals(0, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
        }
    }

    @Test
    fun reroute_on_multileg_route_with_waypoint_names_and_targets_without_indices() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short_two_legs(context)
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
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(
                    context,
                    R.raw.reroute_response_dc_very_short_two_legs,
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

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints)
                .waypointNames("waypoint1;waypoint2;waypoint3")
                .waypointTargetsList(mockRoute.routeWaypoints)
                .build(),
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

        mockLocationReplayerRule.loopUpdateUntil(originalLocation) {
            mapboxNavigation.routeProgressUpdates().first()
        }
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

        mapboxNavigation.routesUpdates().filter { result ->
            (result.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(
                        ";waypoint3",
                        result.navigationRoutes.first().routeOptions.waypointNames(),
                    )
                    assertEquals(
                        listOf(null, mockRoute.routeWaypoints[2]),
                        result.navigationRoutes.first().routeOptions.waypointTargetsList(),
                    )
                }
            }
        }.first()
    }

    @Test
    fun reroute_on_single_leg_route_with_alternatives() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_short_with_alternative_same_beginning(context)

            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .alternatives(true)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes

            mockLocationReplayerRule.playRoute(routes[1].directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)

            val rerouteResult = mapboxNavigation.routesUpdates().filter {
                (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                    if (it) {
                        assertEquals(0, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
            assertEquals(routes[1], rerouteResult.navigationRoutes.first())

            val removePassedAlternativeUpdate = mapboxNavigation.routesUpdates().first {
                it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE &&
                    !it.navigationRoutes.contains(routes[0])
            }
        }
    }

    @Test
    @Ignore("https://mapbox.atlassian.net/browse/NN-1427")
    fun reroute_on_multileg_route_first_leg_with_alternatives() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_short_two_legs_with_alternative(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes

            mockLocationReplayerRule.playRoute(routes[1].directionsRoute)
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutesAsync(routes)

            val rerouteResult = mapboxNavigation.routesUpdates().filter {
                (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                    if (it) {
                        assertEquals(0, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
            assertEquals(routes[1], rerouteResult.navigationRoutes.first())
        }
    }

    @Test
    fun reroute_from_single_leg_primary_to_multileg_alternative() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_short_alternative_has_more_legs(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

            val routes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .waypointsPerRoute(true)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
            ).getSuccessfulResultOrThrowException().routes

            mockLocationReplayerRule.playRoute(
                routes[1].directionsRoute,
                eventsToDrop = 15,
            )
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)

            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentLegProgress?.legIndex == 0 && it.currentRouteGeometryIndex > 5 }
                .first()

            val rerouteResult = mapboxNavigation.routesUpdates().filter {
                (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                    if (it) {
                        assertEquals(1, mapboxNavigation.currentLegIndex())
                    }
                }
            }.first()
            assertEquals(routes[1], rerouteResult.navigationRoutes.first())
        }
    }

    // Restarting mock web server takes up to 30 seconds.
    // Let it finish and skip the test if it did not succeed.
    @Test
    fun reroute_with_retryable_error() = sdkTest(timeout = 50_000) {
        val mockRoute = RoutesProvider.dc_very_short(context)
        val originLocation = mockRoute.routeWaypoints.first()
        val initialLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude()
            longitude = originLocation.longitude()
        }
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(context, R.raw.reroute_response_dc_very_short),
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
        ) { mapboxNavigation ->
            val originalRoutes = mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints)
                    .build(),
            ).getSuccessfulResultOrThrowException().routes
            stayOnPosition(initialLocation) {
                mapboxNavigation.startTripSession()
                mapboxNavigation.flowLocationMatcherResult().first()
                mapboxNavigation.setNavigationRoutesAndWaitForUpdate(originalRoutes)
            }
            withoutInternet {
                stayOnPosition(offRouteLocationUpdate) {
                    mapboxNavigation.offRouteUpdates().filter { it }.first()
                    val failedState = mapboxNavigation.getRerouteController()!!
                        .rerouteStates()
                        .filterIsInstance<RerouteState.Failed>()
                        .first()
                    assertTrue(failedState.isRetryable)
                }
            }
            stayOnPosition(offRouteLocationUpdate) {
                mapboxNavigation.getRerouteController()!!
                    .reroute { routes, _ ->
                        mapboxNavigation.setNavigationRoutes(routes)
                    }

                mapboxNavigation.routesUpdates()
                    .drop(1) // skipping initial route
                    .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW }
            }
        }
    }

    private fun createMapboxNavigation(customRefreshInterval: Long? = null): MapboxNavigation {
        var mapboxNavigation: MapboxNavigation? = null

        fun create(): MapboxNavigation {
            MapboxNavigationProvider.destroy()
            val navigationOptions = NavigationOptions.Builder(context)
                .historyRecorderOptions(
                    HistoryRecorderOptions.Builder()
                        .build(),
                )
                .routingTilesOptions(
                    RoutingTilesOptions.Builder()
                        .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                        .build(),
                ).apply {
                    if (customRefreshInterval != null) {
                        val customRefreshOptions = RouteRefreshOptions.Builder()
                            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                            .build()
                        RouteRefreshOptions::class.java.getDeclaredField(
                            "intervalMillis",
                        ).apply {
                            isAccessible = true
                            set(customRefreshOptions, 3_000L)
                        }
                        routeRefreshOptions(customRefreshOptions)
                    }
                }
                .build()
            return MapboxNavigationProvider.create(navigationOptions).also {
                mapboxHistoryTestRule.historyRecorder = it.historyRecorder
            }
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnMainSync {
                mapboxNavigation = create()
            }
        } else {
            mapboxNavigation = create()
        }
        return mapboxNavigation!!
    }

    private data class RerouteTestData(
        val originLocation: Location,
        val offRouteLocation: Location,
        val originRouteOptions: RouteOptions,
    )

    private fun prepareRouteAndRerouteWithAlternatives(
        mockRoute: MockRoute,
        mockReroute: MockRoute,
        rerouteResponseDelay: Long? = null,
    ): RerouteTestData {
        val originPoint = mockRoute.routeWaypoints.first()
        val originLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originPoint.latitude()
            longitude = originPoint.longitude()
        }

        val offRoutePoint = mockReroute.routeWaypoints.first()
        val offRouteLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = offRoutePoint.latitude()
            longitude = offRoutePoint.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.addAll(
            mockReroute.mockRequestHandlers.also { handlers ->
                rerouteResponseDelay?.let { delay ->
                    handlers.filterIsInstance<MockDirectionsRequestHandler>().forEach { handler ->
                        val responseModifier = DelayedResponseModifier(delay)
                        handler.jsonResponseModifier = responseModifier
                    }
                }
            },
        )

        val originalRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .baseUrl(mockWebServerRule.baseUrl)
            .coordinatesList(mockRoute.routeWaypoints)
            .alternatives(true)
            .build()

        return RerouteTestData(
            originLocation = originLocation,
            offRouteLocation = offRouteLocation,
            originRouteOptions = originalRouteOptions,
        )
    }
}
