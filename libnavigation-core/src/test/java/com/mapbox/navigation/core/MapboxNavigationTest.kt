package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.toTestNavigationRoutes
import com.mapbox.navigation.base.options.IncidentsOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.IgnoredRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.telemetry.NavigationCustomEventType
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.InternalRerouteControllerAdapter
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteResult
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routerefresh.RouteRefreshObserver
import com.mapbox.navigation.core.routerefresh.RouteRefresherResult
import com.mapbox.navigation.core.routerefresh.RouteRefresherStatus
import com.mapbox.navigation.core.routerefresh.RoutesRefresherResult
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NativeSetRouteError
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.createSetRouteResult
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.TilesConfig
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalPreviewMapboxNavigationAPI
@Config(shadows = [ShadowReachabilityFactory::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class MapboxNavigationTest : MapboxNavigationBaseTest() {

    private val invalidRouteReason = "Route is invalid for navigation"

    @Test
    fun sanity() {
        createMapboxNavigation()
        assertNotNull(mapboxNavigation)
    }

    @Test
    fun startSessionWithService() {
        createMapboxNavigation()
        every { directionsSession.initialLegIndex } returns 0
        every { tripSession.isRunningWithForegroundService() } returns true
        mapboxNavigation.startTripSession()

        assertTrue(mapboxNavigation.isRunningForegroundService())
    }

    @Test
    fun startSessionWithoutService() {
        createMapboxNavigation()
        every { directionsSession.initialLegIndex } returns 0
        every { tripSession.isRunningWithForegroundService() } returns false
        mapboxNavigation.startTripSession(false)

        assertFalse(mapboxNavigation.isRunningForegroundService())
    }

    @Test
    fun `trip session route is reset after trip session is restarted`() {
        createMapboxNavigation()
        val primary = mockk<NavigationRoute>()
        val routes = listOf(primary, mockk())
        val currentLegIndex = 3
        every { directionsSession.routes } returns routes
        every { directionsSession.initialLegIndex } returns 2
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns currentLegIndex
            }
        }
        mapboxNavigation.stopTripSession()
        mapboxNavigation.startTripSession()

        coVerify(exactly = 1) {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(currentLegIndex)
            )
        }
    }

    @Test
    fun `getZLevel returns current z level`() {
        createMapboxNavigation()
        every { tripSession.zLevel } returns 3
        assertEquals(3, mapboxNavigation.getZLevel())
    }

    @Test
    fun init_routesObs_internalRouteObs_navigationSession_and_TelemetryLocAndProgressDispatcher() {
        createMapboxNavigation()
        // + 1 for navigationSession
        // + 1 for routesCacheClearer
        verify(exactly = 3) { directionsSession.registerSetNavigationRoutesFinishedObserver(any()) }
    }

    @Test
    fun init_registersRoutesCacheClearerAsObservers() {
        createMapboxNavigation()
        verify(exactly = 1) {
            directionsSession.registerSetNavigationRoutesFinishedObserver(routesCacheClearer)
            routesPreviewController.registerRoutesPreviewObserver(routesCacheClearer)
        }
    }

    @Test
    fun init_registerOffRouteObserver() {
        createMapboxNavigation()
        verify(exactly = 1) { tripSession.registerOffRouteObserver(any()) }
    }

    @Test
    fun destroy_unregisterOffRouteObserver() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllOffRouteObservers() }
    }

    @Test
    fun destroy_unregisterAllRoutesPreviewObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { routesPreviewController.unregisterAllRoutesPreviewObservers() }
    }

    @Test
    fun init_registerOffRouteObserver_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        threadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()

        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)

        verify(exactly = 2) { tripSession.registerOffRouteObserver(any()) }
    }

    @Test
    fun destroy_unregisterAllOffRouteObservers_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        threadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)

        mapboxNavigation.onDestroy()

        verify(exactly = 2) { tripSession.unregisterAllOffRouteObservers() }
    }

    @Test
    fun registerLocationObserver() {
        createMapboxNavigation()
        val observer: LocationObserver = mockk()
        mapboxNavigation.registerLocationObserver(observer)

        verify(exactly = 1) { tripSession.registerLocationObserver(observer) }
    }

    @Test
    fun unregisterLocationObserver() {
        createMapboxNavigation()
        val observer: LocationObserver = mockk()
        mapboxNavigation.unregisterLocationObserver(observer)

        verify(exactly = 1) { tripSession.unregisterLocationObserver(observer) }
    }

    @Test
    fun init_registerStateObserver_navigationSession() {
        createMapboxNavigation()
        val arguments = mutableListOf<TripSessionStateObserver>()
        verify(exactly = 2) { tripSession.registerStateObserver(capture(arguments)) }
        assertEquals(listOf(navigationSession, historyRecordingStateHandler), arguments)
    }

    @Test
    fun onDestroy_unregisters_DirectionSession_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.unregisterAllSetNavigationRoutesFinishedObserver() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_location_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllLocationObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_routeProgress_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllRouteProgressObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_offRoute_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllOffRouteObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_state_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllStateObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_routeAlerts_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllRoadObjectsOnRouteObservers() }
    }

    @Test
    fun onDestroy_unregisters_HistoryRecordingStateHandler_observers() {
        createMapboxNavigation()

        mapboxNavigation.onDestroy()

        verify(exactly = 1) { historyRecordingStateHandler.unregisterAllStateChangeObservers() }
    }

    @Test
    fun onDestroy_unregisters_DeveloperMetadataAggregator_observers() {
        createMapboxNavigation()

        mapboxNavigation.onDestroy()

        verify(exactly = 1) { developerMetadataAggregator.unregisterAllObservers() }
    }

    @Test
    fun onDestroySetsRoutesToEmpty() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    emptyList(),
                    emptyList(),
                    SetRoutes.CleanUp
                )
            )
        }
    }

    @Test
    fun onDestroyDoesNotSetRoutesToEmptyIfEmptyIsInvalid() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")
        mapboxNavigation.onDestroy()

        verify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun onDestroyCallsTripSessionStop() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.stop() }
    }

    @Test
    fun onDestroyCallsNativeNavigatorReset() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        coVerify(exactly = 1) { navigator.resetRideSession() }
    }

    @Test
    fun unregisterAllBannerInstructionsObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllBannerInstructionsObservers() }
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllVoiceInstructionsObservers() }
    }

    @Test
    fun unregisterAllNavigationSessionStateObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { navigationSession.unregisterAllNavigationSessionStateObservers() }
    }

    @Test
    fun telemetryIsDisabled() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false

        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 0) {
            MapboxNavigationTelemetry.initialize(any(), any(), any(), any())
        }
        verify(exactly = 0) { MapboxNavigationTelemetry.destroy(any()) }
    }

    @ExperimentalPreviewMapboxNavigationAPI
    @Test(expected = IllegalStateException::class)
    fun telemetryIsDisabledTryToGetFeedbackMetadataWrapper() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false

        createMapboxNavigation()
        mapboxNavigation.provideFeedbackMetadataWrapper()
    }

    @ExperimentalPreviewMapboxNavigationAPI
    fun telemetryIsDisabledTryToPostFeedback() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false

        createMapboxNavigation()

        mapboxNavigation.postUserFeedback(mockk(), mockk(), mockk(), mockk(), mockk())
        mapboxNavigation.postUserFeedback(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())

        verify(exactly = 0) {
            MapboxNavigationTelemetry.postUserFeedback(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun unregisterAllTelemetryObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { MapboxNavigationTelemetry.destroy(eq(mapboxNavigation)) }
    }

    @Test
    fun unregisterAllTelemetryObserversIsCalledAfterTripSessionStop() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verifyOrder {
            tripSession.stop()
            MapboxNavigationTelemetry.destroy(mapboxNavigation)
        }
    }

    @Test
    fun unregisterAllArrivalObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { arrivalProgressObserver.unregisterAllObservers() }
    }

    @Test
    fun current_route_geometry_index_provider() {
        createMapboxNavigation()
        verify(exactly = 1) {
            tripSession.registerRouteProgressObserver(routeProgressDataProvider)
        }
    }

    @Test
    fun arrival_controller_register() {
        createMapboxNavigation()
        clearMocks(tripSession, answers = false)
        val arrivalController: ArrivalController = mockk()

        mapboxNavigation.setArrivalController(arrivalController)

        verify(exactly = 1) {
            tripSession.registerRouteProgressObserver(ofType(ArrivalProgressObserver::class))
        }
    }

    @Test
    fun arrival_controller_unregister() {
        createMapboxNavigation()
        val arrivalController: ArrivalController? = null

        mapboxNavigation.setArrivalController(arrivalController)

        verify { tripSession.unregisterRouteProgressObserver(any<ArrivalProgressObserver>()) }
    }

    @Test
    fun offroute_lead_to_reroute() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }

        verify(exactly = 1) {
            rerouteController.reroute(any<InternalRerouteController.RoutesCallback>())
        }
        verify(ordering = Ordering.ORDERED) {
            tripSession.registerOffRouteObserver(any())
            rerouteController.reroute(any<InternalRerouteController.RoutesCallback>())
        }
    }

    @Test
    fun non_offroute_cancels_reroute() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(false)
        }

        verify(exactly = 1) { rerouteController.interrupt() }
        verify(ordering = Ordering.ORDERED) {
            tripSession.registerOffRouteObserver(any())
            rerouteController.interrupt()
        }
    }

    @Test
    fun `new routes are set after reroute`() {
        val newInputRoutes = listOf(
            routeWithId("id#0"),
            routeWithId("id#1"),
            routeWithId("id#2")
        )
        val validAlternatives = listOf(
            alternativeWithId("id#0"),
            alternativeWithId("id#2")
        )
        val newAcceptedRoutes = listOf(newInputRoutes[0], newInputRoutes[2])
        val newIgnoredRoutes = listOf(IgnoredRoute(newInputRoutes[1], invalidRouteReason))
        val initialLegIndex = 2
        val navigationRerouteController: InternalRerouteController = mockk(relaxed = true) {
            every { reroute(any<InternalRerouteController.RoutesCallback>()) } answers {
                (firstArg() as InternalRerouteController.RoutesCallback).onNewRoutes(
                    RerouteResult(newInputRoutes, initialLegIndex, mockk(relaxed = true))
                )
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteValue(newInputRoutes, validAlternatives)

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(navigationRerouteController)
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }
        coVerify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    newAcceptedRoutes,
                    newIgnoredRoutes,
                    SetRoutes.Reroute(initialLegIndex)
                )
            )
        }
    }

    @Test
    fun `new routes are not set after reroute if they are invalid`() {
        val newRoutes = listOf(mockk<NavigationRoute>(relaxed = true), mockk(relaxed = true))
        val navigationRerouteController: InternalRerouteController = mockk(relaxed = true) {
            every { reroute(any<InternalRerouteController.RoutesCallback>()) } answers {
                (firstArg() as InternalRerouteController.RoutesCallback)
                    .onNewRoutes(RerouteResult(newRoutes, 1, mockk(relaxed = true)))
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(navigationRerouteController)
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }
        coVerify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `set reroute controller in fetching state sets routes to session`() {
        val newInputRoutes = listOf(
            routeWithId("id#0"),
            routeWithId("id#1"),
            routeWithId("id#2")
        )
        val validAlternatives = listOf(
            alternativeWithId("id#0"),
            alternativeWithId("id#2")
        )
        val newAcceptedRoutes = listOf(newInputRoutes[0], newInputRoutes[2])
        val newIgnoredRoutes = listOf(IgnoredRoute(newInputRoutes[1], invalidRouteReason))
        val oldController = mockk<RerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val initialLegIndex = 1
        val navigationRerouteController: InternalRerouteController = mockk(relaxed = true) {
            every { reroute(any<InternalRerouteController.RoutesCallback>()) } answers {
                (firstArg() as InternalRerouteController.RoutesCallback).onNewRoutes(
                    RerouteResult(newInputRoutes, initialLegIndex, mockk(relaxed = true))
                )
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteValue(newInputRoutes, validAlternatives)

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(navigationRerouteController)
        coVerify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    newAcceptedRoutes,
                    newIgnoredRoutes,
                    SetRoutes.Reroute(initialLegIndex)
                )
            )
        }
    }

    @Test
    fun `set reroute controller in fetching state does not set invalid routes to session`() {
        val newRoutes = listOf(mockk<NavigationRoute>(relaxed = true), mockk(relaxed = true))
        val oldController = mockk<RerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val navigationRerouteController: InternalRerouteController = mockk(relaxed = true) {
            every { reroute(any<InternalRerouteController.RoutesCallback>()) } answers {
                (firstArg() as InternalRerouteController.RoutesCallback)
                    .onNewRoutes(RerouteResult(newRoutes, 1, mockk(relaxed = true)))
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(navigationRerouteController)
        coVerify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `set legacy reroute controller in fetching state sets routes to session`() {
        val newRoutes = emptyList<DirectionsRoute>()
        val oldController = mockk<RerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val rerouteController: RerouteController = mockk(relaxed = true) {
            every { reroute(any()) } answers {
                (firstArg() as RerouteController.RoutesCallback).onNewRoutes(newRoutes)
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteValue(emptyList(), emptyList())

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(rerouteController)
        coVerify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                match { it.setRoutesInfo == SetRoutes.Reroute(0) },
            )
        }
    }

    @Test
    fun `set navigation reroute controller in fetching state sets routes to session`() {
        val newRoutes = emptyList<NavigationRoute>()
        val oldController = mockk<NavigationRerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val rerouteController: NavigationRerouteController = mockk(relaxed = true) {
            every { reroute(any<NavigationRerouteController.RoutesCallback>()) } answers {
                (firstArg() as NavigationRerouteController.RoutesCallback)
                    .onNewRoutes(newRoutes, mockk(relaxed = true))
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteValue(emptyList(), emptyList())

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(rerouteController)
        coVerify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                match { it.setRoutesInfo == SetRoutes.Reroute(0) },
            )
        }
    }

    @Test
    fun `set legacy reroute controller in fetching state does not set invalid routes to session`() {
        val newRoutes = emptyList<DirectionsRoute>()
        val oldController = mockk<RerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val rerouteController: RerouteController = mockk(relaxed = true) {
            every { reroute(any()) } answers {
                (firstArg() as RerouteController.RoutesCallback).onNewRoutes(newRoutes)
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(rerouteController)
        coVerify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `set navigation reroute controller in fetching state does not set invalid routes to session`() {
        val newRoutes = emptyList<NavigationRoute>()
        val oldController = mockk<NavigationRerouteController>(relaxed = true) {
            every { state } returns RerouteState.FetchingRoute
        }
        val rerouteController: NavigationRerouteController = mockk(relaxed = true) {
            every { reroute(any<NavigationRerouteController.RoutesCallback>()) } answers {
                (firstArg() as NavigationRerouteController.RoutesCallback)
                    .onNewRoutes(newRoutes, mockk(relaxed = true))
            }
        }
        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")

        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)
        mapboxNavigation.setRerouteController(rerouteController)
        coVerify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `reset reroute controller`() {
        createMapboxNavigation()
        val originalController = mapboxNavigation.getRerouteController()
        mapboxNavigation.setRerouteController(mockk(relaxed = true))

        mapboxNavigation.setRerouteController()

        assertEquals(originalController, mapboxNavigation.getRerouteController())
    }

    @Test
    fun reRoute_not_called() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(false)

        verify(exactly = 0) {
            rerouteController.reroute(any<InternalRerouteController.RoutesCallback>())
        }
    }

    @Test
    fun internalRouteObserver_notEmpty() {
        createMapboxNavigation()
        val primary: NavigationRoute = mockk {
            every { directionsRoute } returns mockk()
        }
        val secondary: NavigationRoute = mockk {
            every { directionsRoute } returns mockk()
        }
        val routes = listOf(primary, secondary)
        val ignoredRoutes = listOf<IgnoredRoute>(mockk(relaxed = true))
        val reason = RoutesExtra.ROUTES_UPDATE_REASON_NEW
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        every { tripSession.getState() } returns TripSessionState.STARTED
        verify {
            directionsSession.registerSetNavigationRoutesFinishedObserver(
                capture(routeObserversSlot)
            )
        }

        routeObserversSlot.forEach {
            it.onRoutesChanged(RoutesUpdatedResult(routes, ignoredRoutes, reason))
        }

        coVerifyOrder {
            routeProgressDataProvider.onNewRoutes()
            routeRefreshController.requestPlannedRouteRefresh(routes)
        }
    }

    @Test
    fun internalRouteObserver_empty() {
        createMapboxNavigation()
        val routes = emptyList<NavigationRoute>()
        val ignoredRoutes = listOf<IgnoredRoute>(mockk(relaxed = true))
        val reason = RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        every { tripSession.getState() } returns TripSessionState.STARTED
        verify {
            directionsSession.registerSetNavigationRoutesFinishedObserver(
                capture(routeObserversSlot)
            )
        }

        routeObserversSlot.forEach {
            it.onRoutesChanged(RoutesUpdatedResult(routes, ignoredRoutes, reason))
        }

        coVerify(exactly = 1) {
            routeProgressDataProvider.onNewRoutes()
        }
        coVerify(exactly = 1) { routeRefreshController.requestPlannedRouteRefresh(emptyList()) }
    }

    @Test
    fun `don't interrupt reroute requests on a standalone route request`() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk<NavigationRouterCallback>())

        verify(exactly = 0) { rerouteController.interrupt() }
    }

    @Test
    fun interrupt_reroute_process_when_new_reroute_controller_has_been_set() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val newRerouteController: RerouteController = mockk(relaxUnitFun = true) {
            every { state } returns RerouteState.Idle
        }
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }
        every { rerouteController.state } returns RerouteState.FetchingRoute

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }
        mapboxNavigation.setRerouteController(newRerouteController)

        verify(exactly = 1) {
            rerouteController.reroute(any<InternalRerouteController.RoutesCallback>())
        }
        verify(exactly = 1) { rerouteController.interrupt() }
        verify(exactly = 1) { newRerouteController.reroute(any()) }
        verifyOrder {
            rerouteController.reroute(any<InternalRerouteController.RoutesCallback>())
            rerouteController.interrupt()
            newRerouteController.reroute(any())
        }
    }

    @Test
    fun `road objects observer is registered in the trip session`() {
        createMapboxNavigation()
        val observer: RoadObjectsOnRouteObserver = mockk()

        mapboxNavigation.registerRoadObjectsOnRouteObserver(observer)

        verify(exactly = 1) { tripSession.registerRoadObjectsOnRouteObserver(observer) }
    }

    @Test
    fun `road objects observer is unregistered in the trip session`() {
        createMapboxNavigation()
        val observer: RoadObjectsOnRouteObserver = mockk()

        mapboxNavigation.unregisterRoadObjectsOnRouteObserver(observer)

        verify(exactly = 1) { tripSession.unregisterRoadObjectsOnRouteObserver(observer) }
    }

    @Test
    fun `resetTripSession should reset the navigator`() {
        createMapboxNavigation()
        mapboxNavigation.resetTripSession()

        coVerify { navigator.resetRideSession() }
    }

    @Test
    fun `resetTripSession should reset the navigator and call back`() {
        createMapboxNavigation()
        val callback = mockk<TripSessionResetCallback>(relaxUnitFun = true)
        mapboxNavigation.resetTripSession(callback)

        verify { callback.onTripSessionReset() }
    }

    @Test
    fun `verify tile config path`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(),
                any(),
                capture(slot),
                any(),
                any(),
            )
        } returns navigator
        val options = navigationOptions.toBuilder()
            .routingTilesOptions(RoutingTilesOptions.Builder().build())
            .build()

        mapboxNavigation = MapboxNavigation(options, threadController)

        assertTrue(slot.captured.tilesPath.endsWith(RoutingTilesFiles.TILES_PATH_SUB_DIR))
    }

    @Test
    fun `verify tile config dataset`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(),
                any(),
                capture(slot),
                any(),
                any(),
            )
        } returns navigator
        val options = navigationOptions.toBuilder()
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .tilesDataset("someUser.osm")
                    .tilesProfile("truck")
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options, threadController)

        assertEquals(slot.captured.endpointConfig!!.dataset, "someUser.osm/truck")
    }

    @Test
    fun `verify incidents options null when no params set`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every { NavigatorLoader.createConfig(any(), capture(slot)) } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        assertNull(slot.captured.incidentsOptions)
    }

    @Test
    fun `verify incidents options non-null when graph set`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every { NavigatorLoader.createConfig(any(), capture(slot)) } returns mockk()
        val options = navigationOptions.toBuilder()
            .incidentsOptions(
                IncidentsOptions.Builder()
                    .graph("graph")
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options, threadController)

        assertEquals(slot.captured.incidentsOptions!!.graph, "graph")
        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "")
    }

    @Test
    fun `verify incidents options non-null when apiUrl set`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every { NavigatorLoader.createConfig(any(), capture(slot)) } returns mockk()
        val options = navigationOptions.toBuilder()
            .incidentsOptions(
                IncidentsOptions.Builder()
                    .apiUrl("apiUrl")
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options, threadController)

        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "apiUrl")
        assertEquals(slot.captured.incidentsOptions!!.graph, "")
    }

    @Test
    fun `setRoute pushes the route to the directions session`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()

        val routes = listOf(
            routeWithId("id#0"),
            routeWithId("id#1"),
            routeWithId("id#2")
        )
        val validAlternatives = listOf(
            alternativeWithId("id#2")
        )
        val acceptedRoutes = listOf(routeWithId("id#3"), routes[2])
        val ignoredRoutes = listOf(IgnoredRoute(routes[1], invalidRouteReason))
        val initialLegIndex = 2

        coEvery {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(initialLegIndex)
            )
        } returns NativeSetRouteValue(acceptedRoutes, validAlternatives)
        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    acceptedRoutes,
                    ignoredRoutes,
                    SetRoutes.NewRoutes(initialLegIndex)
                )
            )
        }
    }

    @Test
    fun `setRoute for empty pushes empty routes to the directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()

            coEvery {
                tripSession.setRoutes(
                    emptyList(),
                    SetRoutes.CleanUp
                )
            } returns NativeSetRouteValue(emptyList(), listOf(alternativeWithId("id#0")))
            mapboxNavigation.setNavigationRoutes(emptyList())

            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        emptyList(),
                        emptyList(),
                        SetRoutes.CleanUp
                    )
                )
            }
        }

    @Test
    fun `setRoute does not push the invalid route to the directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val route: NavigationRoute = mockk(relaxed = true)
            val routeOptions = createRouteOptions()
            every { route.routeOptions } returns routeOptions
            every { route.directionsRoute.geometry() } returns "geometry"
            every { route.directionsRoute.legs() } returns emptyList()

            val routes = listOf(route)
            val initialLegIndex = 2

            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex)
                )
            } returns NativeSetRouteError("some error")
            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

            verify(exactly = 0) {
                directionsSession.setNavigationRoutesFinished(any())
            }
        }

    @Test
    fun `deprecated setRoutes pushes the route to the directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()

            val route1: DirectionsRoute = createDirectionsRoute(requestUuid = "test1")
            val route2: DirectionsRoute = createDirectionsRoute(requestUuid = "test2")
            val route3: DirectionsRoute = createDirectionsRoute(requestUuid = "test3")
            val routes = listOf(route1, route2, route3)
            val navigationRoutes = routes.toTestNavigationRoutes(RouterOrigin.Custom())
            val acceptedRoutes = listOf(route1, route3)
                .toTestNavigationRoutes(RouterOrigin.Custom())
            val initialLegIndex = 2

            coEvery {
                tripSession.setRoutes(any(), any())
            } returns NativeSetRouteValue(
                navigationRoutes,
                listOf(alternativeWithId("test3#0"))
            )
            mapboxNavigation.setRoutes(routes, initialLegIndex)

            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        acceptedRoutes,
                        listOf(
                            IgnoredRoute(
                                navigationRoutes[1], // route2
                                invalidRouteReason
                            )
                        ),
                        SetRoutes.NewRoutes(initialLegIndex)
                    )
                )
            }
        }

    @Test
    fun `deprecated setRoutes does not push the invalid route to the directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()

            val routes = emptyList<DirectionsRoute>()
            val initialLegIndex = 2

            coEvery {
                tripSession.setRoutes(any(), any())
            } returns NativeSetRouteError("some error")
            mapboxNavigation.setRoutes(routes, initialLegIndex)

            verify(exactly = 0) {
                directionsSession.setNavigationRoutesFinished(any())
            }
        }

    @Test
    fun `requestRoutes pushes the request to the directions session`() {
        createMapboxNavigation()
        val options = mockk<RouteOptions>()
        val callback = mockk<NavigationRouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns 1L

        mapboxNavigation.requestRoutes(options, callback)
        verify(exactly = 1) { directionsSession.requestRoutes(options, callback) }
    }

    @Test
    fun `requestRoutes passes back the request id`() {
        createMapboxNavigation()
        val expected = 1L
        val options = mockk<RouteOptions>()
        val callback = mockk<NavigationRouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns expected

        val actual = mapboxNavigation.requestRoutes(options, callback)

        assertEquals(expected, actual)
    }

    @Test
    fun `requestRoutes doesn't pushes the route to the directions session automatically`() {
        createMapboxNavigation()
        val routes = listOf(mockk<NavigationRoute>(relaxed = true))
        val options = mockk<RouteOptions>()
        val possibleInternalCallbackSlot = slot<NavigationRouterCallback>()
        val origin = mockk<RouterOrigin>()
        every { directionsSession.requestRoutes(options, any()) } returns 1L

        mapboxNavigation.requestRoutes(
            options,
            mockk<NavigationRouterCallback>(relaxUnitFun = true)
        )
        verify { directionsSession.requestRoutes(options, capture(possibleInternalCallbackSlot)) }
        possibleInternalCallbackSlot.captured.onRoutesReady(routes, origin)

        verify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `cancelRouteRequest pushes the data to directions session`() {
        createMapboxNavigation()
        mapboxNavigation.cancelRouteRequest(1L)

        verify(exactly = 1) { directionsSession.cancelRouteRequest(1L) }
    }

    @Test
    fun `directions session is shutdown onDestroy`() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.shutdown() }
    }

    @Test
    fun `register internalFallbackVersionsObserver`() {
        createMapboxNavigation()
        verify(exactly = 1) { tripSession.registerFallbackVersionsObserver(any()) }
    }

    @Test
    fun `unregisterAllFallbackVersionsObservers on destroy`() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllFallbackVersionsObservers() }
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on init`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(),
                any(),
                capture(slot),
                any(),
                any(),
            )
        } returns navigator
        val tilesVersion = "tilesVersion"
        val options = navigationOptions.toBuilder()
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .tilesVersion(tilesVersion)
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options, threadController)

        assertEquals(tilesVersion, slot.captured.endpointConfig?.version)
        assertFalse(slot.captured.endpointConfig?.isFallback!!)
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on fallback`() {
        threadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        every { directionsSession.routesUpdatedResult } returns createRoutesUpdatedResult(
            emptyList(),
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        )
        every { tripSession.getRouteProgress() } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)

        val tileConfigSlot = slot<TilesConfig>()
        every {
            navigator.recreate(
                any(),
                any(),
                capture(tileConfigSlot),
                any(),
                any(),
            )
        } just Runs

        val tilesVersion = "tilesVersion"
        val latestTilesVersion = "latestTilesVersion"
        fallbackObserverSlot.captured.onFallbackVersionsFound(
            listOf(tilesVersion, latestTilesVersion)
        )

        assertEquals(latestTilesVersion, tileConfigSlot.captured.endpointConfig?.version)
        assertTrue(tileConfigSlot.captured.endpointConfig?.isFallback!!)
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on return to latest tiles version`() =
        runBlocking {
            threadController.cancelAllUICoroutines()

            val fallbackObserverSlot = slot<FallbackVersionsObserver>()
            every {
                tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
            } just Runs
            val mockPrimaryNavigationRoute = mockk<NavigationRoute>()
            val mockAlternativeNavigationRoute = mockk<NavigationRoute>()
            val index = 4
            every {
                directionsSession.routes
            } returns listOf(mockPrimaryNavigationRoute, mockAlternativeNavigationRoute)
            every { tripSession.getRouteProgress()?.currentLegProgress?.legIndex } returns index

            mapboxNavigation = MapboxNavigation(navigationOptions, threadController)

            val tileConfigSlot = slot<TilesConfig>()
            every {
                navigator.recreate(
                    any(),
                    any(),
                    capture(tileConfigSlot),
                    any(),
                    any(),
                )
            } just Runs

            fallbackObserverSlot.captured.onCanReturnToLatest("")

            assertEquals("", tileConfigSlot.captured.endpointConfig?.version)
            assertFalse(tileConfigSlot.captured.endpointConfig?.isFallback!!)
            coVerify(exactly = 1) {
                navigator.setRoutes(
                    mockPrimaryNavigationRoute,
                    index,
                    listOf(mockAlternativeNavigationRoute),
                    SetRoutesReason.RESTORE_TO_ONLINE,
                )
            }
        }

    @Test
    fun `verify route and routeProgress are set after navigator recreation`() = runBlocking {
        threadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        val primaryRoute: NavigationRoute = mockk()
        val alternativeRoute: NavigationRoute = mockk()
        val routes: List<NavigationRoute> = listOf(primaryRoute, alternativeRoute)
        val routeProgress: RouteProgress = mockk()
        val legProgress: RouteLegProgress = mockk()
        val index = 137
        every { directionsSession.routes } returns routes
        every { tripSession.getRouteProgress() } returns routeProgress
        every { routeProgress.currentLegProgress } returns legProgress
        every { legProgress.legIndex } returns index
        coEvery { navigator.setRoutes(any(), any(), any(), any()) } answers {
            createSetRouteResult()
        }

        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)

        fallbackObserverSlot.captured.onFallbackVersionsFound(listOf("version"))

        coVerify(exactly = 1) {
            navigator.setRoutes(
                primaryRoute,
                index,
                listOf(alternativeRoute),
                SetRoutesReason.FALLBACK_TO_OFFLINE,
            )
        }
    }

    @Test
    fun `verify that session state callbacks are always delivered to NavigationSession`() =
        runBlocking {
            mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
            every { directionsSession.initialLegIndex } returns 0
            mapboxNavigation.startTripSession()
            mapboxNavigation.onDestroy()

            verifyOrder {
                tripSession.registerStateObserver(navigationSession)
                tripSession.start(
                    withTripService = true,
                    withReplayEnabled = false
                )
                tripSession.stop()
                tripSession.unregisterAllStateObservers()
            }
        }

    @Test(expected = IllegalStateException::class)
    fun `verify that only one instance of MapboxNavigation can be alive`() = runBlocking {
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
    }

    @Test
    fun `verify that MapboxNavigation instance can be recreated`() = runBlocking {
        val firstInstance = MapboxNavigation(navigationOptions)
        firstInstance.onDestroy()
        val secondInstance = MapboxNavigation(navigationOptions)

        assertNotNull(secondInstance)
        assertTrue(firstInstance.isDestroyed)

        secondInstance.onDestroy()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that the old instance is not accessible when a new one is created`() = runBlocking {
        val firstInstance = MapboxNavigation(navigationOptions)
        firstInstance.onDestroy()
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
        firstInstance.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that startTripSession is not called when destroyed`() = runBlocking {
        val localNavigationSession = NavigationSession()
        every { NavigationComponentProvider.createNavigationSession() } answers {
            localNavigationSession
        }

        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
        mapboxNavigation.onDestroy()
        mapboxNavigation.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that stopTripSession is not called when destroyed`() = runBlocking {
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
        mapboxNavigation.onDestroy()
        mapboxNavigation.stopTripSession()
    }

    @Test
    fun `verify that empty routes are not passed to the billing controller`() {
        createMapboxNavigation()
        mapboxNavigation.setNavigationRoutes(emptyList())

        verify(exactly = 0) { billingController.onExternalRouteSet(any(), any()) }
    }

    @Test
    fun `external route is first provided to the billing controller before directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(mockk<NavigationRoute>(relaxed = true))

            mapboxNavigation.setNavigationRoutes(routes)

            verifyOrder {
                billingController.onExternalRouteSet(routes.first(), 0)
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        routes,
                        emptyList(),
                        SetRoutes.NewRoutes(0)
                    )
                )
            }
        }

    @Test
    fun `external route is set with correct initial leg index`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(mockk<NavigationRoute>(relaxed = true))

            mapboxNavigation.setNavigationRoutes(routes, 2)

            verify {
                billingController.onExternalRouteSet(routes.first(), 2)
            }
        }

    @Test
    fun `adding or removing alternative routes creates alternative reason`() {
        createMapboxNavigation()
        val primaryRoute = createNavigationRoute()
        val alternativeRoute = createNavigationRoute()

        every { directionsSession.routes } returns listOf(primaryRoute)

        mapboxNavigation.setNavigationRoutes(listOf(primaryRoute, alternativeRoute))
        mapboxNavigation.setNavigationRoutes(listOf(primaryRoute))

        verifyOrder {
            directionsSession.setNavigationRoutesFinished(
                match { it.setRoutesInfo is SetRoutes.Alternatives }
            )
            directionsSession.setNavigationRoutesFinished(
                match { it.setRoutesInfo is SetRoutes.Alternatives }
            )
        }
    }

    @Test
    fun `verify that billing controller is notified of instance destruction`() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        verify(exactly = 1) {
            billingController.onDestroy()
        }
    }

    @Test
    fun `provider - check if the instance was destroyed outside of the providers scope`() {
        val instance = MapboxNavigationProvider.create(navigationOptions)

        instance.onDestroy()

        assertFalse(MapboxNavigationProvider.isCreated())
    }

    @Test
    fun `set routes are processed in the correct order`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()

        val longRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
        val shortRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
        coEvery { tripSession.setRoutes(longRoutes, any()) } coAnswers {
            delay(100L)
            NativeSetRouteValue(longRoutes, emptyList())
        }
        coEvery { tripSession.setRoutes(shortRoutes, any()) } coAnswers {
            delay(50L)
            NativeSetRouteValue(shortRoutes, emptyList())
        }

        pauseDispatcher {
            mapboxNavigation.setNavigationRoutes(longRoutes)
            mapboxNavigation.setNavigationRoutes(shortRoutes)
        }

        verifyOrder {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    longRoutes,
                    emptyList(),
                    SetRoutes.NewRoutes(0)
                )
            )
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    shortRoutes,
                    emptyList(),
                    SetRoutes.NewRoutes(0)
                )
            )
        }
    }

    @Test
    fun `set route - new routes immediately interrupts reroute`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)

        val shortRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
        coEvery { tripSession.setRoutes(shortRoutes, any()) } coAnswers {
            delay(50L)
            NativeSetRouteValue(shortRoutes, emptyList())
        }

        pauseDispatcher {
            mapboxNavigation.setNavigationRoutes(shortRoutes)
            verify(exactly = 1) { rerouteController.interrupt() }
        }
    }

    @Test
    fun `set route - clean up immediately interrupts reroute`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            mapboxNavigation.setRerouteController(rerouteController)
            val initialRoutes = mutableListOf<NavigationRoute>(mockk(relaxed = true))
            every { directionsSession.routes } returns initialRoutes
            val updatedRoutes = emptyList<NavigationRoute>()

            pauseDispatcher {
                mapboxNavigation.setNavigationRoutes(updatedRoutes)
                verify(exactly = 1) { rerouteController.interrupt() }
            }
        }

    @Test
    fun `set route - reroute immediately interrupts reroute`() =
        coroutineRule.runBlockingTest {
            val controller: InternalRerouteController = mockk(relaxUnitFun = true)
            val rerouteCallbackSlot = slot<InternalRerouteController.RoutesCallback>()
            every { controller.reroute(capture(rerouteCallbackSlot)) } just Runs
            val observers = mutableListOf<OffRouteObserver>()
            every { tripSession.registerOffRouteObserver(capture(observers)) } just Runs
            val initialRoutes = mutableListOf<NavigationRoute>(mockk(relaxed = true))
            every { directionsSession.routes } returns initialRoutes
            val newRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
            coEvery {
                tripSession.setRoutes(newRoutes, any())
            } returns NativeSetRouteValue(newRoutes, emptyList())
            createMapboxNavigation()
            mapboxNavigation.setRerouteController(controller)

            observers.forEach {
                it.onOffRouteStateChanged(true)
            }
            pauseDispatcher {
                rerouteCallbackSlot.captured.onNewRoutes(
                    RerouteResult(newRoutes, 0, RouterOrigin.Offboard)
                )
                verify(exactly = 1) { controller.interrupt() }
            }
        }

    @Test
    fun `set route - alternatives update does not interrupt reroute`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            mapboxNavigation.setRerouteController(rerouteController)
            val initialRoutes = mutableListOf<NavigationRoute>(mockk(relaxed = true))
            every { directionsSession.routes } returns initialRoutes
            val alternativeRoute = mockk<NavigationRoute> {
                every { id } returns "altId1"
            }
            val nativeAlternativeRoute = mockk<RouteAlternative> {
                every { route.routeId } returns alternativeRoute.id
            }
            val updatedRoutes = initialRoutes + alternativeRoute
            coEvery { tripSession.setRoutes(updatedRoutes, any()) } coAnswers {
                delay(50L)
                NativeSetRouteValue(updatedRoutes, listOf(nativeAlternativeRoute))
            }

            mapboxNavigation.setNavigationRoutes(updatedRoutes)

            verify(exactly = 0) { rerouteController.interrupt() }
        }

    @Test
    fun `set route - refresh update does not interrupt reroute`() =
        coroutineRule.runBlockingTest {
            val routeObserversSlot = mutableListOf<RoutesObserver>()
            every {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot)
                )
            } just Runs
            createMapboxNavigation()
            mapboxNavigation.setRerouteController(rerouteController)
            val initialRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
            val primaryRoute = mockk<NavigationRoute>(relaxed = true)
            val alternativeRoute = mockk<NavigationRoute>(relaxed = true)
            val primaryRouteProgressData = RouteProgressData(5, 12, 43)
            val alternativeRouteProgressData = RouteProgressData(1, 2, 3)
            val routesRefreshData = RoutesRefresherResult(
                RouteRefresherResult(
                    primaryRoute,
                    primaryRouteProgressData,
                    RouteRefresherStatus.SUCCESS
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute,
                        alternativeRouteProgressData,
                        RouteRefresherStatus.SUCCESS
                    )
                )
            )

            routeObserversSlot.forEach {
                it.onRoutesChanged(
                    createRoutesUpdatedResult(
                        initialRoutes,
                        RoutesExtra.ROUTES_UPDATE_REASON_NEW,
                    )
                )
            }
            interceptRefreshObserver().onRoutesRefreshed(routesRefreshData)

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(primaryRoute, alternativeRoute),
                    SetRoutes.RefreshRoutes(primaryRouteProgressData)
                )
            }
            verify(exactly = 0) { rerouteController.interrupt() }
        }

    @Test
    fun `set route - correct order of actions, result applied to alternatives controller`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf<NavigationRoute>(mockk(relaxed = true))
            val processedRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
            val nativeAlternatives = listOf<RouteAlternative>(mockk())
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(0)
                )
            } returns NativeSetRouteValue(processedRoutes, nativeAlternatives)

            mapboxNavigation.setNavigationRoutes(routes)

            coVerifyOrder {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(0)
                )
                routeAlternativesController.processAlternativesMetadata(
                    processedRoutes,
                    nativeAlternatives
                )
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        processedRoutes,
                        emptyList(),
                        SetRoutes.NewRoutes(0)
                    )
                )
            }
        }

    @Test
    fun `route refresh - empty native alternatives returned doesn't clear alternatives metadata`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val primary: NavigationRoute = mockk(relaxed = true) {
                every { directionsRoute } returns mockk(relaxed = true)
            }
            val routes = listOf(primary)
            val ignoredRoutes = listOf<IgnoredRoute>(mockk(relaxed = true))
            val routeProgressData = RouteProgressData(5, 12, 43)
            val routeObserversSlot = mutableListOf<RoutesObserver>()
            every { tripSession.getState() } returns TripSessionState.STARTED
            coEvery {
                routeProgressDataProvider.getRouteRefreshRequestDataOrWait()
            } returns RoutesProgressData(routeProgressData, emptyMap())

            val refreshedRoutes = listOf(mockk<NavigationRoute>(relaxed = true))
            coEvery {
                tripSession.setRoutes(
                    refreshedRoutes,
                    SetRoutes.RefreshRoutes(routeProgressData)
                )
            } returns NativeSetRouteError("some error")

            verify {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot)
                )
            }
            routeObserversSlot.forEach {
                it.onRoutesChanged(
                    RoutesUpdatedResult(
                        routes,
                        ignoredRoutes,
                        RoutesExtra.ROUTES_UPDATE_REASON_NEW
                    )
                )
            }

            verify(exactly = 0) {
                routeAlternativesController.processAlternativesMetadata(any(), any())
            }
        }

    @Test
    fun `correct order of actions when trip session started before routes are processed`() =
        coroutineRule.runBlockingTest {
            every { directionsSession.initialLegIndex } returns 0
            every { tripSession.isRunningWithForegroundService() } returns true
            createMapboxNavigation()
            val inputRoutes = listOf(
                routeWithId("id#0"),
                routeWithId("id#1"),
                routeWithId("id#2")
            )
            val validAlternatives = listOf(
                alternativeWithId("id#0"),
                alternativeWithId("id#2")
            )
            val acceptedRoutes = listOf(inputRoutes[0], inputRoutes[2])
            val ignoredRoutes = listOf(IgnoredRoute(inputRoutes[1], invalidRouteReason))
            coEvery {
                tripSession.setRoutes(
                    inputRoutes,
                    SetRoutes.NewRoutes(0)
                )
            } coAnswers {
                delay(100)
                NativeSetRouteValue(inputRoutes, validAlternatives)
            }
            every { directionsSession.setNavigationRoutesFinished(any()) } answers {
                every {
                    directionsSession.routes
                } returns (firstArg() as DirectionsSessionRoutes).acceptedRoutes
            }

            pauseDispatcher {
                mapboxNavigation.setNavigationRoutes(inputRoutes)
                runCurrent()
                advanceTimeBy(50) // let trip session start processing
                mapboxNavigation.startTripSession() // start session before routes processed
            }
            val setRoutesInfo = SetRoutes.NewRoutes(0)

            coVerifyOrder {
                tripSession.setRoutes(inputRoutes, setRoutesInfo)
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(acceptedRoutes, ignoredRoutes, setRoutesInfo)
                )
                tripSession.setRoutes(acceptedRoutes, setRoutesInfo)
            }

            val routesSlot = mutableListOf<DirectionsSessionRoutes>()
            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(capture(routesSlot))
            }
            assertEquals(1, routesSlot.size)
            assertEquals(acceptedRoutes, routesSlot.first().acceptedRoutes)
        }

    @Test
    fun `stopping trip session does not clear the route`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        every { directionsSession.initialLegIndex } returns 0
        every { tripSession.isRunningWithForegroundService() } returns true
        val routes = listOf<NavigationRoute>(mockk(relaxed = true))
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.stopTripSession()

        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(match { it.acceptedRoutes == routes })
        }
        verify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(match { it.acceptedRoutes.isEmpty() })
        }
    }

    @Test
    fun `setNavigationRoutes alternative for current primary route`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id1"
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id2"
        }
        val route3 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id3"
        }
        every { directionsSession.routes } returns listOf(route1, route2)

        mapboxNavigation.setNavigationRoutes(listOf(route1, route3))

        coVerify(exactly = 1) {
            tripSession.setRoutes(any(), ofType(SetRoutes.Alternatives::class))
        }
        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `setNavigationRoutes alternative for changed primary route`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id1"
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id2"
        }
        val route3 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id3"
        }
        every { directionsSession.routes } returns listOf(route1, route2, route3)

        mapboxNavigation.setNavigationRoutes(listOf(route2, route1, route3))

        coVerify(exactly = 1) {
            tripSession.setRoutes(any(), eq(SetRoutes.Reorder(0)))
        }
        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `setNavigationRoutes new routes`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id1"
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id2"
        }
        val route3 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id3"
        }
        every { directionsSession.routes } returns listOf(route1, route2)

        mapboxNavigation.setNavigationRoutes(listOf(route3, route2))

        coVerify(exactly = 1) {
            tripSession.setRoutes(any(), ofType(SetRoutes.NewRoutes::class))
        }
        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `setNavigationRoutes alternative for outdated primary route`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id1"
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id2"
        }
        val route3 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id3"
        }
        every { directionsSession.routes } returnsMany listOf(listOf(route1), listOf(route2))

        mapboxNavigation.setNavigationRoutes(listOf(route1, route3))

        coVerify(exactly = 0) {
            tripSession.setRoutes(any(), any())
        }
        verify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `refreshed route is set to trip session and directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val primaryRoute = routeWithId("id#0")
            val routes = listOf(primaryRoute)
            val reason = RoutesExtra.ROUTES_UPDATE_REASON_NEW
            val routeObserversSlot = mutableListOf<RoutesObserver>()
            every { tripSession.getState() } returns TripSessionState.STARTED
            verify {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot)
                )
            }

            val alternativeRoute1 = routeWithId("id#1")
            val alternativeRoute2 = routeWithId("id#2")
            val primaryRouteProgressData = RouteProgressData(5, 12, 43)
            val alternativeRoute1ProgressData = RouteProgressData(1, 2, 3)
            val alternativeRoute2ProgressData = RouteProgressData(4, 5, 6)
            val refreshedRoutes = listOf(primaryRoute, alternativeRoute1, alternativeRoute2)
            val acceptedRefreshRoutes = listOf(primaryRoute, alternativeRoute2)
            val ignoredRefreshRoutes = listOf(IgnoredRoute(alternativeRoute1, invalidRouteReason))
            coEvery {
                tripSession.setRoutes(refreshedRoutes, ofType(SetRoutes.RefreshRoutes::class))
            } returns NativeSetRouteValue(refreshedRoutes, listOf(alternativeWithId("id#2")))
            routeObserversSlot.forEach {
                it.onRoutesChanged(RoutesUpdatedResult(routes, emptyList(), reason))
            }
            val routesRefreshData = RoutesRefresherResult(
                RouteRefresherResult(
                    primaryRoute,
                    primaryRouteProgressData,
                    RouteRefresherStatus.SUCCESS
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        alternativeRoute1ProgressData,
                        RouteRefresherStatus.SUCCESS
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        alternativeRoute2ProgressData,
                        RouteRefresherStatus.SUCCESS
                    )
                )
            )
            interceptRefreshObserver().onRoutesRefreshed(routesRefreshData)

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(primaryRoute, alternativeRoute1, alternativeRoute2),
                    SetRoutes.RefreshRoutes(primaryRouteProgressData)
                )
            }
            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        acceptedRefreshRoutes,
                        ignoredRefreshRoutes,
                        SetRoutes.RefreshRoutes(primaryRouteProgressData)
                    )
                )
            }
        }

    @Test
    fun `refreshed route is not set to directions session if it is invalid`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val primary: NavigationRoute = mockk(relaxed = true) {
                every { directionsRoute } returns mockk()
            }
            val routes = listOf(primary)
            val ignoredRoutes = listOf<IgnoredRoute>(mockk(relaxed = true))
            val reason = RoutesExtra.ROUTES_UPDATE_REASON_NEW
            val routeObserversSlot = mutableListOf<RoutesObserver>()
            every { tripSession.getState() } returns TripSessionState.STARTED
            verify {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot)
                )
            }

            val primaryRoute = routeWithId("id#0")
            val alternativeRoute = routeWithId("id#1")
            val primaryRouteProgressData = RouteProgressData(5, 12, 43)
            val alternativeRouteProgressData = RouteProgressData(1, 2, 3)
            val routesRefreshData = RoutesRefresherResult(
                RouteRefresherResult(
                    primaryRoute,
                    primaryRouteProgressData,
                    RouteRefresherStatus.SUCCESS
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute,
                        alternativeRouteProgressData,
                        RouteRefresherStatus.SUCCESS
                    )
                )
            )

            coEvery {
                tripSession.setRoutes(any(), any())
            } returns NativeSetRouteError("some error")
            routeObserversSlot.forEach {
                it.onRoutesChanged(RoutesUpdatedResult(routes, ignoredRoutes, reason))
            }
            interceptRefreshObserver().onRoutesRefreshed(routesRefreshData)

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(primaryRoute, alternativeRoute),
                    SetRoutes.RefreshRoutes(primaryRouteProgressData)
                )
            }
            verify(exactly = 0) {
                directionsSession.setNavigationRoutesFinished(any())
            }
        }

    @Test
    fun `set reroute controller`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val oldRerouteController = mapboxNavigation.getRerouteController()
        mapboxNavigation.setRerouteController(rerouteController)
        assertFalse(mapboxNavigation.getRerouteController() === oldRerouteController)
    }

    @Test
    fun `set null reroute controller`() = coroutineRule.runBlockingTest {
        val oldController: RerouteController = mockk(relaxed = true)
        val newController: RerouteController? = null
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)

        mapboxNavigation.setRerouteController(newController)

        assertNull(mapboxNavigation.getRerouteController())
    }

    @Test
    fun `set navigation reroute controller`() = coroutineRule.runBlockingTest {
        val navigationRerouteController: NavigationRerouteController = mockk(relaxed = true)
        createMapboxNavigation()
        val oldController = mapboxNavigation.getRerouteController()
        mapboxNavigation.setRerouteController(navigationRerouteController)
        assertNotEquals(oldController, mapboxNavigation.getRerouteController())
    }

    @Test
    fun `set null navigation reroute controller`() = coroutineRule.runBlockingTest {
        val oldController: NavigationRerouteController = mockk(relaxed = true)
        val newController: NavigationRerouteController? = null
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(oldController)

        mapboxNavigation.setRerouteController(newController)

        assertNull(mapboxNavigation.getRerouteController())
    }

    @Test
    fun `set legacy reroute controller`() = coroutineRule.runBlockingTest {
        val legacyRerouteController: RerouteController = mockk(relaxed = true)
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(legacyRerouteController)

        val actual = mapboxNavigation.getRerouteController()
        assertTrue(actual is InternalRerouteControllerAdapter)
        assertEquals(actual, mapboxNavigation.getRerouteController())
    }

    @Test
    fun `when telemetry is enabled custom event is posted`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true
        every { MapboxNavigationTelemetry.postCustomEvent(any(), any(), any()) } just Runs
        every { MapboxNavigationTelemetry.destroy(any()) } just Runs

        mapboxNavigation.postCustomEvent("", NavigationCustomEventType.ANALYTICS, "1.0")

        verify(exactly = 1) { MapboxNavigationTelemetry.postCustomEvent(any(), any(), any()) }
    }

    @Test
    fun `when telemetry is disabled custom event is not posted`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false
        every { MapboxNavigationTelemetry.postCustomEvent(any(), any(), any()) } just Runs
        every { MapboxNavigationTelemetry.destroy(any()) } just Runs

        mapboxNavigation.postCustomEvent("", NavigationCustomEventType.ANALYTICS, "1.0")

        verify(exactly = 0) { MapboxNavigationTelemetry.postCustomEvent(any(), any(), any()) }
    }

    @Test
    fun requestRoadGraphDataUpdate() {
        val callback = mockk<RoadGraphDataUpdateCallback>()
        createMapboxNavigation()
        mapboxNavigation.requestRoadGraphDataUpdate(callback)
        verify { CacheHandleWrapper.requestRoadGraphDataUpdate(cache, callback) }
    }

    @Test
    fun registerHistoryRecordingStateChangeObserver() {
        val observer = mockk<HistoryRecordingStateChangeObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.registerHistoryRecordingStateChangeObserver(observer)

        verify {
            historyRecordingStateHandler.registerStateChangeObserver(observer)
        }
    }

    @Test
    fun unregisterHistoryRecordingStateChangeObserver() {
        val observer = mockk<HistoryRecordingStateChangeObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(observer)

        verify {
            historyRecordingStateHandler.unregisterStateChangeObserver(observer)
        }
    }

    @Test
    fun registerDeveloperMetadataObserver() {
        val observer = mockk<DeveloperMetadataObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.registerDeveloperMetadataObserver(observer)

        verify {
            developerMetadataAggregator.registerObserver(observer)
        }
    }

    @Test
    fun unregisterDeveloperMetadataObserver() {
        val observer = mockk<DeveloperMetadataObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.unregisterDeveloperMetadataObserver(observer)

        verify {
            developerMetadataAggregator.unregisterObserver(observer)
        }
    }

    @Test
    fun routeRefreshController() {
        createMapboxNavigation()

        assertEquals(routeRefreshController, mapboxNavigation.routeRefreshController)
    }

    @Test
    fun onDestroyDestroysRouteRefreshController() {
        createMapboxNavigation()

        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            routeRefreshController.destroy()
        }
    }

    @Test
    fun onEVDataChanged() {
        val data = mapOf("aaa" to "bbb")
        createMapboxNavigation()

        mapboxNavigation.onEVDataUpdated(data)

        verify(exactly = 1) {
            evDynamicDataHolder.updateData(data)
        }
    }

    @Test
    fun sendsAccumulatedDataToRouteAlternativesControllerWhenEvDataUpdated() {
        val newData = mapOf("option-1" to "option-1")
        val oldData = mapOf("option-2" to "option-2")

        createMapboxNavigation()
        every { evDynamicDataHolder.currentData(any()) } returns oldData + newData

        mapboxNavigation.onEVDataUpdated(newData)

        verify(exactly = 1) {
            routeAlternativesController.onEVDataUpdated(oldData + newData)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun moveRoutesFromPreviewToNavigatorNoPreviewedRoutes() {
        createMapboxNavigation()
        every { routesPreviewController.getRoutesPreview() } returns null

        mapboxNavigation.moveRoutesFromPreviewToNavigator()
    }

    @Test
    fun moveRoutesFromPreviewToNavigatorHasPreviewedRoutes() = coroutineRule.runBlockingTest {
        val routes = listOf<NavigationRoute>(mockk(relaxed = true))
        createMapboxNavigation()
        every {
            routesPreviewController.getRoutesPreview()
        } returns RoutesPreview(routes, emptyList(), listOf(mockk()), 0)

        mapboxNavigation.moveRoutesFromPreviewToNavigator()

        coVerifyOrder {
            tripSession.setRoutes(routes, any())
            routesPreviewController.previewNavigationRoutes(emptyList())
        }
    }

    @Test
    fun setRerouteEnabled() {
        val customRerouteController = mockk<NavigationRerouteController>(relaxed = true)
        val defaultRerouteController = mockk<InternalRerouteController>(relaxed = true)
        every {
            NavigationComponentProvider.createRerouteController(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns defaultRerouteController
        every { defaultRerouteController.state } returns RerouteState.FetchingRoute
        every { customRerouteController.state } returns RerouteState.FetchingRoute
        createMapboxNavigation()

        assertNotNull(mapboxNavigation.getRerouteController())
        assertTrue(mapboxNavigation.isRerouteEnabled())

        mapboxNavigation.setRerouteEnabled(false)

        assertNull(mapboxNavigation.getRerouteController())
        assertFalse(mapboxNavigation.isRerouteEnabled())
        verify { defaultRerouteController.interrupt() }
        clearAllMocks(answers = false)

        mapboxNavigation.setRerouteEnabled(true)

        assertEquals(defaultRerouteController, mapboxNavigation.getRerouteController())
        assertTrue(mapboxNavigation.isRerouteEnabled())

        mapboxNavigation.setRerouteController(customRerouteController)
        verify {
            defaultRerouteController.interrupt()
            customRerouteController.reroute(any<NavigationRerouteController.RoutesCallback>())
        }
        clearAllMocks(answers = false)
        val customRerouteControllerAdapter = mapboxNavigation.getRerouteController()
        assertNotEquals(defaultRerouteController, customRerouteControllerAdapter)
        assertTrue(mapboxNavigation.isRerouteEnabled())

        mapboxNavigation.setRerouteEnabled(true)

        verify(exactly = 0) { customRerouteController.interrupt() }
        assertEquals(customRerouteControllerAdapter, mapboxNavigation.getRerouteController())
        assertTrue(mapboxNavigation.isRerouteEnabled())

        mapboxNavigation.setRerouteController(null)

        verify { customRerouteController.interrupt() }
        assertNull(mapboxNavigation.getRerouteController())
        assertFalse(mapboxNavigation.isRerouteEnabled())
    }

    @Test
    fun currentLegIndexNoRouteProgress() {
        every { tripSession.getRouteProgress() } returns null
        every { directionsSession.initialLegIndex } returns 4
        createMapboxNavigation()

        assertEquals(4, mapboxNavigation.currentLegIndex())
    }

    @Test
    fun currentLegIndexHasRouteProgress() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 5
            }
        }
        every { directionsSession.initialLegIndex } returns 4
        createMapboxNavigation()

        assertEquals(5, mapboxNavigation.currentLegIndex())
    }

    @Test
    fun etcGateApi() {
        val fallbackObserver = slot<FallbackVersionsObserver>()
        val experimental1 = mockk<com.mapbox.navigator.Experimental>(relaxed = true)
        every { navigator.experimental } returns experimental1
        every { tripSession.registerFallbackVersionsObserver(capture(fallbackObserver)) } just Runs

        createMapboxNavigation()

        mapboxNavigation.etcGateAPI.updateEtcGateInfo(EtcGateInfo(10, 10))

        verify(exactly = 1) { experimental1.updateETCGateInfo(any()) }
        clearAllMocks(answers = false)

        val experimental2 = mockk<com.mapbox.navigator.Experimental>(relaxed = true)
        every { navigator.experimental } returns experimental2
        // recreate navigator
        fallbackObserver.captured.onFallbackVersionsFound(listOf("1.2.3"))

        mapboxNavigation.etcGateAPI.updateEtcGateInfo(EtcGateInfo(10, 10))

        verify(exactly = 0) { experimental1.updateETCGateInfo(any()) }
        verify(exactly = 1) { experimental2.updateETCGateInfo(any()) }
    }

    @Test
    fun registerRoutesInvalidatedObserver() {
        val routesInvalidatedObserver = mockk<RoutesInvalidatedObserver>()
        createMapboxNavigation()

        mapboxNavigation.registerRoutesInvalidatedObserver(routesInvalidatedObserver)

        verify(exactly = 1) {
            routeRefreshController.registerRoutesInvalidatedObserver(routesInvalidatedObserver)
        }
    }

    @Test
    fun unregisterRoutesInvalidatedObserver() {
        val routesInvalidatedObserver = mockk<RoutesInvalidatedObserver>()
        createMapboxNavigation()

        mapboxNavigation.unregisterRoutesInvalidatedObserver(routesInvalidatedObserver)

        verify(exactly = 1) {
            routeRefreshController.unregisterRoutesInvalidatedObserver(routesInvalidatedObserver)
        }
    }

    private fun interceptRefreshObserver(): RouteRefreshObserver {
        val observers = mutableListOf<RouteRefreshObserver>()
        verify { routeRefreshController.registerRouteRefreshObserver(capture(observers)) }
        return observers.last()
    }

    private fun alternativeWithId(mockId: String): RouteAlternative {
        val mockedRoute = mockk<RouteInterface> {
            every { routeId } returns mockId
        }
        return mockk(relaxed = true) {
            every { route } returns mockedRoute
        }
    }

    private fun routeWithId(mockId: String): NavigationRoute =
        mockk(relaxed = true) {
            every { id } returns mockId
        }
}
