package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.common.MapboxSDKCommon
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.preview.RoutesPreviewController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.NavigationTelemetry
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.core.trip.session.createSetRouteResult
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.CacheHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import java.io.File
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal open class MapboxNavigationBaseTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    val accessToken = "pk.1234"
    val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    val cache: CacheHandle = mockk(relaxUnitFun = true)
    val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true) {
        every { experimental } returns mockk()
    }
    val navigationTelemetry: NavigationTelemetry = mockk(relaxUnitFun = true)
    val tripService: TripService = mockk(relaxUnitFun = true)
    val tripSession: TripSession = mockk(relaxUnitFun = true)
    val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    val routeRefreshController: RouteRefreshController = mockk(relaxed = true)
    val evDynamicDataHolder: EVDynamicDataHolder = mockk(relaxed = true)
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
    val historyRecordingStateHandler: HistoryRecordingStateHandler = mockk(relaxed = true)
    val developerMetadataAggregator: DeveloperMetadataAggregator = mockk(relaxUnitFun = true)
    val threadController = mockk<ThreadController>(relaxed = true)
    val routeProgressDataProvider = mockk<RoutesProgressDataProvider>(relaxed = true)
    val routesPreviewController = mockk<RoutesPreviewController>(relaxed = true)
    val routesCacheClearer = mockk<RoutesCacheClearer>(relaxed = true)

    val applicationContext: Application = mockk(relaxed = true) {
        every { inferDeviceLocale() } returns Locale.US
        every {
            getSystemService(Context.NOTIFICATION_SERVICE)
        } returns mockk<NotificationManager>()
        every { getSystemService(Context.ALARM_SERVICE) } returns mockk<AlarmManager>()
        every { packageManager } returns mockk(relaxed = true)
        every { packageName } returns "com.mapbox.navigation.core.MapboxNavigationTest"
        every { filesDir } returns File("some/path")
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
        every { threadController.getMainScopeAndRootJob() } answers {
            JobControl(mockk(), coroutineRule.createTestScope())
        }
        mockkObject(LoggerProvider)
        mockkObject(NavigatorLoader)
        every {
            NavigatorLoader.createNativeRouterInterface(any(), any(), any())
        } returns mockk()
        every { NavigatorLoader.createConfig(any(), any()) } returns mockk()
        every {
            NavigatorLoader.createHistoryRecorderHandles(
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

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
        mockkObject(CacheHandleWrapper)
        every { CacheHandleWrapper.requestRoadGraphDataUpdate(any(), any()) } just runs
        mockkObject(RouteRefreshControllerProvider)
        every {
            RouteRefreshControllerProvider.createRouteRefreshController(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns routeRefreshController
        every {
            NavigationComponentProvider.createEVDynamicDataHolder()
        } returns evDynamicDataHolder
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
        every {
            NavigationComponentProvider.createHistoryRecordingStateHandler()
        } returns historyRecordingStateHandler
        every {
            NavigationComponentProvider.createDeveloperMetadataAggregator(any())
        } returns developerMetadataAggregator
        every {
            NavigationComponentProvider.createRouteRefreshRequestDataProvider()
        } returns routeProgressDataProvider
        every {
            NavigationComponentProvider.createRoutesPreviewController(any())
        } returns routesPreviewController
        every { NavigationComponentProvider.createRoutesCacheClearer() } returns routesCacheClearer

        every {
            navigator.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns navigator
        mockkObject(TelemetryUtilsDelegate)
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true
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
        unmockkObject(CacheHandleWrapper)
        unmockkObject(RouteRefreshControllerProvider)
        unmockkObject(RouteAlternativesControllerProvider)
        unmockkObject(NavigationTelemetry.Companion)
        unmockkObject(EventsServiceProvider)
        unmockkObject(TelemetryServiceProvider)
        unmockkObject(TelemetryUtilsDelegate)
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
        coEvery { navigator.setRoutes(any(), any(), any(), any()) } answers {
            createSetRouteResult()
        }
        every { navigator.cache } returns cache
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
        coEvery { tripSession.setRoutes(any(), any()) } answers {
            NativeSetRouteValue(
                routes = firstArg(),
                nativeAlternatives = emptyList()
            )
        }
    }

    private fun mockDirectionSession() {
        every { NavigationComponentProvider.createDirectionsSession(any()) } answers {
            directionsSession
        }
        // TODO Needed for telemetry - Free Drive (empty list) for now
        every { directionsSession.routesUpdatedResult } returns createRoutesUpdatedResult(
            emptyList(),
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
        )
        every { directionsSession.routes } returns emptyList()
    }

    private fun mockNavigationSession() {
        every { NavigationComponentProvider.createNavigationSession() } answers {
            navigationSession
        }
        every { navigationSession.state } returns NavigationSessionState.Idle
    }

    private fun mockNavTelemetry() {
        mockkObject(EventsServiceProvider)
        every {
            EventsServiceProvider.provideEventsService(any())
        } returns mockk(relaxUnitFun = true)

        mockkObject(TelemetryServiceProvider)
        every {
            TelemetryServiceProvider.provideTelemetryService(any())
        } returns mockk(relaxUnitFun = true)

        mockkObject(NavigationTelemetry.Companion)
        every {
            NavigationTelemetry(any(), any())
        } returns navigationTelemetry
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
