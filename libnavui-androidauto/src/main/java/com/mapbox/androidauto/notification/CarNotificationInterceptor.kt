package com.mapbox.androidauto.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.model.CarColor
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.notification.CarPendingIntent
import androidx.core.app.NotificationCompat
import com.mapbox.androidauto.R
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatter
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
@ExperimentalPreviewMapboxNavigationAPI
class CarNotificationInterceptor internal constructor(
    private val context: Context,
    private val pendingOpenIntent: PendingIntent?,
    private val idleExtenderUpdater: IdleExtenderUpdater,
    private val freeDriveExtenderUpdater: FreeDriveExtenderUpdater,
    private val activeGuidanceExtenderUpdater: ActiveGuidanceExtenderUpdater,
) : MapboxNavigationObserver {

    private var navigationSessionState: NavigationSessionState = NavigationSessionState.Idle
    private var routeProgress: RouteProgress? = null

    private val navigationSessionStateObserver = NavigationSessionStateObserver { sessionState ->
        navigationSessionState = sessionState
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        this.routeProgress = routeProgress
    }

    constructor(context: Context, carAppServiceClass: Class<out CarAppService>) : this(
        context,
        CarPendingIntent.getCarApp(
            context,
            0,
            Intent(Intent.ACTION_VIEW).setComponent(ComponentName(context, carAppServiceClass)),
            PendingIntent.FLAG_UPDATE_CURRENT,
        ),
        IdleExtenderUpdater(context),
        FreeDriveExtenderUpdater(context),
        ActiveGuidanceExtenderUpdater(context),
    )

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerNavigationSessionStateObserver(navigationSessionStateObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.setTripNotificationInterceptor { notificationBuilder ->
            val color = context.getColor(R.color.mapbox_notification_blue)
            val formatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions
            val extenderBuilder = getExtenderBuilder(
                MapboxDistanceFormatter(formatterOptions),
                mapboxNavigation.navigationOptions.timeFormatType,
                CarColor.createCustom(color, color),
            )
            notificationBuilder
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .extend(extenderBuilder.build())
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.setTripNotificationInterceptor(null)
        mapboxNavigation.unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }

    private fun getExtenderBuilder(
        distanceFormatter: DistanceFormatter,
        @TimeFormat.Type timeFormatType: Int,
        color: CarColor,
    ): CarAppExtender.Builder {
        val extenderBuilder = CarAppExtender.Builder()
            .setColor(color)
            .setSmallIcon(R.drawable.mapbox_ic_navigation)

        if (pendingOpenIntent != null) {
            extenderBuilder.setContentIntent(pendingOpenIntent)
        }

        when (navigationSessionState) {
            is NavigationSessionState.Idle -> setIdleMode(extenderBuilder)
            is NavigationSessionState.FreeDrive -> setFreeDriveMode(extenderBuilder)
            is NavigationSessionState.ActiveGuidance -> {
                val routeProgress = routeProgress
                if (routeProgress != null) {
                    activeGuidanceExtenderUpdater.update(
                        extenderBuilder,
                        routeProgress,
                        distanceFormatter,
                        timeFormatType,
                    )
                } else {
                    setIdleMode(extenderBuilder)
                }
            }
        }

        return extenderBuilder
    }

    private fun setIdleMode(extenderBuilder: CarAppExtender.Builder) {
        idleExtenderUpdater.update(extenderBuilder)
        activeGuidanceExtenderUpdater.updateCurrentManeuverToDefault()
    }

    private fun setFreeDriveMode(extenderBuilder: CarAppExtender.Builder) {
        freeDriveExtenderUpdater.update(extenderBuilder)
        activeGuidanceExtenderUpdater.updateCurrentManeuverToDefault()
    }
}
