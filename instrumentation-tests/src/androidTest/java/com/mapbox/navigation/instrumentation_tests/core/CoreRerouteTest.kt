package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.os.Looper
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.DelayedResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RerouteStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.offRouteUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForAlternativesUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class CoreRerouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

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
            RouteProgressState.TRACKING
        )
        val offRouteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.OFF_ROUTE
        )
        val mockRoute = RoutesProvider.dc_very_short(activity)
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
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
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : RouterCallback {
                    override fun onRoutesReady(
                        routes: List<DirectionsRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        mapboxNavigation.setRoutes(routes)
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

        runBlocking(Dispatchers.Main) {
            val historyPath = mapboxNavigation.historyRecorder.stopRecording()
            logE("history path=$historyPath", "DEBUG")
        }

        // assert results
        expectedStates.assert()

        runOnMainSync {
            val newWaypoints =
                mapboxNavigation.getRoutes().first().routeOptions()!!.coordinatesList()
            check(newWaypoints.size == 2) {
                "Expected 2 waypoints in the route after reroute but was ${newWaypoints.size}"
            }
            check(newWaypoints[1] == mockRoute.routeWaypoints.last())
        }
    }

    @Test
    fun reroute_is_cancelled_when_the_user_returns_to_route() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short(activity)
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
            relaxedExpectedCoordinates = false
        )
        val rerouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(activity, R.raw.empty_directions_response),
            expectedCoordinates = listOf(
                Point.fromLngLat(
                    offRouteLocationUpdate.longitude,
                    offRouteLocationUpdate.latitude
                ),
                mockRoute.routeWaypoints.last()
            ),
            relaxedExpectedCoordinates = true
        )
        val responseModifier = DelayedResponseModifier(10000)
        rerouteRequestHandler.jsonResponseModifier = responseModifier
        mockWebServerRule.requestHandlers.add(requestHandler)
        mockWebServerRule.requestHandlers.add(rerouteRequestHandler)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.Interrupted)
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints)
                .build()
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
        val mockRoute = RoutesProvider.dc_short_with_alternative(activity)
        val mockReroute = RoutesProvider.dc_short_with_alternative_reroute(activity)
        val testData = prepareRouteAndRerouteWithAlternatives(
            mockRoute = mockRoute,
            mockReroute = mockReroute,
            // setting to 4s as NN router's default timeout at the time of creating the test is 5s
            rerouteResponseDelay = rerouteResponseDelay
        )

        mockLocationReplayerRule.loopUpdate(testData.originLocation, times = 120)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.RouteFetched(RouterOrigin.Offboard))
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            testData.originRouteOptions
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
            mapboxNavigation.getNavigationRoutes() + originalRoutes[1]
        )
        assertEquals(
            originalRoutes,
            mapboxNavigation.getNavigationRoutes()
        )

        // wait for reroute to complete meaning that changing alternatives didn't cancel reroute
        val rerouteUpdate = mapboxNavigation.routesUpdates().first {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
        }

        // assert
        assertEquals(
            mockReroute.routeWaypoints,
            rerouteUpdate.navigationRoutes.first().waypoints!!.map { it.location() }
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
            customRefreshInterval = refreshInterval
        )
        val mockRoute = RoutesProvider.dc_short_with_alternative(activity)
        val mockReroute = RoutesProvider.dc_short_with_alternative_reroute(activity)
        val testData = prepareRouteAndRerouteWithAlternatives(
            mockRoute = mockRoute,
            mockReroute = mockReroute,
            rerouteResponseDelay = rerouteResponseDelay
        )

        mockLocationReplayerRule.loopUpdate(testData.originLocation, times = 120)

        val refreshHandler = MockDirectionsRefreshHandler(
            testUuid = "jpCHHUC26qFOwISCNLjar2xmTfI6Dxd0qCHOoqwt_1VAlESNvsr7Zg==",
            readRawFileText(
                activity,
                R.raw.route_response_dc_short_with_alternative_refresh_route_0
            ),
            acceptedGeometryIndex = 0
        )
        mockWebServerRule.requestHandlers.add(refreshHandler)

        val rerouteStateTransitionAssertion = RerouteStateTransitionAssertion(
            mapboxNavigation.getRerouteController()!!
        ) {
            requiredState(RerouteState.Idle)
            requiredState(RerouteState.FetchingRoute)
            requiredState(RerouteState.RouteFetched(RouterOrigin.Offboard))
            requiredState(RerouteState.Idle)
        }

        val originalRoutes = mapboxNavigation.requestRoutes(
            testData.originRouteOptions
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
            routesUpdate.navigationRoutes.first().waypoints!!.map { it.location() }
        )
        rerouteStateTransitionAssertion.assert()
    }

    @Test
    fun reroute_to_alternative_route() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
        val origin = routes.first().routeOptions.coordinatesList().first()
        mockLocationUpdatesRule.pushLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.routesUpdates().first { it.navigationRoutes == routes }

        mockLocationReplayerRule.playRoute(routes[1].directionsRoute)
        val routesUpdate = mapboxNavigation.routesUpdates().first {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
        }

        assertEquals(
            listOf(routes[1]),
            routesUpdate.navigationRoutes
        )
    }

    @Test
    fun events_order_are_guaranteed_during_reroute_to_alternative_with_custom_reroute_controller() =
        sdkTest {
            val mapboxNavigation = createMapboxNavigation()
            val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
            var latestRouteProgress: RouteProgress? = null
            mapboxNavigation.registerRouteProgressObserver {
                latestRouteProgress = it
            }
            val navigationStateDuringReroute =
                CompletableDeferred<NavigationStateDuringReroute>()
            mapboxNavigation.setRerouteController(object : NavigationRerouteController {
                override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
                    navigationStateDuringReroute.complete(
                        NavigationStateDuringReroute(
                            latestRouteProgress,
                            mapboxNavigation.getNavigationRoutes()
                        )
                    )
                }
                override fun reroute(routesCallback: RerouteController.RoutesCallback) {
                }
                override val state: RerouteState = RerouteState.Idle
                override fun interrupt() {
                }
                override fun registerRerouteStateObserver(
                    rerouteStateObserver: RerouteController.RerouteStateObserver
                ): Boolean {
                    return false
                }
                override fun unregisterRerouteStateObserver(
                    rerouteStateObserver: RerouteController.RerouteStateObserver
                ): Boolean {
                    return false
                }
            })
            val origin = routes.first().routeOptions.coordinatesList().first()
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = origin.latitude()
                longitude = origin.longitude()
            }
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)
            mapboxNavigation.routesUpdates().first { it.navigationRoutes == routes }

            mockLocationReplayerRule.playRoute(routes[1].directionsRoute)

            val state = navigationStateDuringReroute.await()
            assertEquals(
                routes[1].id,
                state.latestRouteProgress?.routeAlternativeId
            )
            assertEquals(
                routes,
                state.routes
            )
        }

    @Test
    fun reroute_on_single_leg_route_without_alternatives() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short(activity)
        val originLocation = mockRoute.routeWaypoints.first()
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
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

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()
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
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short_two_legs(activity)
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
                    activity,
                    R.raw.reroute_response_dc_very_short_two_legs
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

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

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

    @Test
    fun reroute_on_single_leg_route_with_alternatives() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_with_alternative(activity)
        // on alternative
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.893403
            longitude = -77.032033
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        val rerouteResult = mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(0, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
        assertEquals(routes[1], rerouteResult.navigationRoutes.first())
    }

    @Test
    fun reroute_on_multileg_route_first_leg_with_alternatives() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_two_legs_with_alternative(activity)
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.888565
            longitude = -77.039343
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

        mapboxNavigation.routeProgressUpdates().first()
        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        val rerouteResult = mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(0, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
        assertEquals(routes[1], rerouteResult.navigationRoutes.first())
    }

    @Test
    fun reroute_on_multileg_route_second_leg_with_alternatives() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_two_legs_with_alternative_for_second_leg(activity)
        val secondLegLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895469
            longitude = -77.030394
            bearing = 90f
        }
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895192
            longitude = -77.028985
            bearing = 90f
        }
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes

        mapboxNavigation.setNavigationRoutes(routes)

        mockLocationReplayerRule.loopUpdateUntil(secondLegLocation) {
            mapboxNavigation.routeProgressUpdates().first()
            mapboxNavigation.navigateNextRouteLeg()
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentLegProgress?.legIndex == 1 && it.currentRouteGeometryIndex > 6 }
                .first()
        }
        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        val rerouteResult = mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(1, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
        assertEquals(routes[1], rerouteResult.navigationRoutes.first())
    }

    @Test
    fun reroute_from_single_leg_primary_to_multileg_alternative() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_alternative_has_more_legs(activity)
        val onRouteLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895469
            longitude = -77.030394
            bearing = 90f
        }
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895873
            longitude = -77.029556
            bearing = 0f
        }
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .waypointsPerRoute(true)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes

        mapboxNavigation.setNavigationRoutes(routes)

        mockLocationReplayerRule.loopUpdateUntil(onRouteLocation) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentLegProgress?.legIndex == 0 && it.currentRouteGeometryIndex > 5 }
                .first()
        }
        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        val rerouteResult = mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(1, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
        assertEquals(routes[1], rerouteResult.navigationRoutes.first())
    }

    @Test
    fun reroute_from_multileg_primary_to_single_leg_alternative() = sdkTest {
        val mapboxNavigation = createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_short_alternative_has_more_legs(activity)
        val secondLegLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895469
            longitude = -77.030394
            bearing = 90f
        }
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 38.895192
            longitude = -77.028985
            bearing = 90f
        }
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        mapboxNavigation.startTripSession()
        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .waypointsPerRoute(true)
                .coordinatesList(mockRoute.routeWaypoints).build()
        ).getSuccessfulResultOrThrowException().routes.reversed()

        mapboxNavigation.setNavigationRoutes(routes)

        mapboxNavigation.routeProgressUpdates().first()
        mapboxNavigation.navigateNextRouteLeg()
        mockLocationReplayerRule.loopUpdateUntil(secondLegLocation) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentLegProgress?.legIndex == 1 && it.currentRouteGeometryIndex > 6 }
                .first()
        }
        mockLocationReplayerRule.loopUpdateUntil(offRouteLocationUpdate) {
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                .first()
        }

        val rerouteResult = mapboxNavigation.routesUpdates().filter {
            (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE).also {
                if (it) {
                    assertEquals(0, mapboxNavigation.currentLegIndex())
                }
            }
        }.first()
        assertEquals(routes[1], rerouteResult.navigationRoutes.first())
    }

    private fun createMapboxNavigation(customRefreshInterval: Long? = null): MapboxNavigation {
        var mapboxNavigation: MapboxNavigation? = null

        fun create(): MapboxNavigation {
            MapboxNavigationProvider.destroy()
            val navigationOptions = NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .historyRecorderOptions(
                    HistoryRecorderOptions.Builder()
                        .build()
                )
                .routingTilesOptions(
                    RoutingTilesOptions.Builder()
                        .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                        .build()
                ).apply {
                    if (customRefreshInterval != null) {
                        val customRefreshOptions = RouteRefreshOptions.Builder()
                            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                            .build()
                        RouteRefreshOptions::class.java.getDeclaredField(
                            "intervalMillis"
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
        rerouteResponseDelay: Long? = null
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
            }
        )

        val originalRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(activity)
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

private data class NavigationStateDuringReroute(
    val latestRouteProgress: RouteProgress?,
    val routes: List<NavigationRoute>,
)
