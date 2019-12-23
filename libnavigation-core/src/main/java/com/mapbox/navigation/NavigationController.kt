package com.mapbox.navigation

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.DirectionsSession as DirectionsSessionModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.HybridRouter
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.Logger as LoggerModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.OffboardRouter
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.OnboardRouter
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.TripNotification as TripNotificationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.TripService as TripServiceModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType.TripSession as TripSessionModule
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.directions.session.DirectionsSession
import com.mapbox.navigation.module.NavigationModuleProvider
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.notification.NavigationNotificationProvider
import com.mapbox.navigation.trip.service.TripService
import com.mapbox.navigation.trip.session.TripSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class NavigationController {

    private val navigator: MapboxNativeNavigator
    private val locationEngine: LocationEngine
    private val locationEngineRequest: LocationEngineRequest
    private val navigationOffboardRoute: NavigationOffboardRoute
    private val navigationNotificationProvider: NavigationNotificationProvider

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private val workerHandler: Handler by lazy { Handler(workerThread.looper) }
    private val workerThread: HandlerThread by lazy {
        HandlerThread("NavigationController").apply { start() }
    }

    private val logger: Logger
    private val directionsSession: DirectionsSession
    private val tripService: TripService
    private val tripSession: TripSession

    constructor(
        context: Context,
        navigator: MapboxNativeNavigator,
        locationEngine: LocationEngine,
        locationEngineRequest: LocationEngineRequest,
        tripServiceLambda: () -> Unit,
        navigationNotificationProvider: NavigationNotificationProvider
    ) {
        this.context = context
        this.navigator = navigator
        this.locationEngine = locationEngine
        this.locationEngineRequest = locationEngineRequest
        this.navigationNotificationProvider = navigationNotificationProvider
        this.navigationOffboardRoute = navigationOffboardRoute

        logger = NavigationModuleProvider.createModule(LoggerModule, ::paramsProvider)
        directionsSession = NavigationComponentProvider.createDirectionsSession(
            NavigationModuleProvider.createModule(HybridRouter, ::paramsProvider),
            routeObserver
        )
        tripService = NavigationComponentProvider.createTripService(
            NavigationModuleProvider.createModule(
                MapboxNavigationModuleType.TripNotification,
                ::paramsProvider
            ), tripServiceLambda
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigator,
            mainHandler,
            workerHandler
        )
    }

    private val routeObserver = object : DirectionsSession.RouteObserver {
        override fun onRoutesChanged(routes: List<Route>) {
            TODO("not implemented")
        }

        override fun onRoutesRequested() {
            TODO("not implemented")
        }

        override fun onRoutesRequestFailure(throwable: Throwable) {
            TODO("not implemented")
        }
    }

    /**
     * Provides parameters for Mapbox default modules, recursively if a module depends on other Mapbox modules.
     */
    private fun paramsProvider(type: MapboxNavigationModuleType): Array<Pair<Class<*>?, Any?>> {
        return when (type) {
            HybridRouter -> arrayOf(
                Router::class.java to NavigationModuleProvider.createModule(
                    OnboardRouter,
                    ::paramsProvider
                ),
                Router::class.java to NavigationModuleProvider.createModule(
                    OffboardRouter,
                    ::paramsProvider
                )
            )
            OffboardRouter -> arrayOf(
                NavigationOffboardRoute::class.java to navigationOffboardRoute
            )
            OnboardRouter -> arrayOf(
                MapboxNativeNavigator::class.java to navigator
            )
            DirectionsSessionModule -> throw NotImplementedError() // going to be removed when next base version
            TripNotificationModule -> arrayOf(
                Context::class.java to context,
                NavigationNotificationProvider::class.java to navigationNotificationProvider
            )
            TripServiceModule -> throw NotImplementedError() // going to be removed when next base version
            TripSessionModule -> throw NotImplementedError() // going to be removed when next base version
            LoggerModule -> arrayOf()
        }
    }

    internal fun onDestroy() {
        workerThread.quit()
    }
}
