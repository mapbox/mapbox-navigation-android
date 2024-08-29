package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.IncidentsOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.adas.AdasisConfig
import com.mapbox.navigation.core.adas.AdasisDataSendingConfig
import com.mapbox.navigation.core.adas.AdasisMessageBinaryFormat
import com.mapbox.navigation.core.adas.AdasisV2MessageObserver
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.datainputs.CompassData
import com.mapbox.navigation.core.datainputs.EtcGateInfo
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.IgnoredRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.internal.extensions.HistoryRecordingEnabledObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.internal.telemetry.NavigationCustomEventType
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteResult
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routerefresh.RouteRefreshObserver
import com.mapbox.navigation.core.routerefresh.RouteRefresherResult
import com.mapbox.navigation.core.routerefresh.RouteRefresherStatus
import com.mapbox.navigation.core.routerefresh.RoutesRefresherResult
import com.mapbox.navigation.core.sensor.SensorData
import com.mapbox.navigation.core.sensor.UpdateExternalSensorDataCallback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NativeSetRouteError
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.createSetRouteResult
import com.mapbox.navigation.core.utils.SystemLocaleWatcher
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigator.ADASISv2MessageCallback
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.TilesConfig
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.called
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.resume

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
    fun doNotSetAccessToken() {
        createMapboxNavigation()

        verify(exactly = 0) { MapboxOptions.accessToken = any() }
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
    fun `trip session route is reset after trip session is started`() {
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
        every { tripSession.getState() } returns TripSessionState.STOPPED
        mapboxNavigation.startTripSession()

        coVerify(exactly = 1) {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(currentLegIndex),
            )
        }
    }

    @Test
    fun `trip session route is not reset after trip session is started twice`() {
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
        clearAllMocks(answers = false)
        every { tripSession.getState() } returns TripSessionState.STARTED
        mapboxNavigation.startTripSession()

        coVerify(exactly = 0) {
            tripSession.setRoutes(any(), any())
        }
    }

    @Test
    fun `getZLevel returns current z level`() {
        createMapboxNavigation()
        every { tripSession.zLevel } returns 3
        assertEquals(3, mapboxNavigation.getZLevel())
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
    fun init_registerRoutesObservers() {
        createMapboxNavigation()

        val observers = mutableListOf<RoutesObserver>()
        coVerify(exactly = 4) {
            directionsSession.registerSetNavigationRoutesFinishedObserver(capture(observers))
        }

        observers[1].onRoutesChanged(mockk())
        verify { routeProgressDataProvider.onNewRoutes() }

        val testChange: RoutesUpdatedResult = mockk()
        observers[2].onRoutesChanged(testChange)
        verify { routeRefreshController.onRoutesChanged(testChange) }
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
    fun destroy_destroysTripSessionLocationEngine() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSessionLocationEngine.destroy() }
    }

    @Test
    fun destroy_shutdownNativeNavigator() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            navigator.shutdown()
        }
    }

    @Test
    fun init_registerOffRouteObserver_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        threadController.cancelAllUICoroutines()

        createMapboxNavigation()

        verify(exactly = 2) { tripSession.registerOffRouteObserver(any()) }
    }

    @Test
    fun destroy_unregisterAllOffRouteObservers_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        threadController.cancelAllUICoroutines()
        createMapboxNavigation()

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
                    SetRoutes.CleanUp,
                ),
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
    fun initializeTelemetryOnSdkInitialisation() {
        val sdkInformation = mockk<SdkInformation>()
        every { SdkInfoProvider.sdkInformation() } returns sdkInformation

        createMapboxNavigation()

        verify(exactly = 1) {
            telemetryWrapper.initialize(
                mapboxNavigation,
                navigationOptions,
                eq(sdkInformation),
            )
        }
    }

    @Test
    fun telemetryIsEnabledTryToGetFeedbackMetadataWrapper() {
        val feedbackMetadataWrapper = mockk<FeedbackMetadataWrapper>(relaxed = true)
        every { telemetryWrapper.provideFeedbackMetadataWrapper() } returns feedbackMetadataWrapper

        createMapboxNavigation()
        assertSame(feedbackMetadataWrapper, mapboxNavigation.provideFeedbackMetadataWrapper())
    }

    @ExperimentalPreviewMapboxNavigationAPI
    @Test(expected = IllegalStateException::class)
    fun telemetryIsDisabledTryToGetFeedbackMetadataWrapper() {
        every { telemetryWrapper.provideFeedbackMetadataWrapper() } returns null

        createMapboxNavigation()
        mapboxNavigation.provideFeedbackMetadataWrapper()
    }

    @ExperimentalPreviewMapboxNavigationAPI
    @Test
    fun forwardPostUserFeedbackCallToTelemetry() {
        createMapboxNavigation()

        val feedbackSubType = emptyArray<String>()
        val feedbackMetadata = mockk<FeedbackMetadata>()
        val feedbackType = "test-type"
        val description = "test-description"
        val feedbackEvent = FeedbackEvent.REROUTE
        val screenshot = "test-screenshot"

        mapboxNavigation.postUserFeedback(
            feedbackType,
            description,
            feedbackEvent,
            screenshot,
            feedbackSubType,
            feedbackMetadata,
        )

        verify(exactly = 1) {
            telemetryWrapper.postUserFeedback(
                feedbackType,
                description,
                feedbackEvent,
                screenshot,
                feedbackSubType,
                feedbackMetadata,
                null,
            )
        }
    }

    @Test
    fun forwardPostCustomEventCallToTelemetry() {
        createMapboxNavigation()

        mapboxNavigation.postCustomEvent("", NavigationCustomEventType.ANALYTICS, "1.0")

        verify(exactly = 1) {
            telemetryWrapper.postCustomEvent("", NavigationCustomEventType.ANALYTICS, "1.0")
        }
    }

    @Test
    fun unregisterAllTelemetryObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { telemetryWrapper.destroy() }
    }

    @Test
    fun unregisterAllTelemetryObserversIsCalledAfterTripSessionStop() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verifyOrder {
            tripSession.stop()
            telemetryWrapper.destroy()
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
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }

        verify(exactly = 1) {
            defaultRerouteController.rerouteOnDeviation(
                any<InternalRerouteController.RoutesCallback>(),
            )
        }
        verify(ordering = Ordering.ORDERED) {
            tripSession.registerOffRouteObserver(any())
            defaultRerouteController.rerouteOnDeviation(
                any<InternalRerouteController.RoutesCallback>(),
            )
        }
    }

    @Test
    fun non_offroute_cancels_reroute() {
        createMapboxNavigation()
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(false)
        }

        verify(exactly = 1) { defaultRerouteController.interrupt() }
        verify(ordering = Ordering.ORDERED) {
            tripSession.registerOffRouteObserver(any())
            defaultRerouteController.interrupt()
        }
    }

    @Test
    fun `new routes are set after reroute`() {
        val newInputRoutes = listOf(
            routeWithId("id#0"),
            routeWithId("id#1"),
            routeWithId("id#2"),
        )
        val validAlternatives = listOf(
            alternativeWithId("id#0"),
            alternativeWithId("id#2"),
        )
        val newAcceptedRoutes = listOf(newInputRoutes[0], newInputRoutes[2])
        val newIgnoredRoutes = listOf(IgnoredRoute(newInputRoutes[1], invalidRouteReason))
        val initialLegIndex = 2

        every {
            defaultRerouteController.rerouteOnDeviation(
                any<InternalRerouteController.RoutesCallback>(),
            )
        } answers {
            (firstArg() as InternalRerouteController.RoutesCallback).onNewRoutes(
                RerouteResult(newInputRoutes, initialLegIndex, RouterOrigin.ONLINE),
            )
        }

        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteValue(newInputRoutes, validAlternatives)

        createMapboxNavigation()
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
                    SetRoutes.Reroute(initialLegIndex),
                ),
            )
        }
    }

    @Test
    fun `new routes are not set after reroute if they are invalid`() {
        val newRoutes = listOf(mockk<NavigationRoute>(relaxed = true), mockk(relaxed = true))

        every {
            defaultRerouteController.rerouteOnDeviation(
                any<InternalRerouteController.RoutesCallback>(),
            )
        } answers {
            (firstArg() as InternalRerouteController.RoutesCallback)
                .onNewRoutes(RerouteResult(newRoutes, 1, RouterOrigin.ONLINE))
        }

        coEvery {
            tripSession.setRoutes(any(), any())
        } returns NativeSetRouteError("some error")

        createMapboxNavigation()
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
    fun reRoute_not_called() {
        createMapboxNavigation()
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(false)

        verify(exactly = 0) {
            defaultRerouteController.rerouteOnDeviation(
                any<InternalRerouteController.RoutesCallback>(),
            )
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
                capture(routeObserversSlot),
            )
        }

        routeObserversSlot.forEach {
            it.onRoutesChanged(RoutesUpdatedResult(routes, ignoredRoutes, reason))
        }

        coVerify(exactly = 1) {
            routeProgressDataProvider.onNewRoutes()
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
                capture(routeObserversSlot),
            )
        }

        routeObserversSlot.forEach {
            it.onRoutesChanged(RoutesUpdatedResult(routes, ignoredRoutes, reason))
        }

        coVerify(exactly = 1) {
            routeProgressDataProvider.onNewRoutes()
        }
    }

    @Test
    fun `don't interrupt reroute requests on a standalone route request`() {
        createMapboxNavigation()
        every { directionsSession.requestRoutes(any(), any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk<NavigationRouterCallback>())

        verify(exactly = 0) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `resetTripSession should reset the navigator`() {
        createMapboxNavigation()

        runBlocking {
            suspendCancellableCoroutine { cont ->
                mapboxNavigation.resetTripSession {
                    cont.resume(Unit)
                }
            }
        }

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

        every { NavigatorLoader.createCacheHandle(any(), capture(slot), any()) } returns mockk()

        every {
            navigationOptions.routingTilesOptions
        } returns RoutingTilesOptions.Builder().build()

        createMapboxNavigation()

        assertTrue(slot.captured.tilesPath.endsWith(RoutingTilesFiles.TILES_PATH_SUB_DIR))
    }

    @Test
    fun `verify tile config dataset`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()

        every { NavigatorLoader.createCacheHandle(any(), capture(slot), any()) } returns mockk()

        every { navigationOptions.routingTilesOptions } returns RoutingTilesOptions.Builder()
            .tilesDataset("someUser.osm")
            .tilesProfile("truck")
            .build()

        createMapboxNavigation()

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
        every { navigationOptions.incidentsOptions } returns IncidentsOptions.Builder()
            .graph("graph")
            .build()

        createMapboxNavigation()

        assertEquals(slot.captured.incidentsOptions!!.graph, "graph")
        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "")
    }

    @Test
    fun `verify incidents options non-null when apiUrl set`() {
        threadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every { NavigatorLoader.createConfig(any(), capture(slot)) } returns mockk()
        every { navigationOptions.incidentsOptions } returns IncidentsOptions.Builder()
            .apiUrl("apiUrl")
            .build()

        createMapboxNavigation()

        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "apiUrl")
        assertEquals(slot.captured.incidentsOptions!!.graph, "")
    }

    @Test
    fun `setRoute pushes the route to the directions session`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()

        val routes = listOf(
            routeWithId("id#0"),
            routeWithId("id#1"),
            routeWithId("id#2"),
        )
        val validAlternatives = listOf(
            alternativeWithId("id#2"),
        )
        val acceptedRoutes = listOf(routeWithId("id#3"), routes[2])
        val ignoredRoutes = listOf(IgnoredRoute(routes[1], invalidRouteReason))
        val initialLegIndex = 2

        coEvery {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(initialLegIndex),
            )
        } returns NativeSetRouteValue(acceptedRoutes, validAlternatives)
        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

        verify(exactly = 1) {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    acceptedRoutes,
                    ignoredRoutes,
                    SetRoutes.NewRoutes(initialLegIndex),
                ),
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
                    SetRoutes.CleanUp,
                )
            } returns NativeSetRouteValue(emptyList(), listOf(alternativeWithId("id#0")))
            mapboxNavigation.setNavigationRoutes(emptyList())

            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        emptyList(),
                        emptyList(),
                        SetRoutes.CleanUp,
                    ),
                )
            }
        }

    @Test
    fun `setRoute does not push the invalid route to the directions session`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val route: NavigationRoute = createNavigationRoute()

            val routes = listOf(route)
            val initialLegIndex = 2

            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteError("some error")
            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

            verify(exactly = 0) {
                directionsSession.setNavigationRoutesFinished(any())
            }
        }

    @Test
    fun `requestRoutes pushes the request to the directions session`() {
        createMapboxNavigation()
        val options = mockk<RouteOptions>()
        val expectedSignature = GetRouteSignature(
            GetRouteSignature.Reason.NEW_ROUTE,
            GetRouteSignature.Origin.APP,
        )
        val callback = mockk<NavigationRouterCallback>()
        every { directionsSession.requestRoutes(options, expectedSignature, callback) } returns 1L

        mapboxNavigation.requestRoutes(options, callback)
        verify(exactly = 1) {
            directionsSession.requestRoutes(options, expectedSignature, callback)
        }
    }

    @Test
    fun `requestRoutes passes back the request id`() {
        createMapboxNavigation()
        val expected = 1L
        val options = mockk<RouteOptions>()
        val expectedSignature = GetRouteSignature(
            GetRouteSignature.Reason.NEW_ROUTE,
            GetRouteSignature.Origin.APP,
        )
        val callback = mockk<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(options, expectedSignature, callback)
        } returns expected

        val actual = mapboxNavigation.requestRoutes(options, callback)

        assertEquals(expected, actual)
    }

    @Test
    fun `requestRoutes doesn't pushes the route to the directions session automatically`() {
        createMapboxNavigation()
        val routes = listOf(mockk<NavigationRoute>(relaxed = true))
        val options = mockk<RouteOptions>()
        val expectedSignature = GetRouteSignature(
            GetRouteSignature.Reason.NEW_ROUTE,
            GetRouteSignature.Origin.APP,
        )
        val possibleInternalCallbackSlot = slot<NavigationRouterCallback>()
        val origin = RouterOrigin.ONLINE
        every { directionsSession.requestRoutes(options, any(), any()) } returns 1L

        mapboxNavigation.requestRoutes(
            options,
            mockk<NavigationRouterCallback>(relaxUnitFun = true),
        )
        verify {
            directionsSession.requestRoutes(
                options,
                expectedSignature,
                capture(possibleInternalCallbackSlot),
            )
        }
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

        every { NavigatorLoader.createCacheHandle(any(), capture(slot), any()) } returns mockk()

        val tilesVersion = "tilesVersion"
        every { navigationOptions.routingTilesOptions } returns RoutingTilesOptions.Builder()
            .tilesVersion(tilesVersion)
            .build()

        createMapboxNavigation()

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

        createMapboxNavigation()

        val tileConfigSlot = slot<TilesConfig>()

        every {
            NavigatorLoader.createCacheHandle(any(), capture(tileConfigSlot), any())
        } returns mockk()

        val tilesVersion = "tilesVersion"
        val latestTilesVersion = "latestTilesVersion"
        fallbackObserverSlot.captured.onFallbackVersionsFound(
            listOf(tilesVersion, latestTilesVersion),
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

            createMapboxNavigation()

            val tileConfigSlot = slot<TilesConfig>()

            every {
                NavigatorLoader.createCacheHandle(any(), capture(tileConfigSlot), any())
            } returns mockk()

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

        createMapboxNavigation()

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
            createMapboxNavigation()
            every { directionsSession.initialLegIndex } returns 0
            mapboxNavigation.startTripSession()
            mapboxNavigation.onDestroy()

            verifyOrder {
                tripSession.registerStateObserver(navigationSession)
                tripSession.start(
                    withTripService = true,
                    withReplayEnabled = false,
                )
                tripSession.stop()
                tripSession.unregisterAllStateObservers()
            }
        }

    @Test(expected = IllegalStateException::class)
    fun `verify that only one instance of MapboxNavigation can be alive`() = runBlocking {
        createMapboxNavigation()
        createMapboxNavigation()
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
        createMapboxNavigation()
        firstInstance.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that startTripSession is not called when destroyed`() = runBlocking {
        val localNavigationSession = NavigationSession()
        every { NavigationComponentProvider.createNavigationSession() } answers {
            localNavigationSession
        }

        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        mapboxNavigation.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that stopTripSession is not called when destroyed`() = runBlocking {
        createMapboxNavigation()
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
                        SetRoutes.NewRoutes(0),
                    ),
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
                match { it.setRoutesInfo is SetRoutes.Alternatives },
            )
            directionsSession.setNavigationRoutesFinished(
                match { it.setRoutesInfo is SetRoutes.Alternatives },
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
                    SetRoutes.NewRoutes(0),
                ),
            )
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    shortRoutes,
                    emptyList(),
                    SetRoutes.NewRoutes(0),
                ),
            )
        }
    }

    @Test
    fun `set route - new routes immediately interrupts reroute`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()

        val shortRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
        coEvery { tripSession.setRoutes(shortRoutes, any()) } coAnswers {
            delay(50L)
            NativeSetRouteValue(shortRoutes, emptyList())
        }

        pauseDispatcher {
            mapboxNavigation.setNavigationRoutes(shortRoutes)
            verify(exactly = 1) { defaultRerouteController.interrupt() }
        }
    }

    @Test
    fun `set route - clean up immediately interrupts reroute`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val initialRoutes = mutableListOf<NavigationRoute>(mockk(relaxed = true))
            every { directionsSession.routes } returns initialRoutes
            val updatedRoutes = emptyList<NavigationRoute>()

            pauseDispatcher {
                mapboxNavigation.setNavigationRoutes(updatedRoutes)
                verify(exactly = 1) { defaultRerouteController.interrupt() }
            }
        }

    @Test
    fun `set route - reroute immediately interrupts reroute`() =
        coroutineRule.runBlockingTest {
            val rerouteCallbackSlot = slot<InternalRerouteController.RoutesCallback>()
            every {
                defaultRerouteController.rerouteOnDeviation(capture(rerouteCallbackSlot))
            } just Runs
            val observers = mutableListOf<OffRouteObserver>()
            every { tripSession.registerOffRouteObserver(capture(observers)) } just Runs
            val initialRoutes = mutableListOf<NavigationRoute>(mockk(relaxed = true))
            every { directionsSession.routes } returns initialRoutes
            val newRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
            coEvery {
                tripSession.setRoutes(newRoutes, any())
            } returns NativeSetRouteValue(newRoutes, emptyList())
            createMapboxNavigation()

            observers.forEach {
                it.onOffRouteStateChanged(true)
            }
            pauseDispatcher {
                rerouteCallbackSlot.captured.onNewRoutes(
                    RerouteResult(newRoutes, 0, RouterOrigin.ONLINE),
                )
                verify(exactly = 1) { defaultRerouteController.interrupt() }
            }
        }

    @Test
    fun `set route - alternatives update does not interrupt reroute`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
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

            verify(exactly = 0) { defaultRerouteController.interrupt() }
        }

    @Test
    fun `set route - refresh update does not interrupt reroute`() =
        coroutineRule.runBlockingTest {
            val routeObserversSlot = mutableListOf<RoutesObserver>()
            every {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot),
                )
            } just Runs
            createMapboxNavigation()
            val initialRoutes = listOf<NavigationRoute>(mockk(relaxed = true))
            val primaryRoute = mockk<NavigationRoute>(relaxed = true)
            val alternativeRoute = mockk<NavigationRoute>(relaxed = true)
            val primaryRouteProgressData = RouteProgressData(5, 12, 43)
            val alternativeRouteProgressData = RouteProgressData(1, 2, 3)
            val routesRefreshData = RoutesRefresherResult(
                RouteRefresherResult(
                    primaryRoute,
                    primaryRouteProgressData,
                    RouteRefresherStatus.SUCCESS,
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute,
                        alternativeRouteProgressData,
                        RouteRefresherStatus.SUCCESS,
                    ),
                ),
            )

            routeObserversSlot.forEach {
                it.onRoutesChanged(
                    createRoutesUpdatedResult(
                        initialRoutes,
                        RoutesExtra.ROUTES_UPDATE_REASON_NEW,
                    ),
                )
            }
            interceptRefreshObserver().onRoutesRefreshed(routesRefreshData)

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(primaryRoute, alternativeRoute),
                    SetRoutes.RefreshRoutes(primaryRouteProgressData),
                )
            }
            verify(exactly = 0) { defaultRerouteController.interrupt() }
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
                    SetRoutes.NewRoutes(0),
                )
            } returns NativeSetRouteValue(processedRoutes, nativeAlternatives)

            mapboxNavigation.setNavigationRoutes(routes)

            coVerifyOrder {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(0),
                )
                routeAlternativesController.processAlternativesMetadata(
                    processedRoutes,
                    nativeAlternatives,
                )
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        processedRoutes,
                        emptyList(),
                        SetRoutes.NewRoutes(0),
                    ),
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
                    SetRoutes.RefreshRoutes(routeProgressData),
                )
            } returns NativeSetRouteError("some error")

            verify {
                directionsSession.registerSetNavigationRoutesFinishedObserver(
                    capture(routeObserversSlot),
                )
            }
            routeObserversSlot.forEach {
                it.onRoutesChanged(
                    RoutesUpdatedResult(
                        routes,
                        ignoredRoutes,
                        RoutesExtra.ROUTES_UPDATE_REASON_NEW,
                    ),
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
                routeWithId("id#2"),
            )
            val validAlternatives = listOf(
                alternativeWithId("id#0"),
                alternativeWithId("id#2"),
            )
            val acceptedRoutes = listOf(inputRoutes[0], inputRoutes[2])
            val ignoredRoutes = listOf(IgnoredRoute(inputRoutes[1], invalidRouteReason))
            coEvery {
                tripSession.setRoutes(
                    inputRoutes,
                    SetRoutes.NewRoutes(0),
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
            every { tripSession.getState() } returns TripSessionState.STOPPED

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
                    DirectionsSessionRoutes(acceptedRoutes, ignoredRoutes, setRoutesInfo),
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
    fun `setNavigationRoutes alternative for current primary route`() =
        coroutineRule.runBlockingTest {
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
    fun `setNavigationRoutes alternative for changed primary route`() =
        coroutineRule.runBlockingTest {
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
    fun `setNavigationRoutes alternative for outdated primary route`() =
        coroutineRule.runBlockingTest {
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
                    capture(routeObserversSlot),
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
                    RouteRefresherStatus.SUCCESS,
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute1,
                        alternativeRoute1ProgressData,
                        RouteRefresherStatus.SUCCESS,
                    ),
                    RouteRefresherResult(
                        alternativeRoute2,
                        alternativeRoute2ProgressData,
                        RouteRefresherStatus.SUCCESS,
                    ),
                ),
            )
            interceptRefreshObserver().onRoutesRefreshed(routesRefreshData)

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(primaryRoute, alternativeRoute1, alternativeRoute2),
                    SetRoutes.RefreshRoutes(primaryRouteProgressData),
                )
            }
            verify(exactly = 1) {
                directionsSession.setNavigationRoutesFinished(
                    DirectionsSessionRoutes(
                        acceptedRefreshRoutes,
                        ignoredRefreshRoutes,
                        SetRoutes.RefreshRoutes(primaryRouteProgressData),
                    ),
                )
            }
        }

    @Test
    fun `manually refreshed route updates routeRefreshController`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val primary: NavigationRoute = mockk(relaxed = true)
            val routes = listOf(primary)
            mapboxNavigation.setManuallyRefreshedRoutes(routes)

            val routesSlot = mutableListOf<List<NavigationRoute>>()
            verify {
                routeRefreshController.onRoutesRefreshedManually(capture(routesSlot))
            }

            routesSlot[0].run {
                assertEquals(routes, this)
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
                    capture(routeObserversSlot),
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
                    RouteRefresherStatus.SUCCESS,
                ),
                listOf(
                    RouteRefresherResult(
                        alternativeRoute,
                        alternativeRouteProgressData,
                        RouteRefresherStatus.SUCCESS,
                    ),
                ),
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
                    SetRoutes.RefreshRoutes(primaryRouteProgressData),
                )
            }
            verify(exactly = 0) {
                directionsSession.setNavigationRoutesFinished(any())
            }
        }

    @Test
    fun `default reroute controller by default`() {
        createMapboxNavigation()
        assertTrue(mapboxNavigation.isRerouteEnabled())
        assertSame(defaultRerouteController, mapboxNavigation.getRerouteController())
    }

    @Test
    fun `default reroute controller is returned after re-enabling reroute`() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        mapboxNavigation.setRerouteEnabled(true)

        assertTrue(mapboxNavigation.isRerouteEnabled())
        assertSame(defaultRerouteController, mapboxNavigation.getRerouteController())
    }

    @Test
    fun `reroute controller is null when reroute is disabled`() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)

        assertFalse(mapboxNavigation.isRerouteEnabled())
        assertNull(mapboxNavigation.getRerouteController())
    }

    @Test
    fun `rerouting in fetching state interrupted when reroute option disabled`() {
        every { defaultRerouteController.state } returns RerouteState.FetchingRoute
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        verify(exactly = 1) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `rerouting in idle state is not interrupted when reroute option disabled`() {
        every { defaultRerouteController.state } returns RerouteState.Idle
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        verify(exactly = 0) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `rerouting in interrupted state is not interrupted again when reroute option disabled`() {
        every { defaultRerouteController.state } returns RerouteState.Interrupted
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        verify(exactly = 0) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `rerouting in failed state is not interrupted when reroute option disabled`() {
        every { defaultRerouteController.state } returns mockk<RerouteState.Failed>()
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        verify(exactly = 0) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `rerouting in fetched state is not interrupted when reroute option disabled`() {
        every { defaultRerouteController.state } returns mockk<RerouteState.RouteFetched>()
        createMapboxNavigation()
        mapboxNavigation.setRerouteEnabled(false)
        verify(exactly = 0) { defaultRerouteController.interrupt() }
    }

    @Test
    fun `re-enabling reroute does not cause rerouting or interruption`() {
        createMapboxNavigation()

        mapboxNavigation.setRerouteEnabled(false)
        // clearMocks(defaultRerouteController)

        mapboxNavigation.setRerouteEnabled(true)
        verify(exactly = 0) {
            defaultRerouteController.interrupt()
        }

        verify(exactly = 0) {
            defaultRerouteController.reroute(any<RerouteController.RoutesCallback>())
        }
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
    fun testReturnsExpectedDataInputsManager() {
        val nativeInputsService = mockk<com.mapbox.navigator.InputsServiceHandleInterface>(
            relaxed = true,
        )
        every { navigator.inputsService } returns nativeInputsService

        createMapboxNavigation()

        val platformData = mockk<CompassData>(relaxed = true).apply {
            every { mapToNative() } returns mockk()
        }
        mapboxNavigation.dataInputsManager.updateCompassData(platformData)

        verify(exactly = 1) {
            nativeInputsService.updateCompassData(any())
        }
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

    @Test
    fun updateExternalSensorData() {
        createMapboxNavigation()

        val nativeCallbackSlot = slot<com.mapbox.navigator.UpdateExternalSensorDataCallback>()
        every { navigator.updateExternalSensorData(any(), capture(nativeCallbackSlot)) } answers {
            nativeCallbackSlot.captured.run(true)
        }

        val weatherSensorData = SensorData.Weather(SensorData.Weather.ConditionType.FOG)
        val callback = mockk<UpdateExternalSensorDataCallback>(relaxed = true)
        mapboxNavigation.updateExternalSensorData(weatherSensorData, callback)

        verify(exactly = 1) {
            navigator.updateExternalSensorData(
                eq(weatherSensorData.toNativeSensorData()),
                eq(nativeCallbackSlot.captured),
            )
        }

        verify(exactly = 1) {
            callback.onResult(eq(true))
        }
    }

    @Test
    fun setAdasisMessageObserver() {
        createMapboxNavigation()

        val testMessageBuffer = listOf<Byte>(1, 2, 3)
        val nativeCallbackSlot = slot<ADASISv2MessageCallback>()
        every { navigator.setAdasisMessageCallback(capture(nativeCallbackSlot), any()) } answers {
            nativeCallbackSlot.captured.run(testMessageBuffer)
        }

        val dataSendingConfig = AdasisDataSendingConfig.Builder(
            AdasisMessageBinaryFormat.FlatBuffers,
        ).build()

        val adasisConfig = AdasisConfig.Builder(dataSendingConfig).build()
        val callback = mockk<AdasisV2MessageObserver>(relaxed = true)
        mapboxNavigation.setAdasisMessageObserver(adasisConfig, callback)

        verify(exactly = 1) {
            navigator.setAdasisMessageCallback(
                nativeCallbackSlot.captured,
                adasisConfig.toNativeAdasisConfig(),
            )
        }

        verify(exactly = 1) {
            callback.onMessage(eq(testMessageBuffer))
        }
    }

    @Test
    fun resetAdasisMessageCallback() {
        createMapboxNavigation()
        mapboxNavigation.resetAdasisMessageObserver()

        verify(exactly = 1) {
            navigator.resetAdasisMessageCallback()
        }
    }

    @Test
    fun destroy_resetAdasisMessageCallback() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            navigator.resetAdasisMessageCallback()
        }
    }

    @Test
    fun registerHistoryRecordingEnabledObserver() {
        val observer = mockk<HistoryRecordingEnabledObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.registerHistoryRecordingEnabledObserver(observer)

        verify {
            manualHistoryRecorder.registerHistoryRecordingEnabledObserver(observer)
            copilotHistoryRecorder.registerHistoryRecordingEnabledObserver(observer)
        }
    }

    @Test
    fun unregisterHistoryRecordingEnabledObserver() {
        val observer = mockk<HistoryRecordingEnabledObserver>(relaxed = true)
        createMapboxNavigation()

        mapboxNavigation.unregisterHistoryRecordingEnabledObserver(observer)

        verify {
            manualHistoryRecorder.unregisterHistoryRecordingEnabledObserver(observer)
            copilotHistoryRecorder.unregisterHistoryRecordingEnabledObserver(observer)
        }
    }

    @Test
    fun onDestroyUnregistersAllHistoryRecordingEnabledObservers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify {
            manualHistoryRecorder.unregisterAllHistoryRecordingEnabledObservers()
            copilotHistoryRecorder.unregisterAllHistoryRecordingEnabledObservers()
        }
    }

    @Test
    fun `verify router is recreated on fallback`() {
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

        createMapboxNavigation()

        val newNativeRouter = mockk<RouterInterface>()
        every {
            navigator.getRouter()
        } returns newNativeRouter

        fallbackObserverSlot.captured.onFallbackVersionsFound(listOf("tilesVersion"))

        verify(exactly = 1) { nativeRouter.cancelAll() }
        assertEquals(newNativeRouter, routerWrapperSlot.captured.router)
    }

    @Test
    fun initializesSystemLocaleWatcherOnInit() {
        val localeWatcher = mockk<SystemLocaleWatcher>(relaxed = true)
        every { SystemLocaleWatcher.create(any(), any(), any()) } returns localeWatcher

        createMapboxNavigation()
        verify(exactly = 1) {
            SystemLocaleWatcher.create(applicationContext, navigator, any())
        }
        verify {
            localeWatcher wasNot called
        }
    }

    @Test
    fun destroysSystemLocaleWatcherOnDestroy() {
        val localeWatcher = mockk<SystemLocaleWatcher>(relaxed = true)
        every { SystemLocaleWatcher.create(any(), any(), any()) } returns localeWatcher

        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            localeWatcher.destroy()
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
