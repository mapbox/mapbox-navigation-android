package com.mapbox.services.android.navigation.v5.internal.navigation

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationNotificationProvider
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import timber.log.Timber

/**
 * Internal usage only, use navigation by initializing a new instance of {@link MapboxNavigation}
 * and customizing the navigation experience through that class.
 * <p>
 * This class is first created and started when {@link MapboxNavigation#startNavigation(DirectionsRoute)}
 * get's called and runs in the background until either the navigation sessions ends implicitly or
 * the hosting activity gets destroyed. Location updates are also tracked and handled inside this
 * service. Thread creation gets created in this service and maintains the thread until the service
 * gets destroyed.
 * </p>
 */
internal class NavigationService : Service() {

    private val localBinder = LocalBinder()
    private lateinit var thread: RouteProcessorBackgroundThread
    private lateinit var locationUpdater: LocationUpdater
    private lateinit var routeFetcher: RouteFetcher
    private var notificationProvider: NavigationNotificationProvider? = null

    override fun onBind(intent: Intent?) = localBinder

    /**
     * Only should be called once since we want the service to continue running until the navigation
     * session ends.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NavigationTelemetry.initializeLifecycleMonitor(application)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    /**
     * This gets called when [MapboxNavigation.startNavigation] is called and
     * setups variables among other things on the Navigation Service side.
     */
    fun startNavigation(mapboxNavigation: MapboxNavigation) {
        initialize(mapboxNavigation)
        notificationProvider?.let { notificationProvider ->
            startForegroundNotification(notificationProvider.retrieveNotification())
        }
    }

    /**
     * Removes the location / route listeners and  quits the thread.
     */
    fun endNavigation() {
        NavigationTelemetry.endSession()
        if (::routeFetcher.isInitialized) {
            routeFetcher.clearListeners()
        }
        if (::locationUpdater.isInitialized) {
            locationUpdater.removeLocationUpdates()
        }
        notificationProvider?.let { navNotificationProvider ->
            navNotificationProvider.shutdown(application)
            notificationProvider = null
        }
        if (::thread.isInitialized) {
            thread.quit()
        }
    }

    fun updateLocationEngine(locationEngine: LocationEngine) {
        if (::locationUpdater.isInitialized) {
            locationUpdater.updateLocationEngine(locationEngine)
        }
    }

    fun updateLocationEngineRequest(request: LocationEngineRequest) {
        if (::locationUpdater.isInitialized) {
            locationUpdater.updateLocationEngineRequest(request)
        }
    }

    private fun initialize(mapboxNavigation: MapboxNavigation) {
        val dispatcher = mapboxNavigation.eventDispatcher
        val accessToken = mapboxNavigation.obtainAccessToken()
        initializeRouteFetcher(dispatcher, accessToken, mapboxNavigation.retrieveEngineFactory())
        initializeNotificationProvider(mapboxNavigation)
        notificationProvider?.let { navNotificationProvider ->
            initializeRouteProcessorThread(mapboxNavigation, dispatcher, routeFetcher, navNotificationProvider)
        }
        initializeLocationUpdater(mapboxNavigation)
    }

    private fun initializeRouteFetcher(
        dispatcher: NavigationEventDispatcher,
        accessToken: String,
        engineProvider: NavigationEngineFactory
    ) {
        val fasterRouteEngine = engineProvider.retrieveFasterRouteEngine()
        val listener = NavigationFasterRouteListener(dispatcher, fasterRouteEngine)
        routeFetcher = RouteFetcher(application, accessToken)
        routeFetcher.addRouteListener(listener)
    }

    private fun initializeNotificationProvider(mapboxNavigation: MapboxNavigation) {
        notificationProvider = NavigationNotificationProvider(application, mapboxNavigation)
    }

    private fun initializeRouteProcessorThread(
        mapboxNavigation: MapboxNavigation,
        dispatcher: NavigationEventDispatcher,
        routeFetcher: RouteFetcher,
        notificationProvider: NavigationNotificationProvider
    ) {
        val listener = RouteProcessorThreadListener(dispatcher, routeFetcher, notificationProvider)
        thread = RouteProcessorBackgroundThread(mapboxNavigation, Handler(), listener)
    }

    private fun initializeLocationUpdater(mapboxNavigation: MapboxNavigation) {
        val locationEngine = mapboxNavigation.locationEngine
        val locationEngineRequest = mapboxNavigation.retrieveLocationEngineRequest()
        val dispatcher = mapboxNavigation.eventDispatcher
        locationUpdater = LocationUpdater(applicationContext, thread, dispatcher,
                locationEngine, locationEngineRequest)
    }

    private fun startForegroundNotification(navigationNotification: NavigationNotification) {
        val notification = navigationNotification.getNotification()
        val notificationId = navigationNotification.getNotificationId()
        notification.flags = Notification.FLAG_FOREGROUND_SERVICE
        startForeground(notificationId, notification)
    }

    inner class LocalBinder : Binder() {
        fun getService(): NavigationService {
            Timber.d("Local binder called.")
            return this@NavigationService
        }
    }
}
