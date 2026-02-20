package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxOptions
import com.mapbox.common.MapboxSDKCommon
import com.mapbox.common.TileStore
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.clearCache
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.internal.tilestore.NavigationTileStoreOwner
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.internal.router.RouterWrapper
import com.mapbox.navigation.core.mapmatching.MapMatchingAPI
import com.mapbox.navigation.core.mapmatching.MapMatchingAPIProvider
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.preview.RoutesPreviewController
import com.mapbox.navigation.core.reroute.InternalRerouteController
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
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.core.utils.PermissionsChecker
import com.mapbox.navigation.core.utils.SystemLocaleWatcher
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
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.Telemetry
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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

    val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    val cache: CacheHandle = mockk(relaxUnitFun = true)
    val telemetry: Telemetry = mockk(relaxUnitFun = true)
    val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    val router: RouterInterface = mockk(relaxUnitFun = true)
    val tripService: TripService = mockk(relaxUnitFun = true)
    val tripSession: TripSession = mockk(relaxed = true)
    val locationOptions: LocationOptions = mockk(relaxUnitFun = true)
    val distanceFormatterOptions: DistanceFormatterOptions = mockk(relaxed = true)
    val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    val routeRefreshController: RouteRefreshController = mockk(relaxed = true)
    val evDynamicDataHolder: EVDynamicDataHolder = mockk(relaxed = true)
    val routeAlternativesController: RouteAlternativesController = mockk(relaxed = true)
    val mapMatchingAPI: MapMatchingAPI = mockk(relaxed = true)
    val routeProgress: RouteProgress = mockk(relaxed = true)
    val navigationSession: NavigationSession = mockk(relaxed = true)
    val billingController: BillingController = mockk(relaxUnitFun = true)
    val defaultRerouteController = mockk<InternalRerouteController>(relaxed = true)
    val tripSessionLocationEngine: TripSessionLocationEngine = mockk(relaxUnitFun = true)
    val navigationOptions: NavigationOptions = mockk(relaxed = true) {
        every { rerouteOptions } returns RerouteOptions.Builder().build()
    }
    val arrivalProgressObserver: ArrivalProgressObserver = mockk(relaxUnitFun = true)
    val historyRecordingStateHandler: HistoryRecordingStateHandler = mockk(relaxed = true)
    val developerMetadataAggregator: DeveloperMetadataAggregator = mockk(relaxUnitFun = true)
    val threadController = mockk<ThreadController>(relaxed = true)
    val routeProgressDataProvider = mockk<RoutesProgressDataProvider>(relaxed = true)
    val routesPreviewController = mockk<RoutesPreviewController>(relaxed = true)
    val routesCacheClearer = mockk<RoutesCacheClearer>(relaxed = true)
    val manualHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    val copilotHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    val compositeHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    val compositeHandle = mockk<HistoryRecorderHandle>(relaxed = true)
    val permissionsChecker = mockk<PermissionsChecker>(relaxed = true)
    val lowMemoryManager = mockk<LowMemoryManager>(relaxed = true)
    val eHorizonSubscriptionManager: EHorizonSubscriptionManager = mockk(relaxed = true)

    val navigationTelemetry = mockk<NavigationTelemetry>(relaxed = true)

    val applicationContext: Application = mockk(relaxed = true) {
        every { inferDeviceLocale() } returns Locale.US
        every {
            getSystemService(Context.NOTIFICATION_SERVICE)
        } returns mockk<NotificationManager>()
        every { getSystemService(Context.ALARM_SERVICE) } returns mockk<AlarmManager>()
        every { packageManager } returns mockk(relaxed = true)
        every { packageName } returns "com.mapbox.navigation.core.MapboxNavigationTest"
        every { filesDir } returns File("some/path")
        every { navigator.experimental } returns mockk()
    }
    val nativeRouter: RouterInterface = mockk {
        every { cancelAll() } just Runs
    }
    val routerWrapperSlot = CapturingSlot<RouterWrapper>()
    val navigatorObserverSlot = slot<NavigatorObserver>()

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
            JobControl(
                mockk { every { children } returns sequenceOf() },
                coroutineRule.createTestScope(),
            )
        }

        mockkObject(LoggerProvider)
        mockkObject(NavigatorLoader)
        every {
            NavigatorLoader.createNativeRouterInterface(any(), any(), any())
        } returns nativeRouter
        every { NavigatorLoader.createConfig(any(), any()) } returns mockk()
        every {
            NavigatorLoader.createHistoryRecorderHandles(
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true) {
            every { composite } returns compositeHandle
        }
        every {
            NavigatorLoader.createCacheHandle(any(), any(), any())
        } returns mockk()

        mockkObject(MapboxSDKCommon)
        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns "some.token"
        every {
            MapboxSDKCommon.getContext().getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockk<ConnectivityManager>()
        mockkObject(MapboxModuleProvider)

        every {
            MapboxModuleProvider.createModule<TripNotification>(
                MapboxModuleType.NavigationTripNotification,
                any(),
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
                any(),
            )
        } returns routeRefreshController
        every {
            NavigationComponentProvider.createEVDynamicDataHolder()
        } returns evDynamicDataHolder
        mockkObject(RouteAlternativesControllerProvider)
        every {
            RouteAlternativesControllerProvider.create(any(), any(), any(), any(), any())
        } returns routeAlternativesController
        mockkObject(MapMatchingAPIProvider)
        every {
            MapMatchingAPIProvider.provideMapMatchingAPI()
        } returns mapMatchingAPI

        every { applicationContext.applicationContext } returns applicationContext
        every { navigator.addNavigatorObserver(capture(navigatorObserverSlot)) } answers {}

        mockTileStore()

        provideNavigationOptions()

        mockNativeNavigator()
        mockTripService()
        mockTripSession()
        mockDirectionSession()
        mockNavigationSession()
        every {
            NavigationComponentProvider.createBillingController(any(), any(), any(), any(), any())
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

        mockkObject(TelemetryUtilsDelegate)
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true

        mockkStatic(NavigationComponentProvider::class)
        every {
            NavigationComponentProvider.createRerouteController(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns defaultRerouteController

        every {
            NavigationComponentProvider.createTilesetVersionManager(any(), any())
        } returns mockk(relaxed = true)

        mockkObject(SdkInfoProvider)

        mockkObject(SystemLocaleWatcher.Companion)

        mockkObject(NavigationTelemetry.Companion)
        every { NavigationTelemetry.create(any(), any()) } returns navigationTelemetry

        every {
            permissionsChecker.hasForegroundServiceLocationPermissions()
        } returns ExpectedFactory.createValue(Unit)

        mockkObject(LowMemoryManager.Companion)
        every { LowMemoryManager.create() } returns lowMemoryManager

        mockkObject(DecodeUtils)
        mockkStatic(DecodeUtils::clearCache)
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
        unmockkObject(TelemetryUtilsDelegate)
        unmockkStatic(MapboxOptions::class)
        unmockkStatic(NavigationComponentProvider::class)
        unmockkObject(SdkInfoProvider)
        unmockkObject(SystemLocaleWatcher.Companion)
        unmockkObject(NavigationTelemetry.Companion)
        unmockkObject(NavigationTileStoreOwner)
        unmockkObject(LowMemoryManager.Companion)
        unmockkObject(DecodeUtils)
        unmockkStatic(DecodeUtils::clearCache)
    }

    fun createMapboxNavigation() {
        mapboxNavigation = MapboxNavigation(
            navigationOptions,
            threadController,
            manualHistoryRecorder,
            copilotHistoryRecorder,
            compositeHistoryRecorder,
            permissionsChecker,
            lowMemoryManager,
        )
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

        every { navigator.getRouter() } returns nativeRouter

        coEvery { navigator.setRoutes(any(), any(), any(), any()) } answers {
            createSetRouteResult()
        }
        every { navigator.cache } returns cache
        every { navigator.telemetry } returns telemetry
        every { navigator.getRerouteDetector() } returns null
        every { navigator.getRerouteController() } returns null
        every { navigator.reset(null) } just Runs
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
            NavigationComponentProvider.createTripSessionLocationEngine(locationOptions)
        } returns tripSessionLocationEngine

        every {
            NavigationComponentProvider.createTripSession(
                tripService = tripService,
                directionsSession = directionsSession,
                tripSessionLocationEngine = tripSessionLocationEngine,
                navigator = navigator,
                threadController,
                any(),
            )
        } returns tripSession
        every { tripSession.getRouteProgress() } returns routeProgress
        coEvery { tripSession.setRoutes(any(), any()) } answers {
            NativeSetRouteValue(
                routes = firstArg(),
                nativeAlternatives = emptyList(),
            )
        }
    }

    private fun mockDirectionSession() {
        every {
            NavigationComponentProvider.createDirectionsSession(capture(routerWrapperSlot))
        } answers {
            directionsSession
        }
        // TODO Needed for telemetry - Free Drive (empty list) for now
        every { directionsSession.routesUpdatedResult } returns createRoutesUpdatedResult(
            emptyList(),
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        )
        every { directionsSession.routes } returns emptyList()
    }

    private fun mockNavigationSession() {
        every { NavigationComponentProvider.createNavigationSession() } answers {
            navigationSession
        }
        every { navigationSession.state } returns NavigationSessionState.Idle
    }

    fun provideNavigationOptions() {
        every { navigationOptions.applicationContext } returns applicationContext
        every { navigationOptions.distanceFormatterOptions } returns distanceFormatterOptions
        every { navigationOptions.navigatorPredictionMillis } returns 1500
        every { navigationOptions.timeFormatType } returns TimeFormat.NONE_SPECIFIED
        every { navigationOptions.locationOptions } returns locationOptions
        every { navigationOptions.routingTilesOptions } returns routingTilesOptions
    }

    fun mockTileStore() {
        mockkObject(NavigationTileStoreOwner)
        every { NavigationTileStoreOwner.init(any()) } answers { }
        every { NavigationTileStoreOwner.invoke() } returns mockk<TileStore>(relaxed = true)
    }
}
