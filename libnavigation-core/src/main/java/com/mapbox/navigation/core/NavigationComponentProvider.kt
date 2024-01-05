package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.preview.NativeRoutesDataParser
import com.mapbox.navigation.core.preview.RoutesPreviewController
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManagerImpl
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.RouterInterface
import kotlinx.coroutines.CoroutineScope

internal object NavigationComponentProvider {

    fun createDirectionsSession(
        router: NavigationRouterV2,
    ): DirectionsSession = MapboxDirectionsSession(router)

    fun createNativeNavigator(
        cacheHandle: CacheHandle,
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        accessToken: String,
        router: RouterInterface?,
    ): MapboxNativeNavigator = MapboxNativeNavigatorImpl.create(
        cacheHandle,
        config,
        historyRecorderComposite,
        accessToken,
        router,
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
        navigationOptions: NavigationOptions
    ): TripSessionLocationEngine = TripSessionLocationEngine(navigationOptions)

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
        accessToken: String?,
        navigationSession: NavigationSession,
        tripSession: TripSession,
        arrivalProgressObserver: ArrivalProgressObserver
    ): BillingController = BillingController(
        navigationSession,
        arrivalProgressObserver,
        accessToken.toString(),
        tripSession
    )

    fun createArrivalProgressObserver(
        tripSession: TripSession
    ): ArrivalProgressObserver = ArrivalProgressObserver(tripSession)

    fun createHistoryRecordingStateHandler(): HistoryRecordingStateHandler =
        HistoryRecordingStateHandler()

    fun createDeveloperMetadataAggregator(
        historyRecordingStateHandler: HistoryRecordingStateHandler,
    ): DeveloperMetadataAggregator = DeveloperMetadataAggregator(
        historyRecordingStateHandler.currentCopilotSession().sessionId
    ).also {
        historyRecordingStateHandler.registerCopilotSessionObserver(it)
    }

    fun createRoutesPreviewController(
        scope: CoroutineScope
    ) = RoutesPreviewController(
        routesDataParser = NativeRoutesDataParser(),
        scope = scope
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
}
