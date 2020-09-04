package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.OnboardRouterOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteAlertsObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val accessToken = "pk.1234"
    private val locationEngine: LocationEngine = mockk()
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val distanceFormatter: MapboxDistanceFormatter = mockk(relaxed = true)
    private val onBoardRouterOptions: OnboardRouterOptions = mockk(relaxed = true)
    private val fasterRouteRequestCallback: RoutesRequestCallback = mockk(relaxed = true)
    private val routeOptions: RouteOptions = provideDefaultRouteOptionsBuilder().build()
    private val routes: List<DirectionsRoute> = listOf(mockk())
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val navigationSession: NavigationSession = mockk(relaxUnitFun = true)
    private val logger: Logger = mockk(relaxUnitFun = true)
    private lateinit var rerouteController: RerouteController

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

        mockkObject(NavigationComponentProvider)

        every { applicationContext.applicationContext } returns applicationContext

        mockLocation()
        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()

        every { navigator.create(any(), any(), any()) } returns navigator

        val navigationOptions = NavigationOptions
            .Builder(applicationContext)
            .accessToken(accessToken)
            .distanceFormatter(distanceFormatter)
            .navigatorPredictionMillis(1500L)
            .onboardRouterOptions(onBoardRouterOptions)
            .timeFormatType(NONE_SPECIFIED)
            .locationEngine(locationEngine)
            .build()

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
        val navigationOptions = NavigationOptions
            .Builder(applicationContext)
            .accessToken(accessToken)
            .distanceFormatter(distanceFormatter)
            .navigatorPredictionMillis(1500L)
            .onboardRouterOptions(onBoardRouterOptions)
            .timeFormatType(NONE_SPECIFIED)
            .locationEngine(locationEngine)
            .build()

        mapboxNavigation = MapboxNavigation(navigationOptions)

        verify(exactly = 4) { tripSession.registerOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun destroy_unregisterOffRouteObserver_MapboxNavigation_recreated() {
        ThreadController.cancelAllUICoroutines()
        val navigationOptions = NavigationOptions
            .Builder(applicationContext)
            .accessToken(accessToken)
            .distanceFormatter(distanceFormatter)
            .navigatorPredictionMillis(1500L)
            .onboardRouterOptions(onBoardRouterOptions)
            .timeFormatType(NONE_SPECIFIED)
            .locationEngine(locationEngine)
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions)

        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterOffRouteObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerStateObserver_navigationSession() {
        verify(exactly = 1) { tripSession.registerStateObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerNavigationSessionStateObserver() {
        verify(exactly = 2) { navigationSession.registerNavigationSessionStateObserver(any()) }

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

        verify(exactly = 1) { tripSession.unregisterAllRouteAlertsObservers() }
    }

    @Test
    fun onDestroySetsRouteToNullInTripSession() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.route = null }
    }

    @Test
    fun onDestroyCallsTripSessionStop() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.stop() }
    }

    @Test
    fun onDestroyCallsNativeNavigatorReset() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { navigator.create(any(), any(), any()) }
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
    fun fasterRoute_noRouteOptions_noRequest() {
        every { directionsSession.getRouteOptions() } returns null
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun fasterRoute_noEnhancedLocation_noRequest() {
        every { tripSession.getEnhancedLocation() } returns null
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }

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
    fun interrupt_reroute_on_route_request() {
        mapboxNavigation.requestRoutes(mockk())

        verify(exactly = 1) { rerouteController.interrupt() }

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
    fun `route alerts observer is registered in the trip session`() {
        val observer: RouteAlertsObserver = mockk()

        mapboxNavigation.registerRouteAlertsObserver(observer)

        verify(exactly = 1) { tripSession.registerRouteAlertsObserver(observer) }
    }

    @Test
    fun `route alerts observer is unregistered in the trip session`() {
        val observer: RouteAlertsObserver = mockk()

        mapboxNavigation.unregisterRouteAlertsObserver(observer)

        verify(exactly = 1) { tripSession.unregisterRouteAlertsObserver(observer) }
    }

    private fun mockLocation() {
        every { location.longitude } returns -122.789876
        every { location.latitude } returns 37.657483
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
    }

    private fun mockNativeNavigator() {
        every {
            NavigationComponentProvider.createNativeNavigator(any(), any(), any())
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
                locationEngine,
                any(),
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
        every { NavigationComponentProvider.createDirectionsSession(any()) } answers {
            directionsSession
        }
        every { directionsSession.getRouteOptions() } returns routeOptions
        every { directionsSession.requestFasterRoute(any(), any()) } answers {
            fasterRouteRequestCallback.onRoutesReady(routes)
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
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_DRIVING)
            .coordinates(emptyList())
            .geometries("")
            .requestUuid("")
}
