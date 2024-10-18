package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.internal.accounts.SkuIdProvider
import com.mapbox.navigation.base.options.EventsAppMetadata
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.ForkPointPassedObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.internal.router.Router
import com.mapbox.navigation.core.preview.NativeRoutesDataParser
import com.mapbox.navigation.core.preview.RoutesPreviewController
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.telemetry.ApplicationLifecycleMonitor
import com.mapbox.navigation.core.telemetry.EventsMetadataInterfaceImpl
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManagerImpl
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.EventsMetadataInterface
import com.mapbox.navigator.HistoryRecorderHandle
import kotlinx.coroutines.CoroutineScope

internal object NavigationComponentProvider {

    fun createDirectionsSession(
        router: Router,
    ): DirectionsSession = MapboxDirectionsSession(router)

    fun createEventsMetadataInterface(
        context: Context,
        lifecycleMonitor: ApplicationLifecycleMonitor,
        eventsAppMetadata: EventsAppMetadata?,
    ): EventsMetadataInterface = EventsMetadataInterfaceImpl(
        context.applicationContext,
        lifecycleMonitor,
        eventsAppMetadata,
    )

    fun createNativeNavigator(
        cacheHandle: CacheHandle,
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        offlineCacheHandle: CacheHandle?,
        eventsMetadataProvider: EventsMetadataInterface,
    ): MapboxNativeNavigator = MapboxNativeNavigatorImpl(
        cacheHandle,
        config,
        historyRecorderComposite,
        offlineCacheHandle,
        eventsMetadataProvider,
    )

    fun createTripService(
        applicationContext: Context,
        tripNotification: TripNotification,
        threadController: ThreadController,
    ): TripService = MapboxTripService(
        applicationContext,
        tripNotification,
        threadController,
    )

    fun createTripSessionLocationEngine(
        locationOptions: LocationOptions,
    ): TripSessionLocationEngine = TripSessionLocationEngine(locationOptions)

    fun createTripSession(
        tripService: TripService,
        tripSessionLocationEngine: TripSessionLocationEngine,
        navigator: MapboxNativeNavigator,
        threadController: ThreadController,
    ): TripSession = MapboxTripSession(
        tripService,
        tripSessionLocationEngine,
        navigator = navigator,
        threadController,
        EHorizonSubscriptionManagerImpl(navigator, threadController),
    )

    fun createNavigationSession(): NavigationSession = NavigationSession()

    fun createBillingController(
        navigationSession: NavigationSession,
        tripSession: TripSession,
        arrivalProgressObserver: ArrivalProgressObserver,
        skuIdProvider: SkuIdProvider,
        sdkInformation: SdkInformation,
    ): BillingController = BillingController(
        navigationSession,
        arrivalProgressObserver,
        tripSession,
        skuIdProvider,
        sdkInformation,
    )

    fun createArrivalProgressObserver(
        tripSession: TripSession,
    ): ArrivalProgressObserver = ArrivalProgressObserver(tripSession)

    fun createHistoryRecordingStateHandler(): HistoryRecordingStateHandler =
        HistoryRecordingStateHandler()

    fun createDeveloperMetadataAggregator(
        historyRecordingStateHandler: HistoryRecordingStateHandler,
    ): DeveloperMetadataAggregator = DeveloperMetadataAggregator(
        historyRecordingStateHandler.currentCopilotSession().sessionId,
    ).also {
        historyRecordingStateHandler.registerCopilotSessionObserver(it)
    }

    fun createRoutesPreviewController(
        scope: CoroutineScope,
    ) = RoutesPreviewController(
        routesDataParser = NativeRoutesDataParser(),
        scope = scope,
    )

    fun createRouteRefreshRequestDataProvider(): RoutesProgressDataProvider =
        RoutesProgressDataProvider()

    fun createEVDynamicDataHolder(): EVDynamicDataHolder = EVDynamicDataHolder()

    fun createRoutesCacheClearer(): RoutesCacheClearer = RoutesCacheClearer()

    fun createRerouteController(
        directionsSession: DirectionsSession,
        tripSession: TripSession,
        routeOptionsProvider: RouteOptionsUpdater,
        rerouteOptions: RerouteOptions,
        threadController: ThreadController,
        evDynamicDataHolder: EVDynamicDataHolder,
    ): InternalRerouteController = MapboxRerouteController(
        directionsSession,
        tripSession,
        routeOptionsProvider,
        rerouteOptions,
        threadController,
        evDynamicDataHolder,
    )

    fun createForkPointPassedObserver(
        directionsSession: DirectionsSession,
        currentLegIndex: () -> Int,
    ): RouteProgressObserver = ForkPointPassedObserver(
        directionsSession,
        currentLegIndex,
    )
}
