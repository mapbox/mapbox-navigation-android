package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
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
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.TilesConfig

internal object NavigationComponentProvider {

    fun createDirectionsSession(
        router: NavigationRouter,
    ): DirectionsSession = MapboxDirectionsSession(router)

    fun createNativeNavigator(
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        tilesConfig: TilesConfig,
        accessToken: String,
        router: RouterInterface,
    ): MapboxNativeNavigator = MapboxNativeNavigatorImpl.create(
        config,
        historyRecorderComposite,
        tilesConfig,
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
}
