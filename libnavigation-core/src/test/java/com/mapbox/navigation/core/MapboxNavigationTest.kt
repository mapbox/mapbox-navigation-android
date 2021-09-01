package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.MapboxSDKCommon
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.IncidentsOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.TilesConfig
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

@Config(shadows = [ShadowReachabilityFactory::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxNavigationTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val accessToken = "pk.1234"
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    private val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    private val routeRefreshController: RouteRefreshController = mockk(relaxUnitFun = true)
    private val routeAlternativesController: RouteAlternativesController =
        mockk(relaxUnitFun = true)
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val navigationSession: NavigationSession = mockk(relaxed = true)
    private val billingController: BillingController = mockk(relaxUnitFun = true)
    private val logger: Logger = mockk(relaxUnitFun = true)
    private val rerouteController: RerouteController = mockk(relaxUnitFun = true)
    private val tripSessionLocationEngine: TripSessionLocationEngine = mockk(relaxUnitFun = true)
    private lateinit var navigationOptions: NavigationOptions
    private val arrivalProgressObserver: ArrivalProgressObserver = mockk(relaxUnitFun = true)

    private val applicationContext: Context = mockk(relaxed = true) {
        every { inferDeviceLocale() } returns Locale.US
        every {
            getSystemService(Context.NOTIFICATION_SERVICE)
        } returns mockk<NotificationManager>()
        every { getSystemService(Context.ALARM_SERVICE) } returns mockk<AlarmManager>()
        every {
            getSharedPreferences(
                MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
            )
        } returns mockk(relaxed = true) {
            every { getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"
        }
        every { packageManager } returns mockk(relaxed = true)
        every { packageName } returns "com.mapbox.navigation.core.MapboxNavigationTest"
        every { filesDir } returns File("some/path")
        every { navigator.cache } returns mockk()
        every { navigator.getHistoryRecorderHandle() } returns null
        every { navigator.experimental } returns mockk()
    }

    private lateinit var mapboxNavigation: MapboxNavigation

    companion object {
        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        }
    }

    @Before
    fun setUp() {
        mockkObject(MapboxSDKCommon)
        every {
            MapboxSDKCommon.getContext().getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockk<ConnectivityManager>()
        mockkObject(MapboxModuleProvider)

        val hybridRouter: Router = mockk(relaxUnitFun = true)
        every {
            MapboxModuleProvider.createModule<Router>(
                MapboxModuleType.NavigationRouter,
                any()
            )
        } returns hybridRouter
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns logger
        every {
            MapboxModuleProvider.createModule<TripNotification>(
                MapboxModuleType.NavigationTripNotification,
                any()
            )
        } returns mockk()

        mockkObject(NavigationComponentProvider)
        mockkObject(RouteRefreshControllerProvider)
        every {
            RouteRefreshControllerProvider.createRouteRefreshController(
                any(), any(), any(), any()
            )
        } returns routeRefreshController
        mockkObject(RouteAlternativesControllerProvider)
        every {
            RouteAlternativesControllerProvider.create(any(), any(), any(), any(), any())
        } returns routeAlternativesController

        every { applicationContext.applicationContext } returns applicationContext

        navigationOptions = provideNavigationOptions().build()

        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()
        mockNavTelemetry()
        every {
            NavigationComponentProvider.createBillingController(any(), any(), any(), any())
        } returns billingController
        every {
            NavigationComponentProvider.createArrivalProgressObserver(tripSession)
        } returns arrivalProgressObserver

        every { navigator.create(any(), any(), any(), any(), any()) } returns navigator
    }

    @After
    fun tearDown() {
        if (this::mapboxNavigation.isInitialized) {
            mapboxNavigation.onDestroy()
        }

        unmockkObject(MapboxSDKCommon)
        unmockkObject(MapboxModuleProvider)
        unmockkObject(LoggerProvider)
        unmockkObject(NavigationComponentProvider)
        unmockkObject(RouteRefreshControllerProvider)
        unmockkObject(RouteAlternativesControllerProvider)
        unmockkObject(MapboxNavigationTelemetry)

        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
    }

    @Test
    fun sanity() {
        createMapboxNavigation()
        assertNotNull(mapboxNavigation)
    }

    @Test
    fun startSessionWithService() {
        createMapboxNavigation()
        every { tripSession.isRunningWithForegroundService() } returns true
        mapboxNavigation.startTripSession()

        assertTrue(mapboxNavigation.isRunningForegroundService())
    }

    @Test
    fun startSessionWithoutService() {
        createMapboxNavigation()
        every { tripSession.isRunningWithForegroundService() } returns false
        mapboxNavigation.startTripSession(false)

        assertFalse(mapboxNavigation.isRunningForegroundService())
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
        verify(exactly = 2) { directionsSession.registerRoutesObserver(any()) }
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
    fun init_registerOffRouteObserver_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        ThreadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        verify(exactly = 2) { tripSession.registerOffRouteObserver(any()) }
    }

    @Test
    fun destroy_unregisterAllOffRouteObservers_MapboxNavigation_recreated() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()
        ThreadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()
        mapboxNavigation = MapboxNavigation(navigationOptions)

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
        verify(exactly = 1) { tripSession.registerStateObserver(any()) }
    }

    @Test
    fun onDestroy_unregisters_DirectionSession_observers() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.unregisterAllRoutesObservers() }
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
    fun onDestroySetsRoutesToEmpty() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.setRoutes(emptyList()) }
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

        verify(exactly = 1) { navigator.resetRideSession() }
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
    fun arrival_controller_register() {
        createMapboxNavigation()
        val arrivalController: ArrivalController = mockk()

        mapboxNavigation.setArrivalController(arrivalController)

        verify { tripSession.registerRouteProgressObserver(any<ArrivalProgressObserver>()) }
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

        verify(exactly = 1) { rerouteController.reroute(any()) }
        verify(ordering = Ordering.ORDERED) {
            tripSession.registerOffRouteObserver(any())
            rerouteController.reroute(any())
        }
    }

    @Test
    fun reRoute_not_called() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(false)

        verify(exactly = 0) { rerouteController.reroute(any()) }
    }

    @Test
    fun internalRouteObserver_notEmpty() {
        createMapboxNavigation()
        val primary: DirectionsRoute = mockk()
        val secondary: DirectionsRoute = mockk()
        val routes = listOf(primary, secondary)
        val initialLegIndex = 2
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        every { directionsSession.initialLegIndex } returns initialLegIndex
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }

        routeObserversSlot.forEach {
            it.onRoutesChanged(routes)
        }

        verify { tripSession.setRoute(primary, initialLegIndex) }
        verify { routeRefreshController.restart(primary) }
    }

    @Test
    fun internalRouteObserver_empty() {
        createMapboxNavigation()
        val routes = emptyList<DirectionsRoute>()
        val initialLegIndex = 0
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        every { directionsSession.initialLegIndex } returns initialLegIndex
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }

        routeObserversSlot.forEach {
            it.onRoutesChanged(routes)
        }

        verify { tripSession.setRoute(null, initialLegIndex) }
        verify { routeRefreshController.stop() }
    }

    @Test
    fun `don't interrupt reroute requests on a standalone route request`() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk())

        verify(exactly = 0) { rerouteController.interrupt() }
    }

    @Test
    fun interrupt_reroute_on_set_routes() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        mapboxNavigation.setRoutes(listOf())

        verify(exactly = 1) { rerouteController.interrupt() }
    }

    @Test
    fun `don't interrupt route alternatives on a standalone route request`() {
        createMapboxNavigation()
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk())

        verify(exactly = 0) { routeAlternativesController.interrupt() }
    }

    @Test
    fun `interrupt route alternatives on set route`() {
        createMapboxNavigation()
        mapboxNavigation.setRoutes(listOf())

        verify(exactly = 1) { routeAlternativesController.interrupt() }
    }

    @Test
    fun interrupt_reroute_process_when_new_reroute_controller_has_been_set() {
        createMapboxNavigation()
        mapboxNavigation.setRerouteController(rerouteController)
        val newRerouteController: RerouteController = mockk(relaxUnitFun = true)
        val observers = mutableListOf<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(observers)) }
        every { rerouteController.state } returns RerouteState.FetchingRoute

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }
        mapboxNavigation.setRerouteController(newRerouteController)

        verify(exactly = 1) { rerouteController.reroute(any()) }
        verify(exactly = 1) { rerouteController.interrupt() }
        verify(exactly = 1) { newRerouteController.reroute(any()) }
        verifyOrder {
            rerouteController.reroute(any())
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

        verify { navigator.resetRideSession() }
    }

    @Test
    fun `verify tile config path`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), any(), capture(slot), any(), any()
            )
        } returns navigator
        val options = navigationOptions.toBuilder()
            .routingTilesOptions(RoutingTilesOptions.Builder().build())
            .build()

        mapboxNavigation = MapboxNavigation(options)

        assertTrue(slot.captured.tilesPath.endsWith(RoutingTilesFiles.TILES_PATH_SUB_DIR))
    }

    @Test
    fun `verify tile config dataset`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), any(), capture(slot), any(), any()
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

        mapboxNavigation = MapboxNavigation(options)

        assertEquals(slot.captured.endpointConfig!!.dataset, "someUser.osm/truck")
    }

    @Test
    fun `verify incidents options null when no params set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), capture(slot), any(), any(), any()
            )
        } returns navigator

        mapboxNavigation = MapboxNavigation(navigationOptions)

        assertNull(slot.captured.incidentsOptions)
    }

    @Test
    fun `verify incidents options non-null when graph set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), capture(slot), any(), any(), any()
            )
        } returns navigator
        val options = navigationOptions.toBuilder()
            .incidentsOptions(
                IncidentsOptions.Builder()
                    .graph("graph")
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options)

        assertEquals(slot.captured.incidentsOptions!!.graph, "graph")
        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "")
    }

    @Test
    fun `verify incidents options non-null when apiUrl set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), capture(slot), any(), any(), any()
            )
        } returns navigator
        val options = navigationOptions.toBuilder()
            .incidentsOptions(
                IncidentsOptions.Builder()
                    .apiUrl("apiUrl")
                    .build()
            )
            .build()

        mapboxNavigation = MapboxNavigation(options)

        assertEquals(slot.captured.incidentsOptions!!.apiUrl, "apiUrl")
        assertEquals(slot.captured.incidentsOptions!!.graph, "")
    }

    @Test
    fun `setRoute pushes the route to the directions session`() {
        createMapboxNavigation()
        val route: DirectionsRoute = mockk()
        val routeOptions: RouteOptions = mockk()
        every { route.routeOptions() } returns routeOptions
        every { route.geometry() } returns "geometry"
        every { route.legs() } returns emptyList()
        every { routeOptions.overview() } returns "full"
        every { routeOptions.annotationsList() } returns emptyList()

        val routes = listOf(route)
        val initialLegIndex = 2

        mapboxNavigation.setRoutes(routes, initialLegIndex)

        verify(exactly = 1) { directionsSession.setRoutes(routes, initialLegIndex) }
    }

    @Test
    fun `requestRoutes pushes the request to the directions session`() {
        createMapboxNavigation()
        val options = mockk<RouteOptions>()
        val callback = mockk<RouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns 1L

        mapboxNavigation.requestRoutes(options, callback)
        verify(exactly = 1) { directionsSession.requestRoutes(options, callback) }
    }

    @Test
    fun `requestRoutes passes back the request id`() {
        createMapboxNavigation()
        val expected = 1L
        val options = mockk<RouteOptions>()
        val callback = mockk<RouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns expected

        val actual = mapboxNavigation.requestRoutes(options, callback)

        assertEquals(expected, actual)
    }

    @Test
    fun `requestRoutes doesn't pushes the route to the directions session automatically`() {
        createMapboxNavigation()
        val routes = listOf(mockk<DirectionsRoute>())
        val options = mockk<RouteOptions>()
        val possibleInternalCallbackSlot = slot<RouterCallback>()
        val origin = mockk<RouterOrigin>()
        every { directionsSession.requestRoutes(options, any()) } returns 1L

        mapboxNavigation.requestRoutes(options, mockk(relaxUnitFun = true))
        verify { directionsSession.requestRoutes(options, capture(possibleInternalCallbackSlot)) }
        possibleInternalCallbackSlot.captured.onRoutesReady(routes, origin)

        verify(exactly = 0) { directionsSession.setRoutes(routes) }
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
        ThreadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(), any(), capture(slot), any(), any()
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

        mapboxNavigation = MapboxNavigation(options)

        assertEquals(tilesVersion, slot.captured.endpointConfig?.version)
        assertFalse(slot.captured.endpointConfig?.isFallback!!)
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on fallback`() {
        ThreadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        every { directionsSession.routes } returns emptyList()
        every { tripSession.getRouteProgress() } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        val tileConfigSlot = slot<TilesConfig>()
        every {
            navigator.recreate(
                any(),
                any(),
                capture(tileConfigSlot),
                any(),
                any()
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
    fun `verify tile config tilesVersion and isFallback on return to latest tiles version`() {
        ThreadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        every { directionsSession.routes } returns emptyList()
        every { tripSession.getRouteProgress() } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        val tileConfigSlot = slot<TilesConfig>()
        every {
            navigator.recreate(
                any(),
                any(),
                capture(tileConfigSlot),
                any(),
                any()
            )
        } just Runs

        fallbackObserverSlot.captured.onCanReturnToLatest("")

        assertEquals("", tileConfigSlot.captured.endpointConfig?.version)
        assertFalse(tileConfigSlot.captured.endpointConfig?.isFallback!!)
    }

    @Test
    fun `verify route and routeProgress are set after navigator recreation`() = runBlocking {
        ThreadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        val route: DirectionsRoute = mockk()
        val routeProgress: RouteProgress = mockk()
        val legProgress: RouteLegProgress = mockk()
        val index = 137
        every { directionsSession.routes } returns listOf(route)
        every { tripSession.getRouteProgress() } returns routeProgress
        every { routeProgress.currentLegProgress } returns legProgress
        every { legProgress.legIndex } returns index
        coEvery { navigator.setRoute(any(), any()) } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        fallbackObserverSlot.captured.onFallbackVersionsFound(listOf("version"))

        coVerify {
            navigator.setRoute(route, index)
        }
    }

    @Test
    fun `verify that session state callbacks are always delivered to NavigationSession`() =
        runBlocking {
            mapboxNavigation = MapboxNavigation(navigationOptions)
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
        mapboxNavigation = MapboxNavigation(navigationOptions)
        mapboxNavigation = MapboxNavigation(navigationOptions)
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
        mapboxNavigation = MapboxNavigation(navigationOptions)
        firstInstance.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that startTripSession is not called when destroyed`() = runBlocking {
        val localNavigationSession = NavigationSession()
        every { NavigationComponentProvider.createNavigationSession() } answers {
            localNavigationSession
        }

        mapboxNavigation = MapboxNavigation(navigationOptions)
        mapboxNavigation.onDestroy()
        mapboxNavigation.startTripSession()
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that stopTripSession is not called when destroyed`() = runBlocking {
        mapboxNavigation = MapboxNavigation(navigationOptions)
        mapboxNavigation.onDestroy()
        mapboxNavigation.stopTripSession()
    }

    @Test
    fun `verify that empty routes are not passed to the billing controller`() {
        createMapboxNavigation()
        mapboxNavigation.setRoutes(emptyList())

        verify(exactly = 0) { billingController.onExternalRouteSet(any()) }
    }

    @Test
    fun `external route is first provided to the billing controller before directions session`() {
        createMapboxNavigation()
        val routes = listOf(mockk<DirectionsRoute>())

        mapboxNavigation.setRoutes(routes)

        verifyOrder {
            billingController.onExternalRouteSet(routes.first())
            directionsSession.setRoutes(routes)
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

    private fun createMapboxNavigation() {
        mapboxNavigation = MapboxNavigation(navigationOptions)
    }

    private fun mockNativeNavigator() {
        every {
            NavigationComponentProvider.createNativeNavigator(any(), any(), any(), any(), any())
        } returns navigator
    }

    private fun mockTripService() {
        every {
            NavigationComponentProvider.createTripService(
                applicationContext,
                any(),
                logger
            )
        } returns tripService
    }

    private fun mockTripSession() {
        every {
            NavigationComponentProvider.createTripSessionLocationEngine(
                navigationOptions = navigationOptions
            )
        } returns tripSessionLocationEngine

        every {
            NavigationComponentProvider.createTripSession(
                tripService = tripService,
                tripSessionLocationEngine = tripSessionLocationEngine,
                navigator = navigator,
                logger = logger,
            )
        } returns tripSession
        every { tripSession.getRouteProgress() } returns routeProgress
    }

    private fun mockDirectionSession() {
        every { NavigationComponentProvider.createDirectionsSession(any()) } answers {
            directionsSession
        }
        // TODO Needed for telemetry - Free Drive (empty list) for now
        every { directionsSession.routes } returns emptyList()
    }

    private fun mockNavigationSession() {
        every { NavigationComponentProvider.createNavigationSession() } answers {
            navigationSession
        }
    }

    private fun mockNavTelemetry() {
        mockkObject(MapboxNavigationTelemetry)
        every { MapboxNavigationTelemetry.initialize(any(), any(), any(), any(), any()) } just runs
        every { MapboxNavigationTelemetry.destroy(any()) } just runs
        every {
            MapboxNavigationTelemetry.postUserFeedback(any(), any(), any(), any(), any(), any())
        } just runs
    }

    private fun provideNavigationOptions() =
        NavigationOptions
            .Builder(applicationContext)
            .accessToken(accessToken)
            .distanceFormatterOptions(distanceFormatterOptions)
            .navigatorPredictionMillis(1500L)
            .routingTilesOptions(routingTilesOptions)
            .timeFormatType(NONE_SPECIFIED)
            .locationEngine(locationEngine)
}
