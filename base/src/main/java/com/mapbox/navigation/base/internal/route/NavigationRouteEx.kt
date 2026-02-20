@file:JvmName("NavigationRouteEx")
@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshMetadata
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterOrigin
import java.net.URL

private const val ROUTE_REFRESH_LOG_CATEGORY = "RouteRefresh"

val NavigationRoute.routerOrigin: RouterOrigin get() = nativeRoute.routerOrigin

/**
 * Internal handle for the route's native peer.
 */
fun NavigationRoute.nativeRoute(): RouteInterface = this.nativeRoute

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@WorkerThread
fun NavigationRoute.internalRefreshRoute(
    refreshResponse: DataRef,
    legIndex: Int,
    legGeometryIndex: Int,
    responseTimeElapsedSeconds: Long,
    experimentalProperties: Map<String, String>? = null,
): Result<NavigationRoute> {
    return this.refresh(
        refreshResponse = refreshResponse,
        legIndex = legIndex,
        legGeometryIndex = legGeometryIndex,
        responseTimeElapsedSeconds = responseTimeElapsedSeconds,
        experimentalProperties = experimentalProperties,
    )
}

val NavigationRoute.routeOptions get() = this.routeOptions

val NavigationRoute.overriddenTraffic get() = this.overriddenTraffic

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationRoute.toDirectionsRefreshResponseInternal() = this.toDirectionsRefreshResponse()

/**
 * Updates only java representation of route.
 * The native route should later be updated through [Navigator.refreshRoute].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationRoute.update(
    directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
    waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
    overriddenTraffic: CongestionNumericOverride? = this.overriddenTraffic,
    routeRefreshMetadata: RouteRefreshMetadata? = this.routeRefreshMetadata,
): NavigationRoute = this.clientSideUpdate(
    directionsRouteBlock,
    waypointsBlock,
    overriddenTraffic,
    routeRefreshMetadata,
).getOrElse {
    logE(ROUTE_REFRESH_LOG_CATEGORY) {
        "Can't update ${nativeRoute.routeId} because of ${it.message}. " +
            "Working with initial route instead."
    }
    this
}

fun NavigationRoute.updateExpirationTime(newExpirationTimeElapsedSeconds: Long?): NavigationRoute {
    this.expirationTimeElapsedSeconds = newExpirationTimeElapsedSeconds
    return this
}

fun NavigationRoute.isExpired(): Boolean {
    return this.expirationTimeElapsedSeconds?.let { Time.SystemClockImpl.seconds() >= it } ?: false
}

/**
 * Used to rebuild any [NavigationRoute] fields that are backed by a native peer, which might've been refreshed.
 *
 * At the moment, all fields are `val`s, so a simple re-instantiation is enough.
 */
fun NavigationRoute.refreshNativePeer(): NavigationRoute = copy()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun String.toDirectionsResponse(requestUrl: String): DirectionsResponse {
    val model = MapMatchingResponse.fromJson(this)
    val routeOptions = RouteOptions.fromUrl(URL(requestUrl))
    return model.toDirectionsResponse(routeOptions)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapMatchingResponse.toDirectionsResponse(routeOptions: RouteOptions): DirectionsResponse {
    val directionRoutes = this.matchings()?.mapIndexed { index, matching ->
        matching.toDirectionRoute()
            .toBuilder()
            .routeIndex("$index")
            .routeOptions(routeOptions)
            .waypoints(
                this.tracepoints()
                    ?.filterNotNull()
                    ?.filter {
                        it.waypointIndex() != null && it.matchingsIndex() == index
                    }
                    ?.map {
                        DirectionsWaypoint.builder()
                            .rawLocation(
                                it.location()!!.coordinates().toDoubleArray(),
                            )
                            .name(it.name() ?: "")
                            // TODO: NAVAND-1737 introduce distance in trace point
                            // .distance(it.distance)
                            .build()
                    },
            )
            .build()
    }?.toMutableList()
    return DirectionsResponse.builder()
        .routes(directionRoutes ?: emptyList())
        // TODO: NAVAND-1737 introduce uuid in map matching response model
        // .uuid(model.uuid())
        .code(this.code())
        .message(this.message())
        .build()
}
