package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.internal.RouteUrl
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteDetector
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.module.NavigationModuleProvider
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.navigation.utils.timer.MapboxTimer
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
import org.junit.Ignore
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTest {

    private val accessToken = "pk.1234"
    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk(relaxUnitFun = true)
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val distanceFormatter: MapboxDistanceFormatter = mockk(relaxed = true)
    private val onBoardRouterConfig: MapboxOnboardRouterConfig = mockk(relaxed = true)
    private val fasterRouteRequestCallback: RoutesRequestCallback = mockk(relaxed = true)
    private val routeOptions: RouteOptions = provideDefaultRouteOptionsBuilder().build()
    private val fasterRouteObserver: FasterRouteObserver = mockk(relaxUnitFun = true)
    private val mapboxTimer: MapboxTimer = mockk(relaxUnitFun = true)
    private val routes: List<DirectionsRoute> = listOf(mockk())
    private val routeProgress: RouteProgress = mockk(relaxed = true)

    private lateinit var delayLambda: () -> Unit
    private lateinit var mapboxNavigation: MapboxNavigation

    companion object {
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
        private const val DEFAULT_REROUTE_BEARING_ANGLE = 11f

        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.utils.extensions.ContextEx")
        }
    }

    @Before
    fun setUp() {
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        mockkObject(NavigationModuleProvider)
        val hybridRouter: Router = mockk(relaxUnitFun = true)
        every {
            NavigationModuleProvider.createModule<Router>(
                MapboxNavigationModuleType.HybridRouter,
                any()
            )
        } returns hybridRouter

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
        mockMapboxTimer()
        mockTripService()
        mockTripSession()
        mockDirectionSession()

        val navigationOptions = NavigationOptions
            .Builder()
            .distanceFormatter(distanceFormatter)
            .fasterRouteDetectorInterval(1000L)
            .navigatorPollingDelay(1500L)
            .onboardRouterConfig(onBoardRouterConfig)
            .roundingIncrement(1)
            .timeFormatType(NONE_SPECIFIED)
            .build()

        mapboxNavigation =
            MapboxNavigation(
                context,
                accessToken,
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
    fun fasterRoute_timerStarted() {
        mapboxNavigation.registerFasterRouteObserver(fasterRouteObserver)
        verify { mapboxTimer.start() }
    }

    @Test
    fun fasterRoute_timerStopped() {
        mapboxNavigation.registerFasterRouteObserver(fasterRouteObserver)
        mapboxNavigation.unregisterFasterRouteObserver(fasterRouteObserver)
        verify { mapboxTimer.stop() }
    }

    @Test
    fun fasterRoute_noRouteOptions_noRequest() {
        every { directionsSession.getRouteOptions() } returns null
        delayLambda()
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_noEnhancedLocation_noRequest() {
        every { tripSession.getEnhancedLocation() } returns null
        delayLambda()
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_makeRequest() {
        delayLambda()
        verify(exactly = 1) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_fasterRouteNotAvailable() {
        mockkObject(FasterRouteDetector)
        every { FasterRouteDetector.isRouteFaster(any(), any()) } returns false
        mapboxNavigation.registerFasterRouteObserver(fasterRouteObserver)
        delayLambda()
        verify(exactly = 0) { fasterRouteObserver.onFasterRouteAvailable(routes[0]) }
    }

    @Test
    fun reRoute_called() {
        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(true)

        verify(exactly = 1) { directionsSession.requestRoutes(any(), any()) }
    }

    // TODO Fix test not working because of MapboxNavigationTelemetry#unregisterListeners initializer = initializerDelegate
    @Ignore
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

    // TODO Fix test not working because of MapboxNavigationTelemetry#unregisterListeners initializer = initializerDelegate
    @Ignore
    @Test
    fun reRoute_called_with_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        every { directionsSession.getRouteOptions() } returns routeOptions

        val offRouteObserverSlot = slot<OffRouteObserver>()
        verify { tripSession.registerOffRouteObserver(capture(offRouteObserverSlot)) }

        offRouteObserverSlot.captured.onOffRouteStateChanged(true)

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

    private fun mockTripService() {
        every {
            NavigationComponentProvider.createTripService(
                applicationContext,
                any()
            )
        } returns tripService
    }

    private fun mockMapboxTimer() {
        val lambda = slot<() -> Unit>()
        every { NavigationComponentProvider.createMapboxTimer(1000L, capture(lambda)) } answers {
            delayLambda = lambda.captured
            mapboxTimer
        }
    }

    private fun mockLocation() {
        every { location.longitude } returns -122.789876
        every { location.latitude } returns 37.657483
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
    }

    private fun mockDirectionSession() {
        every { NavigationComponentProvider.createDirectionsSession(any()) } answers {
            directionsSession
        }
        every { directionsSession.getRouteOptions() } returns routeOptions
        every { directionsSession.requestFasterRoute(any(), any()) } answers {
            fasterRouteRequestCallback.onRoutesReady(routes)
        }
    }

    private fun mockTripSession() {
        every {
            NavigationComponentProvider.createTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                any()
            )
        } returns tripSession
        every { tripSession.getEnhancedLocation() } returns location
        every { tripSession.getRouteProgress() } returns routeProgress
        every { tripSession.getRawLocation() } returns location
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
        unmockkObject(NavigationModuleProvider)
        unmockkObject(NavigationComponentProvider)
    }
}
