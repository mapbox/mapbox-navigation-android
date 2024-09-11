package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * Metadata of an alternative route in the current navigation session.
 *
 * Valid only until [RoutesObserver] or [NavigationRouteAlternativesObserver] fires again.
 *
 * @param navigationRoute the alternative route
 * @param forkIntersectionOfAlternativeRoute intersection point of this alternative route with the primary route,
 * from the perspective of the alternative route
 * @param forkIntersectionOfPrimaryRoute intersection point of this alternative route with the primary route,
 * from the perspective of the primary route
 * @param infoFromFork information about the alternative route from the fork with the primary route, until the destination
 * @param infoFromStartOfPrimary summed up information about the alternative route by joining
 * the primary route's data until the deviation point with the alternative route's data from the deviation point
 */
class AlternativeRouteMetadata internal constructor(
    val navigationRoute: NavigationRoute,
    val forkIntersectionOfAlternativeRoute: AlternativeRouteIntersection,
    val forkIntersectionOfPrimaryRoute: AlternativeRouteIntersection,
    val infoFromFork: AlternativeRouteInfo,
    val infoFromStartOfPrimary: AlternativeRouteInfo,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlternativeRouteMetadata

        if (navigationRoute.id != other.navigationRoute.id) return false
        if (forkIntersectionOfAlternativeRoute != other.forkIntersectionOfAlternativeRoute) {
            return false
        }
        if (forkIntersectionOfPrimaryRoute != other.forkIntersectionOfPrimaryRoute) return false
        if (infoFromFork != other.infoFromFork) return false
        if (infoFromStartOfPrimary != other.infoFromStartOfPrimary) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = navigationRoute.id.hashCode()
        result = 31 * result + forkIntersectionOfAlternativeRoute.hashCode()
        result = 31 * result + forkIntersectionOfPrimaryRoute.hashCode()
        result = 31 * result + infoFromFork.hashCode()
        result = 31 * result + infoFromStartOfPrimary.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AlternativeRouteMetadata(" +
            "navigationRouteId=${navigationRoute.id}, " +
            "forkIntersectionOfAlternativeRoute=$forkIntersectionOfAlternativeRoute, " +
            "forkIntersectionOfPrimaryRoute=$forkIntersectionOfPrimaryRoute, " +
            "infoFromFork=$infoFromFork, " +
            "infoFromStartOfPrimary=$infoFromStartOfPrimary" +
            ")"
    }
}
