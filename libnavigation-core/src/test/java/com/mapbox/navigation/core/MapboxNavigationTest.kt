package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.location.Location
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
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
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
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
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
    private val location: Location = mockk(relaxUnitFun = true)
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    private val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    private val routeRefreshController: RouteRefreshController = mockk(relaxUnitFun = true)
    private val routeAlternativesController: RouteAlternativesController =
        mockk(relaxUnitFun = true)
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val navigationSession: NavigationSession = mockk(relaxed = true)
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
        every { navigator.getHistoryRecorderHandle() } returns null
        every { navigator.experimental } returns mockk()
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

        mockLocation()
        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()

        every { navigator.create(any(), any(), any(), any(), any()) } returns navigator

        mapboxNavigation = MapboxNavigation(navigationOptions)

        rerouteController = mockk(relaxUnitFun = true)
        mapboxNavigation.setRerouteController(rerouteController)
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxSDKCommon)
        unmockkObject(MapboxModuleProvider)
        unmockkObject(LoggerProvider)
        unmockkObject(NavigationComponentProvider)
        unmockkObject(RouteRefreshControllerProvider)
        unmockkObject(RouteAlternativesControllerProvider)

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
    fun `restart the routeRefreshController during initialization`() {
        ThreadController.cancelAllUICoroutines()
        verify(exactly = 1) { routeRefreshController.restart() }
    }

    @Test
    fun `restart the routeRefreshController if new route is set`() {
        ThreadController.cancelAllUICoroutines()

        mapboxNavigation = MapboxNavigation(
            provideNavigationOptions()
                .routeRefreshOptions(
                    RouteRefreshOptions.Builder()
                        .build()
                )
                .build()
        )
        val route: DirectionsRoute = mockk()
        val routeOptions: RouteOptions = mockk()
        every { route.routeOptions() } returns routeOptions
        every { route.geometry() } returns "geometry"
        every { route.legs() } returns emptyList()
        every { routeOptions.overview() } returns "full"
        every { routeOptions.annotationsList() } returns emptyList()
        every { routeOptions.enableRefresh() } returns true
        mapboxNavigation.setRoutes(listOf(route))

        // the sequence needs to be kept because routeRefreshController uses the route stored
        // in the trip session, which has to be set first via directionsSession
        verifyOrder {
            directionsSession.routes = listOf(route)
            routeRefreshController.restart()
        }
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

        verify(exactly = 1) { tripSession.unregisterAllRoadObjectsOnRouteObservers() }
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
    fun routeAlternatives_noRouteOptions_noRequest() {
        every { directionsSession.getPrimaryRouteOptions() } returns null
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun routeAlternatives_noEnhancedLocation_noRequest() {
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
    fun `don't interrupt reroute requests on a standalone route request`() {
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk())

        verify(exactly = 0) { rerouteController.interrupt() }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun interrupt_reroute_on_set_routes() {
        mapboxNavigation.setRoutes(listOf())

        verify(exactly = 1) { rerouteController.interrupt() }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `don't interrupt route alternatives on a standalone route request`() {
        every { directionsSession.requestRoutes(any(), any()) } returns 1L
        mapboxNavigation.requestRoutes(mockk(), mockk())

        verify(exactly = 0) { routeAlternativesController.interrupt() }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `interrupt route alternatives on set route`() {
        mapboxNavigation.setRoutes(listOf())

        verify(exactly = 1) { routeAlternativesController.interrupt() }

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
        val observer: RoadObjectsOnRouteObserver = mockk()

        mapboxNavigation.registerRoadObjectsOnRouteObserver(observer)

        verify(exactly = 1) { tripSession.registerRoadObjectsOnRouteObserver(observer) }
    }

    @Test
    fun `road objects observer is unregistered in the trip session`() {
        val observer: RoadObjectsOnRouteObserver = mockk()

        mapboxNavigation.unregisterRoadObjectsOnRouteObserver(observer)

        verify(exactly = 1) { tripSession.unregisterRoadObjectsOnRouteObserver(observer) }
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
            NavigationComponentProvider.createNativeNavigator(
                any(), any(), capture(slot), any(), any()
            )
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

        mapboxNavigation.onDestroy()
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

        mapboxNavigation.onDestroy()
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

        mapboxNavigation.onDestroy()
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `setRoute pushes the route to the directions session`() {
        val route: DirectionsRoute = mockk()
        val routeOptions: RouteOptions = mockk()
        every { route.routeOptions() } returns routeOptions
        every { route.geometry() } returns "geometry"
        every { route.legs() } returns emptyList()
        every { routeOptions.overview() } returns "full"
        every { routeOptions.annotationsList() } returns emptyList()

        val routes = listOf(route)

        mapboxNavigation.setRoutes(routes)

        verify(exactly = 1) { directionsSession.routes = routes }
    }

    @Test
    fun `requestRoutes pushes the request to the directions session`() {
        val options = mockk<RouteOptions>()
        val callback = mockk<RouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns 1L

        mapboxNavigation.requestRoutes(options, callback)
        verify(exactly = 1) { directionsSession.requestRoutes(options, callback) }
    }

    @Test
    fun `requestRoutes passes back the request id`() {
        val expected = 1L
        val options = mockk<RouteOptions>()
        val callback = mockk<RouterCallback>()
        every { directionsSession.requestRoutes(options, callback) } returns expected

        val actual = mapboxNavigation.requestRoutes(options, callback)

        assertEquals(expected, actual)
    }

    @Test
    fun `requestRoutes doesn't pushes the route to the directions session automatically`() {
        val routes = listOf(mockk<DirectionsRoute>())
        val options = mockk<RouteOptions>()
        val possibleInternalCallbackSlot = slot<RouterCallback>()
        val origin = mockk<RouterOrigin>()
        every { directionsSession.requestRoutes(options, any()) } returns 1L

        mapboxNavigation.requestRoutes(options, mockk(relaxUnitFun = true))
        verify { directionsSession.requestRoutes(options, capture(possibleInternalCallbackSlot)) }
        possibleInternalCallbackSlot.captured.onRoutesReady(routes, origin)

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

    @Test
    fun `register internalFallbackVersionsObserver`() {
        verify(exactly = 1) { tripSession.registerFallbackVersionsObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `unregisterAllFallbackVersionsObservers on destroy`() {
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on fallback`() {
        ThreadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        every { tripSession.route } returns null
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

        mapboxNavigation.onDestroy()
    }

    @Test
    fun `verify tile config tilesVersion and isFallback on return to latest tiles version`() {
        ThreadController.cancelAllUICoroutines()

        val fallbackObserverSlot = slot<FallbackVersionsObserver>()
        every {
            tripSession.registerFallbackVersionsObserver(capture(fallbackObserverSlot))
        } just Runs
        every { tripSession.route } returns null
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

        mapboxNavigation.onDestroy()
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
        every { tripSession.route } returns route
        every { tripSession.getRouteProgress() } returns routeProgress
        every { routeProgress.currentLegProgress } returns legProgress
        every { legProgress.legIndex } returns index
        coEvery { navigator.setRoute(any(), any()) } returns mockk()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        fallbackObserverSlot.captured.onFallbackVersionsFound(listOf("version"))

        coVerify {
            navigator.setRoute(route, index)
        }

        mapboxNavigation.onDestroy()
    }

    private fun mockLocation() {
        every { location.longitude } returns -122.789876
        every { location.latitude } returns 37.657483
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
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
            NavigationComponentProvider.createTripSession(
                tripService,
                navigationOptions = navigationOptions,
                navigator = navigator,
                logger = logger,
            )
        } returns tripSession
        every { tripSession.getEnhancedLocation() } returns location
        every { tripSession.getRouteProgress() } returns routeProgress
        every { tripSession.getRawLocation() } returns location
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
