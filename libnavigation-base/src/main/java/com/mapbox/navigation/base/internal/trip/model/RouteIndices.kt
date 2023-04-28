package com.mapbox.navigation.base.internal.trip.model

/**
 * Contains information about route indices for a route.
 *
 * @param legIndex current leg index of the route. Analogous to [RouteLegProgress.legIndex] for primary route.
 * @param stepIndex current step index of the leg. Analogous to [RouteStepProgress.stepIndex] for primary route.
 * @param routeGeometryIndex current index in the route geometry. Analogous to [RouteProgress.currentRouteGeometryIndex] for primary route.
 * @param legGeometryIndex current index in the leg geometry. Analogous to [RouteLegProgress.geometryIndex] for primary route.
 * @param intersectionIndex current step-wise intersection index. Analogous to [RouteStepProgress.intersectionIndex] for primary route.
 */
class RouteIndices internal constructor(
    val legIndex: Int,
    val stepIndex: Int,
    val routeGeometryIndex: Int,
    val legGeometryIndex: Int,
    val intersectionIndex: Int,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteIndices

        if (legIndex != other.legIndex) return false
        if (stepIndex != other.stepIndex) return false
        if (routeGeometryIndex != other.routeGeometryIndex) return false
        if (legGeometryIndex != other.legGeometryIndex) return false
        if (intersectionIndex != other.intersectionIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = legIndex.hashCode()
        result = 31 * result + stepIndex
        result = 31 * result + routeGeometryIndex
        result = 31 * result + legGeometryIndex
        result = 31 * result + intersectionIndex
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteIndices(" +
            "legIndex=$legIndex, " +
            "stepIndex=$stepIndex, " +
            "routeGeometryIndex=$routeGeometryIndex, " +
            "legGeometryIndex=$legGeometryIndex, " +
            "intersectionIndex=$intersectionIndex" +
            ")"
    }
}
