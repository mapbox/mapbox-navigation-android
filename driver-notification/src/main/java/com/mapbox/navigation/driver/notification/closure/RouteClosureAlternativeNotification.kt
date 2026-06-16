package com.mapbox.navigation.driver.notification.closure

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.driver.notification.DriverNotification

/**
 * Notification emitted when a road closure is detected within the configured threshold distance
 * and a closure alternative route is available after a routes update.
 *
 * @param incidentId unique identifier of the closure incident
 * @param alternativeRoute the first available alternative route to switch onto
 *
 * @see [RouteClosureMonitoringNotification] emitted when the closure is far ahead
 * @see [RouteClosureResolvedNotification] emitted when no closure is detected
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteClosureAlternativeNotification internal constructor(
    val incidentId: String,
    val distanceMeters: Double,
    val alternativeRoute: NavigationRoute,
) : DriverNotification() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteClosureAlternativeNotification) return false
        return incidentId == other.incidentId
    }

    override fun hashCode(): Int = incidentId.hashCode()

    override fun toString(): String =
        "RouteClosureAlternativeNotification(" +
            "incidentId=$incidentId, " +
            "distanceMeters=$distanceMeters, " +
            "altRouteId=${alternativeRoute.id}" +
            ")"
}
