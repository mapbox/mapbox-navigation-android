package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.EHorizonSubscriptionManagerImpl
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.TilesConfig

internal object NavigationComponentProvider {
    fun createDirectionsSession(
        router: Router,
        logger: Logger
    ): DirectionsSession = MapboxDirectionsSession(router, logger)

    fun createNativeNavigator(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        logger: Logger
    ): MapboxNativeNavigator =
        MapboxNativeNavigatorImpl.create(deviceProfile, navigatorConfig, tilesConfig, logger)

    fun createTripService(
        applicationContext: Context,
        tripNotification: TripNotification,
        logger: Logger
    ): TripService = MapboxTripService(
        applicationContext,
        tripNotification,
        logger
    )

    fun createTripSession(
        tripService: TripService,
        navigationOptions: NavigationOptions,
        navigator: MapboxNativeNavigator,
        logger: Logger,
        accessToken: String?
    ): TripSession = MapboxTripSession(
        tripService,
        navigationOptions,
        navigator = navigator,
        logger = logger,
        accessToken = accessToken,
        eHorizonSubscriptionManager = EHorizonSubscriptionManagerImpl(navigator),
    )

    fun createNavigationSession(): NavigationSession = NavigationSession()
}
