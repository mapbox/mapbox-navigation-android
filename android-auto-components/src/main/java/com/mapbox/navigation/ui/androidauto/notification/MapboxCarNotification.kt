package com.mapbox.navigation.ui.androidauto.notification

import android.app.PendingIntent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.model.CarColor
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.TimeFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

/**
 * Register this observer using [MapboxNavigationApp.registerObserver]. As long as it is
 * registered, the trip notification will be shown in the car using
 * [MapboxNavigation.setTripNotificationInterceptor].
 */
class MapboxCarNotification internal constructor(
    private val notificationOptions: () -> MapboxCarNotificationOptions,
    private val carContext: CarContext,
    private val idleExtenderUpdater: IdleExtenderUpdater,
    private val freeDriveExtenderUpdater: FreeDriveExtenderUpdater,
    private val activeGuidanceExtenderUpdater: ActiveGuidanceExtenderUpdater,
) : MapboxNavigationObserver {

    /**
     * Creates the car trip notification for an app that does not use
     * [com.mapbox.navigation.ui.androidauto.MapboxCarContext].
     *
     * @param carContext the car context of the session.
     * @param notificationOptions options of the notification.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    constructor(
        carContext: CarContext,
        notificationOptions: MapboxCarNotificationOptions,
    ) : this(
        { notificationOptions },
        carContext,
        IdleExtenderUpdater(carContext),
        FreeDriveExtenderUpdater(carContext),
        ActiveGuidanceExtenderUpdater(carContext),
    )

    // The observers run on the main thread, while the trip service calls the notification
    // interceptor on a background thread. The fields below are guarded by this lock. The lock is
    // held only to read or write them, never while the notification is rendered, so the
    // observers never wait for a render.
    private val lock = Any()

    // Serializes the interceptor calls, which can overlap when the trip service starts, because
    // the extender updaters keep state between calls. Taken before [lock], never inside it.
    private val renderLock = Any()
    private var navigationSessionState: NavigationSessionState = NavigationSessionState.Idle
    private var routeProgress: RouteProgress? = null

    // Set when a trip ends. The extender updaters are only used from the interceptor under
    // [renderLock], so the reset is applied there.
    private var activeGuidanceResetPending = false

    // The formatters come from NavigationOptions, which don't change for a MapboxNavigation
    // instance, so they are created once per attach instead of once per notification update.
    private var distanceFormatter: DistanceFormatter? = null
    private var timeFormatter: TimeFormatter? = null
    private val color: CarColor by lazy {
        val color = carContext.getColor(com.mapbox.navigation.base.R.color.mapbox_notification_blue)
        CarColor.createCustom(color, color)
    }
    private var contentIntent: Pair<Class<out CarAppService>, PendingIntent>? = null

    private val navigationSessionStateObserver = NavigationSessionStateObserver { sessionState ->
        synchronized(lock) {
            val previousState = navigationSessionState
            navigationSessionState = sessionState
            // A trip ended or was replaced, so nothing from it may show on the next one.
            if (previousState is NavigationSessionState.ActiveGuidance &&
                previousState != sessionState
            ) {
                resetActiveGuidance()
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        synchronized(lock) {
            // A progress of the previous route can still arrive after its trip has ended.
            if (navigationSessionState is NavigationSessionState.ActiveGuidance) {
                this.routeProgress = routeProgress
            }
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val navigationOptions = mapboxNavigation.navigationOptions
        synchronized(lock) {
            distanceFormatter = MapboxDistanceFormatter(navigationOptions.distanceFormatterOptions)
            timeFormatter = navigationOptions.timeFormatter
        }
        mapboxNavigation.registerNavigationSessionStateObserver(navigationSessionStateObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.setTripNotificationInterceptor { notificationBuilder ->
            notificationBuilder
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .extend(synchronized(renderLock) { getExtenderBuilder() }.build())
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.setTripNotificationInterceptor(null)
        mapboxNavigation.unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        synchronized(lock) {
            navigationSessionState = NavigationSessionState.Idle
            resetActiveGuidance()
            distanceFormatter = null
            timeFormatter = null
        }
    }

    private class Snapshot(
        val sessionState: NavigationSessionState,
        val routeProgress: RouteProgress?,
        val distanceFormatter: DistanceFormatter?,
        val timeFormatter: TimeFormatter?,
        val resetActiveGuidance: Boolean,
    )

    private fun takeSnapshot(): Snapshot = synchronized(lock) {
        Snapshot(
            navigationSessionState,
            routeProgress,
            distanceFormatter,
            timeFormatter,
            activeGuidanceResetPending,
        ).also { activeGuidanceResetPending = false }
    }

    private fun getExtenderBuilder(): CarAppExtender.Builder {
        val snapshot = takeSnapshot()
        if (snapshot.resetActiveGuidance) {
            activeGuidanceExtenderUpdater.reset()
        }
        val extenderBuilder = CarAppExtender.Builder()
            .setColor(color)
            .setSmallIcon(com.mapbox.navigation.ui.base.R.drawable.mapbox_ic_navigation)

        val carStartAppClass = notificationOptions().startAppService
        if (carStartAppClass != null) {
            extenderBuilder.setContentIntent(getContentIntent(carStartAppClass))
        }

        val distanceFormatter = snapshot.distanceFormatter
        val timeFormatter = snapshot.timeFormatter
        when (snapshot.sessionState) {
            is NavigationSessionState.Idle -> setIdleMode(extenderBuilder)
            is NavigationSessionState.FreeDrive -> setFreeDriveMode(extenderBuilder)
            is NavigationSessionState.ActiveGuidance -> {
                val routeProgress = snapshot.routeProgress
                if (routeProgress != null && distanceFormatter != null && timeFormatter != null) {
                    activeGuidanceExtenderUpdater.update(
                        extenderBuilder,
                        routeProgress,
                        distanceFormatter,
                        timeFormatter,
                    )
                } else {
                    setIdleMode(extenderBuilder)
                }
            }
        }

        return extenderBuilder
    }

    private fun getContentIntent(carStartAppClass: Class<out CarAppService>): PendingIntent {
        contentIntent?.takeIf { it.first == carStartAppClass }?.let { return it.second }
        return CarPendingIntentFactory.create(carContext, carStartAppClass).also {
            contentIntent = carStartAppClass to it
        }
    }

    // Called with the lock held.
    private fun resetActiveGuidance() {
        routeProgress = null
        activeGuidanceResetPending = true
    }

    private fun setIdleMode(extenderBuilder: CarAppExtender.Builder) {
        idleExtenderUpdater.update(extenderBuilder)
        activeGuidanceExtenderUpdater.reset()
    }

    private fun setFreeDriveMode(extenderBuilder: CarAppExtender.Builder) {
        freeDriveExtenderUpdater.update(extenderBuilder)
        activeGuidanceExtenderUpdater.reset()
    }
}
