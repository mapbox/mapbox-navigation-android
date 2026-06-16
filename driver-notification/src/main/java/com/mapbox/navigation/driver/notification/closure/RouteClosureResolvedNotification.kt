package com.mapbox.navigation.driver.notification.closure

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.driver.notification.DriverNotification

/**
 * Signal to dismiss the monitoring banner. Emitted by [RouteClosureNotificationProvider] whenever
 * the monitoring phase is not active:
 * - no closure is present in [upcomingRoadObjects], or
 * - a closure is present but is within the [RouteClosureNotificationOptions.alternativeTriggerThresholdMeters]
 *   ("close" state), meaning the driver should be offered an alternative rather than the
 *   informational banner.
 *
 * @see [RouteClosureMonitoringNotification] emitted when a far closure is detected
 * @see [RouteClosureAlternativeNotification] emitted when a close closure has an available alternative
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteClosureResolvedNotification internal constructor() : DriverNotification() {

    override fun equals(other: Any?): Boolean = other is RouteClosureResolvedNotification

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "RouteClosureResolvedNotification"
}
