package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigator.NavigatorConfig

internal object NavigationComponentProvider {
    fun createDirectionsSession(
        router: Router
    ): DirectionsSession =
        MapboxDirectionsSession(router)

    fun createNativeNavigator(deviceProfile: DeviceProfile, navigatorConfig: NavigatorConfig, logger: Logger?): MapboxNativeNavigator =
        MapboxNativeNavigatorImpl.create(deviceProfile, navigatorConfig, logger)

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
        locationEngine: LocationEngine,
        navigatorPredictionMillis: Long,
        navigator: MapboxNativeNavigator,
        time: Time,
        logger: Logger,
        accessToken: String?
    ): TripSession = MapboxTripSession(
        tripService,
        locationEngine,
        navigatorPredictionMillis,
        time = time,
        navigator = navigator,
        logger = logger,
        accessToken = accessToken
    )

    fun createNavigationSession(): NavigationSession = NavigationSession()
}
