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
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService
import com.mapbox.navigation.base.trip.TripSession
import com.mapbox.navigation.module.NavigationModuleProvider
import com.mapbox.navigation.navigator.MapboxNativeNavigator

class NavigationController(
    private val navigator: MapboxNativeNavigator,
    private val locationEngine: LocationEngine,
    private val locationEngineRequest: LocationEngineRequest
) {

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private val workerHandler: Handler by lazy { Handler(workerThread.looper) }
    private val workerThread: HandlerThread by lazy {
        HandlerThread("NavigationController").apply { start() }
    }

    private val logger: Logger
    private val directionsSession: DirectionsSession
    private val tripSession: TripSession

    init {
        logger = NavigationModuleProvider.createModule(LoggerModule, ::paramsProvider)
        directionsSession = NavigationModuleProvider.createModule(DirectionsSessionModule, ::paramsProvider)
        tripSession = NavigationModuleProvider.createModule(TripSessionModule, ::paramsProvider)
    }

    /**
     * Provides parameters for Mapbox default modules, recursively if a module depends on other Mapbox modules.
     */
    private fun paramsProvider(type: MapboxNavigationModuleType): Array<Pair<Class<*>?, Any?>> {
        return when (type) {
            HybridRouter -> arrayOf(
                Pair(Router::class.java, NavigationModuleProvider.createModule(OnboardRouter, ::paramsProvider)),
                Pair(Router::class.java, NavigationModuleProvider.createModule(OffboardRouter, ::paramsProvider))
            )
            OffboardRouter -> arrayOf()
            OnboardRouter -> arrayOf(
                Pair(MapboxNativeNavigator::class.java, navigator)
            )
            DirectionsSessionModule -> arrayOf(
                Pair(Router::class.java, NavigationModuleProvider.createModule(HybridRouter, ::paramsProvider))
            )
            TripNotificationModule -> arrayOf()
            TripServiceModule -> arrayOf(
                Pair(TripNotification::class.java, NavigationModuleProvider.createModule(TripNotificationModule, ::paramsProvider))
            )
            TripSessionModule -> arrayOf(
                Pair(TripService::class.java, NavigationModuleProvider.createModule(TripServiceModule, ::paramsProvider)),
                Pair(LocationEngine::class.java, locationEngine),
                Pair(LocationEngineRequest::class.java, locationEngineRequest),
                Pair(MapboxNativeNavigator::class.java, navigator),
                Pair(Handler::class.java, mainHandler),
                Pair(Handler::class.java, workerHandler)
            )
            LoggerModule -> arrayOf()
        }
    }

    internal fun onDestroy() {
        workerThread.quit()
    }
}
