package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.internal.trip.service.MapboxTripService
import com.mapbox.navigation.core.internal.trip.service.TripService
import com.mapbox.navigation.core.internal.trip.session.MapboxTripSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl

internal object NavigationComponentProvider {
    fun createDirectionsSession(
        router: Router
    ): DirectionsSession =
        MapboxDirectionsSession(router)

    fun createNativeNavigator(deviceProfile: DeviceProfile, logger: Logger?): MapboxNativeNavigator =
        MapboxNativeNavigatorImpl.create(deviceProfile, logger)

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
        logger: Logger
    ): TripSession = MapboxTripSession(
        tripService,
        locationEngine,
        navigatorPredictionMillis,
        navigator = navigator,
        logger = logger
    )

    fun createNavigationSession(): NavigationSession = NavigationSession()
}
