@file:JvmName("NavigationRouteEx")

package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import java.util.UUID

/**
 * Wraps a route object used across the Navigation SDK features.
 *
 * @param directionsResponse the original response that returned this route object.
 * @param routeIndex the index of the route that this wrapper tracks
 * from the collection of routes returned in the original response.
 * @param routeOptions options used to generate the [directionsResponse]
 * @param id unique ID of this route entity used for matching it against additional metadata.
 * Unique ID doesn't mean that the underlying [directionsRoute] is unique.
 * Multiple [NavigationRoute]s can represent similar or equal [DirectionsRoute].
 */
class NavigationRoute(
    val directionsResponse: DirectionsResponse,
    val routeIndex: Int,
    val routeOptions: RouteOptions,
    val id: String = UUID.randomUUID().toString()
) {

    /**
     * [DirectionsRoute] that this [NavigationRoute] represents.
     */
    val directionsRoute = directionsResponse.routes()[routeIndex].toBuilder()
        .requestUuid(directionsResponse.uuid())
        .routeIndex(routeIndex.toString())
        .routeOptions(routeOptions)
        .build()

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * The [NavigationRoute] equality check only takes into consideration
     * the equality of the exact routes tracked by [NavigationRoute] instances which are being compared.
     *
     * This means comparing only [NavigationRoute.directionsRoute] and other response metadata,
     * without comparing all other routes found in the [DirectionsResponse.routes].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationRoute

        if (directionsRoute != other.directionsRoute) return false
        if (routeOptions != other.routeOptions) return false
        if (directionsResponse.waypoints() != other.directionsResponse.waypoints()) return false
        if (directionsResponse.metadata() != other.directionsResponse.metadata()) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = directionsRoute.hashCode()
        result = 31 * result + routeOptions.hashCode()
        result = 31 * result + directionsResponse.waypoints().hashCode()
        result = 31 * result + directionsResponse.metadata().hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationRoute(" +
            "directionsResponse=$directionsResponse, " +
            "routeIndex=$routeIndex, " +
            "routeOptions=$routeOptions, " +
            "id=$id, " +
            ")"
    }
}

/**
 * Returns all [NavigationRoute.directionsRoute]s.
 *
 * This is a lossy mapping since the [DirectionsRoute] cannot carry the same amount of information as [NavigationRoute].
 */
fun List<NavigationRoute>.toDirectionsRoutes() = map { it.directionsRoute }

/**
 * Maps [DirectionsRoute]s to [NavigationRoute]s.
 *
 * This mapping tries to fulfill some of the required [NavigationRoute] data points from the nested features of the [DirectionsRoute],
 * or supplies them with fake supplements to the best of its ability.
 *
 * This is a lossy mapping since the [DirectionsRoute] cannot carry the same amount of information as [NavigationRoute].
 *
 * **Avoid using this mapper and instead try using APIs that accept [NavigationRoute] type where possible.**
 */
fun List<DirectionsRoute>.toNavigationRoutes() = map { it.toNavigationRoute() }

/**
 * Maps [DirectionsRoute] to [NavigationRoute].
 *
 * This mapping tries to fulfill some of the required [NavigationRoute] data points from the nested features of the [DirectionsRoute],
 * or supplies them with fake supplements to the best of its ability.
 */
fun DirectionsRoute.toNavigationRoute(): NavigationRoute {
    val options = requireNotNull(routeOptions()) {
        """
            Provided DirectionsRoute has to have #routeOptions property set.
            If the route was generated independently of Nav SDK,
            rebuild the object and assign the options based on the used request URL.
        """.trimIndent()
    }

    val routeIndex = requireNotNull(routeIndex()?.toInt()) {
        """
            Provided DirectionsRoute has to have #routeIndex property set.
            If the route was generated independently of Nav SDK,
            rebuild the object and assign the index based on the position in the response collection.
        """.trimIndent()
    }

    val routes = Array(routeIndex + 1) { arrIndex ->
        if (arrIndex != routeIndex) {
            // build a fake route to fill up the index gap in the response
            DirectionsRoute.builder()
                .routeIndex(arrIndex.toString())
                .distance(distance())
                .duration(duration())
                .build()
        } else {
            this
        }
    }

    val waypoints = buildWaypointsFromLegs(legs()) ?: buildWaypointsFromOptions(options)

    val response = DirectionsResponse.builder()
        .routes(routes.toList())
        .code("Ok")
        .waypoints(waypoints)
        .uuid(requestUuid())
        .build()
    return NavigationRoute(response, routeIndex, options)
}

/**
 * This function tries to recreate the [DirectionsWaypoint] structure when the full [DirectionsResponse] is not available.
 *
 * It looks at the list of maneuvers for a leg and return either the location of first maneuver
 * to find the origin of the route, or the last maneuver point of each leg to find destinations.
 *
 * The points in the [RouteLeg] are matched to the road graph so this method is preferable to the last resort [buildWaypointsFromOptions].
 */
private fun buildWaypointsFromLegs(
    routeLegs: List<RouteLeg>?
): List<DirectionsWaypoint>? {
    return routeLegs?.mapIndexed { legIndex, routeLeg ->
        val steps = routeLeg.steps() ?: return null
        val getWaypointForStepIndex: (Int) -> DirectionsWaypoint? = { stepIndex ->
            steps.getOrNull(stepIndex)
                ?.maneuver()
                ?.location()
                ?.let {
                    DirectionsWaypoint.builder()
                        .name("")
                        .rawLocation(doubleArrayOf(it.longitude(), it.latitude()))
                        .build()
                }
        }
        val waypoints = mutableListOf<DirectionsWaypoint>()
        if (legIndex == 0) {
            // adds origin of the route
            waypoints.add(getWaypointForStepIndex(0) ?: return null)
        }
        waypoints.add(getWaypointForStepIndex(steps.lastIndex) ?: return null)
        return@mapIndexed waypoints
    }?.flatten()
}

/**
 * This function tries to recreate the [DirectionsWaypoint] structure when the full [DirectionsResponse] is not available.
 *
 * It will return all non-silent waypoints on the route as [DirectionsWaypoint].
 */
private fun buildWaypointsFromOptions(
    routeOptions: RouteOptions
): List<DirectionsWaypoint> {
    val waypointIndices = routeOptions.waypointIndicesList()
    return routeOptions.coordinatesList().filterIndexed { index, _ ->
        waypointIndices?.contains(index) ?: true
    }.map {
        DirectionsWaypoint.builder()
            .name("")
            .rawLocation(doubleArrayOf(it.longitude(), it.latitude()))
            .build()
    }
}
