package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.internal.RouteUrl
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter
import com.mapbox.navigation.core.internal.trip.service.TripService
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val accessToken = "pk.1234"
    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk(relaxUnitFun = true)
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val distanceFormatter: MapboxDistanceFormatter = mockk(relaxed = true)
    private val onBoardRouterConfig: MapboxOnboardRouterConfig = mockk(relaxed = true)
    private val fasterRouteRequestCallback: RoutesRequestCallback = mockk(relaxed = true)
    private val routeOptions: RouteOptions = provideDefaultRouteOptionsBuilder().build()
    private val routes: List<DirectionsRoute> = listOf(mockk())
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val navigationSession: NavigationSession = mockk(relaxUnitFun = true)
    private val logger: Logger = mockk(relaxUnitFun = true)

    private lateinit var mapboxNavigation: MapboxNavigation

    companion object {
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
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

        every { context.inferDeviceLocale() } returns Locale.US
        every { context.applicationContext } returns applicationContext
        val notificationManager = mockk<NotificationManager>()
        every { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        val alarmManager = mockk<AlarmManager>()
        every { applicationContext.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            applicationContext.getSharedPreferences(
                MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
            )
        } returns sharedPreferences
        every { sharedPreferences.getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"

        mockLocation()
        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()

        val navigationOptions = NavigationOptions
            .Builder()
            .accessToken(accessToken)
            .distanceFormatter(distanceFormatter)
            .navigatorPredictionMillis(1500L)
            .onboardRouterConfig(onBoardRouterConfig)
            .timeFormatType(NONE_SPECIFIED)
            .build()

        mapboxNavigation =
            MapboxNavigation(
                context,
                navigationOptions,
                locationEngine,
                locationEngineRequest
            )
    }

    @Test
    fun sanity() {
        assertNotNull(mapboxNavigation)
    }

    @Test
    fun init_registerRoutesObservers_internalRoutesObserver_navigationSession_and_TelemetryLocationAndProgressDispatcher() {
        verify(exactly = 3) { directionsSession.registerRoutesObserver(any()) }

        mapboxNavigation.onDestroy()
    }

    @Test
    fun init_registerOffRouteObserver_internalOffRouteObserver() {
        verify(exactly = 1) { tripSession.registerOffRouteObserver(any()) }

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

        verify(exactly = 1) { navigator.reset() }
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
    }

    @Test
    fun fasterRoute_noEnhancedLocation_noRequest() {
        every { tripSession.getEnhancedLocation() } returns null
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun reRoute_called() {
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(true)

        verify(exactly = 1) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun getEnhancedLocation_reRoute() {
        val observers = mutableListOf<OffRouteObserver>()
        verify(exactly = 2) { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }

        verify(exactly = 1) { tripSession.getEnhancedLocation() }
        verify(exactly = 0) { tripSession.getRawLocation() }
    }

    @Test
    fun enhanced_location_used_for_reroute() {
        val observers = mutableListOf<OffRouteObserver>()
        verify(exactly = 2) { tripSession.registerOffRouteObserver(capture(observers)) }
        mockkObject(AdjustedRouteOptionsProvider)
        val mockedLocation = Location("mock")
        every { tripSession.getEnhancedLocation() } returns mockedLocation

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }

        verify(exactly = 1) { AdjustedRouteOptionsProvider.getRouteOptions(eq(directionsSession), eq(tripSession), eq(mockedLocation)) }

        unmockkObject(AdjustedRouteOptionsProvider)
    }

    @Test
    fun reRoute_called_with_null_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        every { directionsSession.getRouteOptions() } returns routeOptions

        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(true)

        val optionsSlot = slot<RouteOptions>()
        verify(exactly = 1) { directionsSession.requestRoutes(capture(optionsSlot), any()) }

        val expectedBearings = listOf(
            listOf(DEFAULT_REROUTE_BEARING_ANGLE.toDouble(), DEFAULT_REROUTE_BEARING_TOLERANCE),
            null,
            null,
            null
        )
        val actualBearings = optionsSlot.captured.bearingsList()

        assertEquals(expectedBearings, actualBearings)
    }

    @Test
    fun reRoute_called_with_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        every { directionsSession.getRouteOptions() } returns routeOptions

        val observers = mutableListOf<OffRouteObserver>()
        verify(exactly = 2) { tripSession.registerOffRouteObserver(capture(observers)) }

        observers.forEach {
            it.onOffRouteStateChanged(true)
        }

        val optionsSlot = slot<RouteOptions>()
        verify(exactly = 1) { directionsSession.requestRoutes(capture(optionsSlot), any()) }

        val expectedBearings = listOf(
            listOf(DEFAULT_REROUTE_BEARING_ANGLE.toDouble(), 10.0),
            listOf(20.0, 20.0),
            listOf(30.0, 30.0),
            listOf(40.0, 40.0)
        )
        val actualBearings = optionsSlot.captured.bearingsList()

        assertEquals(expectedBearings, actualBearings)
    }

    @Test
    fun reRoute_not_called() {
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(false)

        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun internalRouteObserver_notEmpty() {
        val primary: DirectionsRoute = mockk()
        val secondary: DirectionsRoute = mockk()
        val routes = listOf(primary, secondary)
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }
        routeObserversSlot[0].onRoutesChanged(routes)

        verify { tripSession.route = primary }
    }

    @Test
    fun internalRouteObserver_empty() {
        val routes = emptyList<DirectionsRoute>()
        val routeObserversSlot = mutableListOf<RoutesObserver>()
        verify { directionsSession.registerRoutesObserver(capture(routeObserversSlot)) }
        routeObserversSlot[0].onRoutesChanged(routes)

        verify { tripSession.route = null }
    }

    private fun mockLocation() {
        every { location.longitude } returns -122.789876
        every { location.latitude } returns 37.657483
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
    }

    private fun mockNativeNavigator() {
        every {
            NavigationComponentProvider.createNativeNavigator()
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
                locationEngineRequest,
                any(),
                navigator = navigator,
                logger = logger
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

    private fun provideRouteOptionsWithCoordinates() =
        provideDefaultRouteOptionsBuilder()
            .coordinates(
                listOf(
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0)
                )
            )
            .build()

    private fun provideRouteOptionsWithCoordinatesAndBearings() =
        provideRouteOptionsWithCoordinates()
            .toBuilder()
            .bearingsList(
                listOf(
                    listOf(10.0, 10.0),
                    listOf(20.0, 20.0),
                    listOf(30.0, 30.0),
                    listOf(40.0, 40.0),
                    listOf(50.0, 50.0),
                    listOf(60.0, 60.0)
                )
            )
            .build()

    @After
    fun tearDown() {
        unmockkObject(MapboxModuleProvider)
        unmockkObject(NavigationComponentProvider)

        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
    }
}
