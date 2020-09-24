package com.mapbox.navigation.base.trip.model.alert

/**
 * Holds the route alert and the distance to the point where the alert occurs,
 * or start an alert if it has length.
 *
 * @param routeAlert route alert
 * @param distanceToStart distance to the start of the alert.
 * If the alert has a length, and we've passed the start point,
 * **this value will be negative** until we cross the finish point of the alert's geometry.
 * This negative value, together with [RouteAlertGeometry.length]
 * can be used to calculate the distance since the start of an alert.
 */
class UpcomingRouteAlert private constructor(
    val routeAlert: RouteAlert,
    val distanceToStart: Double
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder() = Builder(routeAlert, distanceToStart)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpcomingRouteAlert

        if (routeAlert != other.routeAlert) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeAlert.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpcomingRouteAlert(routeAlert=$routeAlert, distanceRemaining=$distanceToStart)"
    }

    /**
     * Use to create a new instance.
     *
     * @see UpcomingRouteAlert
     */
    class Builder(
        private val routeAlert: RouteAlert,
        private val distanceRemaining: Double
    ) {

        /**
         * Build the object instance.
         */
        fun build() = UpcomingRouteAlert(routeAlert, distanceRemaining)
    }
}
