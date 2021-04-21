package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.core.constants.Constants
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.IncidentsOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.TilesConfig
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val accessToken = "pk.1234"
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    private val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    private val fasterRouteRequestCallback: RoutesRequestCallback = mockk(relaxed = true)
    private val routeRefreshController: RouteRefreshController = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptions = provideDefaultRouteOptionsBuilder().build()
    private val routes: List<DirectionsRoute> = listOf(mockk())
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val navigationSession: NavigationSession = mockk(relaxUnitFun = true)
    private val logger: Logger = mockk(relaxUnitFun = true)
    private lateinit var rerouteController: RerouteController
    private lateinit var navigationOptions: NavigationOptions

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
    }

    private lateinit var mapboxNavigation: MapboxNavigation

    companion object {
        private const val DEFAULT_REROUTE_BEARING_ANGLE = 11f

        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        }
    }

    @Before
    fun setUp() {
        mockkObject(MapboxModuleProvider)
        val hybridRouter: Router = mockk(relaxUnitFun = true)
        every {
            MapboxModuleProvider.createModule<Router>(
                MapboxModuleType.NavigationRouter,
                any()
            )
        } returns hybridRouter
        every {
            MapboxModuleProvider.createModule<Logger>(
                MapboxModuleType.CommonLogger,
                any()
            )
        } returns logger
        every {
            MapboxModuleProvider.createModule<TripNotification>(
                MapboxModuleType.NavigationTripNotification,
                any()
            )
        } returns mockk()

        mockkObject(NavigationComponentProvider)

        every { applicationContext.applicationContext } returns applicationContext

        navigationOptions = provideNavigationOptions().build()

        mockLocation()
        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()

        every { navigator.create(any(), any(), any(), any()) } returns navigator

        mapboxNavigation = MapboxNavigation(navigationOptions)

        rerouteController = mockk(relaxUnitFun = true)
        mapboxNavigation.setRerouteController(rerouteController)
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxModuleProvider)
        unmockkObject(NavigationComponentProvider)

        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
    }

    @Test
    fun sanity() {
        assertNotNull(mapboxNavigation)

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_routesObs_internalRouteObs_navigationSession_and_TelemetryLocAndProgressDisptchr() {
        verify(exactly = 3) { directionsSession.registerRoutesObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerOffRouteObserver() {
        verify(exactly = 2) { tripSession.registerOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun destroy_unregisterOffRouteObserver() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerOffRouteObserver_MapboxNavigation_recreated() {
        ThreadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        verify(exactly = 4) { tripSession.registerOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun destroy_unregisterOffRouteObserver_MapboxNavigation_recreated() {
        ThreadController.cancelAllUICoroutines()
        val navigationOptions = provideNavigationOptions().build()
        mapboxNavigation = MapboxNavigation(navigationOptions)

        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_routeRefreshController_start_called_when_isRouteRefresh_enabled() {
        ThreadController.cancelAllUICoroutines()
        mockkObject(RouteRefreshControllerProvider)
        every {
            RouteRefreshControllerProvider.createRouteRefreshController(
                directionsSession,
                tripSession,
                logger
            )
        } returns routeRefreshController
        every { routeRefreshController.start() } returns mockk()
        val navigationOptions = provideNavigationOptions()
            .isRouteRefreshEnabled(true)
            .build()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        verify(exactly = 1) { routeRefreshController.start() }

        unmockkObject(RouteRefreshControllerProvider)
    }

    @Test
    fun init_routeRefreshController_start_not_called_when_isRouteRefresh_disabled() {
        ThreadController.cancelAllUICoroutines()
        mockkObject(RouteRefreshControllerProvider)
        every {
            RouteRefreshControllerProvider.createRouteRefreshController(
                directionsSession,
                tripSession,
                logger
            )
        } returns routeRefreshController
        every { routeRefreshController.start() } returns mockk()
        val navigationOptions = provideNavigationOptions()
            .isRouteRefreshEnabled(false)
            .build()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        verify(exactly = 0) { routeRefreshController.start() }

        unmockkObject(RouteRefreshControllerProvider)
    }

    @Test
    fun registerMapMatcherResultObserver() {
        val observer: MapMatcherResultObserver = mockk()
        mapboxNavigation.registerMapMatcherResultObserver(observer)

        verify(exactly = 1) { tripSession.registerMapMatcherResultObserver(observer) }
    }

    @Test
    fun unregisterMapMatcherResultObserver() {
        val observer: MapMatcherResultObserver = mockk()
        mapboxNavigation.unregisterMapMatcherResultObserver(observer)

        verify(exactly = 1) { tripSession.unregisterMapMatcherResultObserver(observer) }
    }

    @Test
    fun init_registerStateObserver_navigationSession() {
        verify(exactly = 1) { tripSession.registerStateObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerNavigationSessionStateObserver() {
        verify(exactly = 1) { navigationSession.registerNavigationSessionStateObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun onDestroy_unregisters_DirectionSession_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.unregisterAllRoutesObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_location_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllLocationObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_routeProgress_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllRouteProgressObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_offRoute_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllOffRouteObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_state_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllStateObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_routeAlerts_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllRoadObjectsObservers() }
    }

    @Test
    fun onDestroySetsRoutesToEmpty() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.routes = emptyList() }
    }

    @Test
    fun onDestroyCallsTripSessionStop() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.stop() }
    }

    @Test
    fun onDestroyCallsNativeNavigatorReset() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { navigator.resetRideSession() }
    }

    @Test
    fun unregisterAllBannerInstructionsObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllBannerInstructionsObservers() }
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllVoiceInstructionsObservers() }
    }

    @Test
    fun unregisterAllNavigationSessionStateObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { navigationSession.unregisterAllNavigationSessionStateObservers() }
    }

    @Test
    fun unregisterAllMapMatcherResultObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllMapMatcherResultObservers() }
    }

    @Test
    fun unregisterAllTelemetryObservers() {
        mockkObject(MapboxNavigationTelemetry)

        mapboxNavigation.onDestroy()

        verify(exactly = 1) { MapboxNavigationTelemetry.unregisterListeners(eq(mapboxNavigation)) }

        unmockkObject(MapboxNavigationTelemetry)
    }

    @Test
    fun unregisterAllTelemetryObserversIsCalledAfterTripSessionStop() {
        mockkObject(MapboxNavigationTelemetry)

        mapboxNavigation.onDestroy()

        verifyOrder {
            tripSession.stop()
            MapboxNavigationTelemetry.unregisterListeners(mapboxNavigation)
        }

        unmockkObject(MapboxNavigationTelemetry)
    }

    @Test
    fun fasterRoute_noRouteOptions_noRequest() {
        every { directionsSession.getPrimaryRouteOptions() } returns null
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun fasterRoute_noEnhancedLocation_noRequest() {
        every { tripSession.getEnhancedLocation() } returns null
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun arrival_controller_register() {
        val arrivalController: ArrivalController = mockk()

        mapboxNavigation.setArrivalController(arrivalController)

        verify { tripSession.registerRouteProgressObserver(any<ArrivalProgressObserver>()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun arrival_controller_unregister() {
        val arrivalController: ArrivalController? = null

        mapboxNavigation.setArrivalController(arrivalController)

        verify { tripSession.unregisterRouteProgressObserver(any<ArrivalProgressObserver>()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun offroute_lead_to_reroute() {
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun reRoute_not_called() {
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(false)

        verify(exactly = 0) { rerouteController.reroute(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun internalRouteObserver_notEmpty() {
        val primary: DirectionsRoute = mockk()
        val secondary: DirectionsRoute = mockk()
        val routes = listOf(primary, secondary)
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }

        routeObserversSlot.forEach {
            it.onRoutesChanged(routes)
        }

        verify { tripSession.route = primary }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun internalRouteObserver_empty() {
        val routes = emptyList<DirectionsRoute>()
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }

        routeObserversSlot.forEach {
            it.onRoutesChanged(routes)
        }

        verify { tripSession.route = null }
        mapboxNavigation.onDestroy()
    }

    @Test
    fun `don't interrupt faster route request on a standalone route request`() {
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk())

        verify(exactly = 0) { rerouteController.interrupt() }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun interrupt_reroute_on_set_routes() {
        mapboxNavigation.setRoutes(mockk())

        verify(exactly = 1) { rerouteController.interrupt() }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun interrupt_reroute_process_when_new_reroute_controller_has_been_set() {
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `road objects observer is registered in the trip session`() {
        val observer: RoadObjectsObserver = mockk()

        mapboxNavigation.registerRoadObjectsObserver(observer)

        verify(exactly = 1) { tripSession.registerRoadObjectsObserver(observer) }
    }

    @Test
    fun `road objects observer is unregistered in the trip session`() {
        val observer: RoadObjectsObserver = mockk()

        mapboxNavigation.unregisterRoadObjectsObserver(observer)

        verify(exactly = 1) { tripSession.unregisterRoadObjectsObserver(observer) }
    }

    @Test
    fun `resetTripSession should reset the navigator`() {
        mapboxNavigation.resetTripSession()

        verify { navigator.resetRideSession() }
    }

    @Test
    fun `verify tile config path`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(any(), any(), capture(slot), any())
        } returns navigator
        val options = navigationOptions.toBuilder()
            .routingTilesOptions(RoutingTilesOptions.Builder().build())
            .build()

        mapboxNavigation = MapboxNavigation(options)

        assertTrue(slot.captured.tilesPath.endsWith(RoutingTilesFiles.TILES_PATH_SUB_DIR))

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify tile config dataset`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<TilesConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(any(), any(), capture(slot), any())
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify incidents options null when no params set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(any(), capture(slot), any(), any())
        } returns navigator

        mapboxNavigation = MapboxNavigation(navigationOptions)

        assertNull(slot.captured.incidentsOptions)

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify incidents options non-null when graph set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(any(), capture(slot), any(), any())
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify incidents options non-null when apiUrl set`() {
        ThreadController.cancelAllUICoroutines()
        val slot = slot<NavigatorConfig>()
        every {
            NavigationComponentProvider.createNativeNavigator(any(), capture(slot), any(), any())
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `setRoute pushes the route to the directions session`() {
        val routes = listOf(mockk<DirectionsRoute>())

        mapboxNavigation.setRoutes(routes)

        verify(exactly = 1) { directionsSession.routes = routes }
    }

    @Test
    fun `requestRoutes pushes the request to the directions session`() {
        val options = mockk<RouteOptions>()
        val callback = mockk<RoutesRequestCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns 1L

        mapboxNavigation.requestRoutes(options, callback)
        verify(exactly = 1) { directionsSession.requestRoutes(options, callback) }
    }

    @Test
    fun `requestRoutes passes back the request id`() {
        val expected = 1L
        val options = mockk<RouteOptions>()
        val callback = mockk<RoutesRequestCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns expected

        val actual = mapboxNavigation.requestRoutes(options, callback)

        assertEquals(expected, actual)
    }

    @Test
    fun `requestRoutes doesn't pushes the route to the directions session automatically`() {
        val routes = listOf(mockk<DirectionsRoute>())
        val options = mockk<RouteOptions>()
        val possibleInternalCallbackSlot = slot<RoutesRequestCallback>()
        every { directionsSession.requestRoutes(options, any()) } returns 1L

        mapboxNavigation.requestRoutes(options, mockk(relaxUnitFun = true))
        verify { directionsSession.requestRoutes(options, capture(possibleInternalCallbackSlot)) }
        possibleInternalCallbackSlot.captured.onRoutesReady(routes)

        verify(exactly = 0) { directionsSession.routes = routes }
    }

    @Test
    fun `cancelRouteRequest pushes the data to directions session`() {
        mapboxNavigation.cancelRouteRequest(1L)

        verify(exactly = 1) { directionsSession.cancelRouteRequest(1L) }
    }

    @Test
    fun `directions session is shutdown onDestroy`() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.shutdown() }
    }

    private fun mockLocation() {
        every { location.longitude } returns -122.789876
        every { location.latitude } returns 37.657483
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
    }

    private fun mockNativeNavigator() {
        every {
            NavigationComponentProvider.createNativeNavigator(any(), any(), any(), any())
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
            NavigationComponentProvider.createTripSession(
                tripService,
                navigationOptions = navigationOptions,
                navigator = navigator,
                logger = logger,
                accessToken = "pk.1234"
            )
        } returns tripSession
        every { tripSession.getEnhancedLocation() } returns location
        every { tripSession.getRouteProgress() } returns routeProgress
        every { tripSession.getRawLocation() } returns location
    }

    private fun mockDirectionSession() {
        every { NavigationComponentProvider.createDirectionsSession(any(), any()) } answers {
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

    private fun provideDefaultRouteOptionsBuilder() =
        RouteOptions.builder()
            .accessToken(accessToken)
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinates(emptyList())
            .geometries("")
            .requestUuid("")

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
