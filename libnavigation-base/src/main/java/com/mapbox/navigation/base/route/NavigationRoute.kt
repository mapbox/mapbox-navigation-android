@file:JvmName("NavigationRouteEx")

package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigator.RouteInterface
import java.net.URL

/**
 * Wraps a route object used across the Navigation SDK features.
 * @param directionsResponse the original response that returned this route object.
 * @param routeIndex the index of the route that this wrapper tracks
 * from the collection of routes returned in the original response.
 * @param routeOptions options used to generate the [directionsResponse]
 */
class NavigationRoute internal constructor(
    val directionsResponse: DirectionsResponse,
    val routeIndex: Int,
    val routeOptions: RouteOptions,
    internal val nativeRoute: RouteInterface,
) {

    companion object {
        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponse].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponse response to be parsed into [NavigationRoute]s
         * @param routeOptions options used to generate the [directionsResponse]
         */
        fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString()
            )
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponseJson].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponseJson response to be parsed into [NavigationRoute]s
         * @param routeRequestUrl URL used to generate the [directionsResponse]
         */
        fun create(
            directionsResponseJson: String,
            routeRequestUrl: String,
        ): List<NavigationRoute> {
            return create(
                DirectionsResponse.fromJson(directionsResponseJson),
                directionsResponseJson = directionsResponseJson,
                RouteOptions.fromUrl(URL(routeRequestUrl)),
                routeOptionsUrlString = routeRequestUrl
            )
        }

        internal fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            routeParser: SDKRouteParser,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString(),
                routeParser
            )
        }

        private fun create(
            directionsResponse: DirectionsResponse,
            directionsResponseJson: String,
            routeOptions: RouteOptions,
            routeOptionsUrlString: String,
            routeParser: SDKRouteParser = NativeRouteParserWrapper
        ): List<NavigationRoute> {
            return routeParser.parseDirectionsResponse(
                directionsResponseJson, routeOptionsUrlString
            ).fold(
                { error ->
                    throw RuntimeException("Failed to parse a route. Reason: $error")
                },
                { value ->
                    value.mapIndexed { index, routeInterface ->
                        NavigationRoute(
                            directionsResponse,
                            index,
                            routeOptions,
                            routeInterface
                        )
                    }
                }
            ).cache()
        }
    }

    /**
     * Unique local identifier of the route instance.
     * For routes which contain server-side UUID it's equal to: `UUID + "#" + routeIndex`, for example: `d77PcddF8rhGUc3ORYGfcwcDfS_8QW6r1iXugXD0HOgmr9CWL8wn0g==#0`.
     * For routes which were generated onboard and do not have a UUID it's equal to: `"local@" + generateUuid() + "#" + routeIndex`, for example: `local@84438c3e-f608-47e9-88cc-cddf341d2fb1#0`.
     */
    val id: String = nativeRoute.routeId

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

        if (id != other.id) return false
        if (directionsRoute != other.directionsRoute) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + directionsRoute.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationRoute(id=$id)"
    }

    internal fun copy(
        directionsResponse: DirectionsResponse = this.directionsResponse,
        routeIndex: Int = this.routeIndex,
        routeOptions: RouteOptions = this.routeOptions,
        nativeRoute: RouteInterface = this.nativeRoute,
    ): NavigationRoute =
        NavigationRoute(directionsResponse, routeIndex, routeOptions, nativeRoute).cache()
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
 * This compatibility extension is blocking and can now take a significant amount of time to return (in order of hundreds of milliseconds for long routes).
 * To avoid potential slowdowns, try not using the compatibility layer and work with [NavigationRoute] and [NavigationRouterCallback] were possible (look for deprecation warnings and refactor).
 * There's a cache layer baked in that avoids the blocking operations if the route was generated by the Navigation SDK itself which could avoid majority of blocking operations, but it's not guaranteed.
 *
 * **Avoid using this mapper and instead try using APIs that accept [NavigationRoute] type where possible.**
 */
fun List<DirectionsRoute>.toNavigationRoutes() = map { it.toNavigationRoute() }

/**
 * Maps [DirectionsRoute] to [NavigationRoute].
 *
 * This mapping tries to fulfill some of the required [NavigationRoute] data points from the nested features of the [DirectionsRoute],
 * or supplies them with fake supplements to the best of its ability.
 *
 * This is a lossy mapping since the [DirectionsRoute] cannot carry the same amount of information as [NavigationRoute].
 *
 * This compatibility extension is blocking and can now take a significant amount of time to return (in order of hundreds of milliseconds for long routes).
 * To avoid potential slowdowns, try not using the compatibility layer and work with [NavigationRoute] and [NavigationRouterCallback] were possible (look for deprecation warnings and refactor).
 * There's a cache layer baked in that avoids the blocking operations if the route was generated by the Navigation SDK itself which could avoid majority of blocking operations, but it's not guaranteed.
 *
 * **Avoid using this mapper and instead try using APIs that accept [NavigationRoute] type where possible.**
 */
fun DirectionsRoute.toNavigationRoute(): NavigationRoute = this.toNavigationRoute(
    NativeRouteParserWrapper
)

internal fun DirectionsRoute.toNavigationRoute(sdkRouteParser: SDKRouteParser): NavigationRoute {
    RouteCompatibilityCache.getFor(this)?.let {
        return it
    }

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
            /**
             *  build a fake route to fill up the index gap in the response
             *  This is needed, so that calling NavigationRoute#directionsResponse#routes().get(routeIndex) always returns a correct value.
             *  Without padding the response with fake routes, the DirectionsRoute that we're wrapping would be at a wrong index in the response.
             */
            fakeDirectionsRoute
                .toBuilder()
                .routeIndex(arrIndex.toString())
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
    return NavigationRoute.create(response, options, sdkRouteParser)[routeIndex]
}

internal fun RouteInterface.toNavigationRoute(): NavigationRoute {
    return NavigationRoute(
        directionsResponse = DirectionsResponse.fromJson(responseJson),
        routeOptions = RouteOptions.fromUrl(URL(requestUri)),
        routeIndex = routeIndex,
        nativeRoute = this
    ).cache()
}

private fun List<NavigationRoute>.cache(): List<NavigationRoute> {
    RouteCompatibilityCache.cacheCreationResult(this)
    return this
}

private fun NavigationRoute.cache(): NavigationRoute {
    RouteCompatibilityCache.cacheCreationResult(listOf(this))
    return this
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

private val fakeDirectionsRoute: DirectionsRoute by lazy {
    val fakeIntersection = StepIntersection.builder()
        .rawLocation(doubleArrayOf(0.0, 0.0))
        .build()
    val fakeManeuver = StepManeuver.builder()
        .rawLocation(doubleArrayOf(0.0, 0.0))
        .type(StepManeuver.END_OF_ROAD)
        .build()
    val fakeSteps = listOf(
        LegStep.builder()
            .distance(0.0)
            .duration(0.0)
            .mode("fake")
            .maneuver(fakeManeuver)
            .weight(0.0)
            .geometry(
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(0.0, 0.0),
                        Point.fromLngLat(0.0, 0.0)
                    )
                ).toPolyline(6)
            )
            .intersections(listOf(fakeIntersection))
            .build()
    )
    val fakeLegs = listOf(
        RouteLeg.builder()
            .steps(fakeSteps)
            .build()
    )
    DirectionsRoute.builder()
        .distance(0.0)
        .duration(0.0)
        .legs(fakeLegs)
        .build()
}
