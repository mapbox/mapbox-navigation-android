package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.DeviceProfile
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
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.TilesConfig

internal object NavigationComponentProvider {
    fun createDirectionsSession(
        router: NavigationRouter,
    ): DirectionsSession = MapboxDirectionsSession(router)

    fun createNativeNavigator(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger,
        accessToken: String,
    ): MapboxNativeNavigator = MapboxNativeNavigatorImpl.create(
        deviceProfile,
        navigatorConfig,
        tilesConfig,
        historyDir,
        logger,
        accessToken,
    )

    fun createTripService(
        applicationContext: Context,
        tripNotification: TripNotification,
        logger: Logger,
        threadController: ThreadController,
    ): TripService = MapboxTripService(
        applicationContext,
        tripNotification,
        logger,
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
        logger: Logger,
    ): TripSession = MapboxTripSession(
        tripService,
        tripSessionLocationEngine,
        navigator = navigator,
        threadController,
        logger = logger,
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
}
