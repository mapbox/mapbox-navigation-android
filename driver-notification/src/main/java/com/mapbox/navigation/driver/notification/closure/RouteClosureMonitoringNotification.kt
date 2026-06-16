package com.mapbox.navigation.driver.notification.closure

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.driver.notification.DriverNotification

/**
 * Notification emitted when a road closure is detected farther than the configured threshold.
 * Informs the driver without requiring any action; the situation is being monitored.
 *
 * Equality is based on [incidentId] only — distance updates for the same closure do not
 * produce a new notification.
 *
 * @param incidentId unique identifier of the closure incident
 * @param distanceMeters distance from the current position to the start of the closure, in metres
 *
 * @see [RouteClosureAlternativeNotification] emitted when the closure is close and an alternative is available
 * @see [RouteClosureResolvedNotification] emitted when no closure is detected
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteClosureMonitoringNotification internal constructor(
    val incidentId: String,
    val distanceMeters: Double,
) : DriverNotification() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteClosureMonitoringNotification) return false
        return incidentId == other.incidentId
    }

    override fun hashCode(): Int = incidentId.hashCode()

    override fun toString(): String =
        "RouteClosureMonitoringNotification(incidentId=$incidentId, distanceMeters=$distanceMeters)"
}
