package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import com.mapbox.android.telemetry.TelemetryEnabler
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxSDKCommon
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.NativeSetRouteResult
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.core.trip.session.createSetRouteResult
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterOrigin
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import java.io.File
import java.util.Locale

internal open class MapboxNavigationBaseTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    val accessToken = "pk.1234"
    val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    val tripService: TripService = mockk(relaxUnitFun = true)
    val tripSession: TripSession = mockk(relaxUnitFun = true)
    val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    val routeRefreshController: RouteRefreshController = mockk(relaxed = true)
    val routeAlternativesController: RouteAlternativesController = mockk(relaxed = true)
    val routeProgress: RouteProgress = mockk(relaxed = true)
    val navigationSession: NavigationSession = mockk(relaxed = true)
    val billingController: BillingController = mockk(relaxUnitFun = true)
    val rerouteController: RerouteController = mockk(relaxUnitFun = true) {
        every { state } returns RerouteState.Idle
    }
    val tripSessionLocationEngine: TripSessionLocationEngine = mockk(relaxUnitFun = true)
    lateinit var navigationOptions: NavigationOptions
    val arrivalProgressObserver: ArrivalProgressObserver = mockk(relaxUnitFun = true)
    val threadController = ThreadController()

    val applicationContext: Context = mockk(relaxed = true) {
        every { inferDeviceLocale() } returns Locale.US
        every {
            getSystemService(Context.NOTIFICATION_SERVICE)
        } returns mockk<NotificationManager>()
        every { getSystemService(Context.ALARM_SERVICE) } returns mockk<AlarmManager>()
        every {
            getSharedPreferences(
                MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES,
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

    lateinit var mapboxNavigation: MapboxNavigation

    companion object {
        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        }
    }

    @Before
    open fun setUp() {
        mockkObject(LoggerProvider)
        every { LoggerProvider.initialize() } just Runs
        mockkObject(NavigatorLoader)
        every {
            NavigatorLoader.createNativeRouterInterface(any(), any(), any(), any())
        } returns mockk()

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
                any(), any(), any(),
            )
        } returns routeRefreshController
        mockkObject(RouteAlternativesControllerProvider)
        every {
            RouteAlternativesControllerProvider.create(any(), any(), any(), any())
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

        every { navigator.create(any(), any(), any(), any(), any(), any()) } returns navigator
        mockkStatic(TelemetryEnabler::class)
        every { TelemetryEnabler.isEventsEnabled(any()) } returns true

        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeId } returns "$it"
                            every { routerOrigin } returns RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    open fun tearDown() {
        if (this::mapboxNavigation.isInitialized) {
            mapboxNavigation.onDestroy()
        }

        unmockkObject(LoggerProvider)
        unmockkObject(NavigatorLoader)
        unmockkObject(MapboxSDKCommon)
        unmockkObject(MapboxModuleProvider)
        unmockkObject(NavigationComponentProvider)
        unmockkObject(RouteRefreshControllerProvider)
        unmockkObject(RouteAlternativesControllerProvider)
        unmockkObject(MapboxNavigationTelemetry)
        unmockkStatic(TelemetryEnabler::class)
        unmockkObject(NativeRouteParserWrapper)

        threadController.cancelAllNonUICoroutines()
        threadController.cancelAllUICoroutines()
    }

    fun createMapboxNavigation() {
        mapboxNavigation = MapboxNavigation(navigationOptions, threadController)
    }

    private fun mockNativeNavigator() {
        every {
            NavigationComponentProvider.createNativeNavigator(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns navigator
        coEvery { navigator.setRoutes(any(), any(), any()) } answers {
            createSetRouteResult()
        }
    }

    private fun mockTripService() {
        every {
            NavigationComponentProvider.createTripService(
                applicationContext,
                any(),
                threadController,
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
                threadController,
            )
        } returns tripSession
        every { tripSession.getRouteProgress() } returns routeProgress
        coEvery { tripSession.setRoutes(any(), any(), any()) } returns NativeSetRouteResult(
            nativeAlternatives = emptyList()
        )
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
        every { MapboxNavigationTelemetry.initialize(any(), any(), any(), any()) } just runs
        every { MapboxNavigationTelemetry.destroy(any()) } just runs
        every {
            MapboxNavigationTelemetry.postUserFeedback(
                any(), any(), any(), any(), any(), any(), any(),
            )
        } just runs
    }

    fun provideNavigationOptions() =
        NavigationOptions
            .Builder(applicationContext)
            .accessToken(accessToken)
            .distanceFormatterOptions(distanceFormatterOptions)
            .navigatorPredictionMillis(1500L)
            .routingTilesOptions(routingTilesOptions)
            .timeFormatType(TimeFormat.NONE_SPECIFIED)
            .locationEngine(locationEngine)
}
