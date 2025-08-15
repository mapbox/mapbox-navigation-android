@file:JvmName("NavigationRouteEx")
@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.Notification
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.internal.utils.Constants.RouteResponse.KEY_REFRESH_TTL
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshMetadata
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterOrigin

private const val ROUTE_REFRESH_LOG_CATEGORY = "RouteRefresh"

val NavigationRoute.routerOrigin: RouterOrigin get() = nativeRoute.routerOrigin

/**
 * Internal handle for the route's native peer.
 */
fun NavigationRoute.nativeRoute(): RouteInterface = this.nativeRoute

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationRoute.internalRefreshRoute(
    routeRefresh: DirectionsRouteRefresh,
    legIndex: Int,
    legGeometryIndex: Int?,
    responseTimeElapsedSeconds: Long,
): NavigationRoute {
    val updatedWaypoints = WaypointsParser.parse(
        routeRefresh.unrecognizedJsonProperties
            ?.get(Constants.RouteResponse.KEY_WAYPOINTS),
    )
    return this.refreshRoute(
        legIndex,
        legGeometryIndex,
        routeRefresh.legs()?.map { it.annotation() },
        routeRefresh.legs()?.map { it.incidents() },
        routeRefresh.legs()?.map { it.closures() },
        routeRefresh.legs()?.map { it.notifications() },
        updatedWaypoints,
        responseTimeElapsedSeconds,
        routeRefresh.unrecognizedJsonProperties
            ?.get(KEY_REFRESH_TTL)?.asInt,
        IncidentsRefresher(),
        ClosuresRefresher(),
        NotificationsRefresher(),
    )
}

/**
 * Updates route's annotations, incidents, and closures in place while keeping the Native peer as is.
 * The peer should later be updated through [Navigator.refreshRoute].
 * Call from a worker thread as the function contains geometry parsing under the hood.
 */
@WorkerThread
fun NavigationRoute.refreshRoute(
    initialLegIndex: Int,
    currentLegGeometryIndex: Int?,
    legAnnotations: List<LegAnnotation?>?,
    incidents: List<List<Incident>?>?,
    closures: List<List<Closure>?>?,
    notifications: List<List<Notification>?>?,
    waypoints: List<DirectionsWaypoint?>?,
    responseTimeElapsedSeconds: Long,
    refreshTtl: Int?,
): NavigationRoute {
    return refreshRoute(
        initialLegIndex,
        currentLegGeometryIndex,
        legAnnotations,
        incidents,
        closures,
        notifications,
        waypoints,
        responseTimeElapsedSeconds,
        refreshTtl,
        IncidentsRefresher(),
        ClosuresRefresher(),
        NotificationsRefresher(),
    )
}

@WorkerThread
internal fun NavigationRoute.refreshRoute(
    initialLegIndex: Int,
    currentLegGeometryIndex: Int?,
    legAnnotations: List<LegAnnotation?>?,
    incidents: List<List<Incident>?>?,
    closures: List<List<Closure>?>?,
    notifications: List<List<Notification>?>?,
    waypoints: List<DirectionsWaypoint?>?,
    responseTimeElapsedSeconds: Long,
    refreshTtl: Int?,
    incidentsRefresher: IncidentsRefresher,
    closuresRefresher: ClosuresRefresher,
    notificationsRefresher: NotificationsRefresher,
): NavigationRoute {
    val updateLegs = directionsRoute.legs()?.mapIndexed { index, routeLeg ->
        if (index < initialLegIndex) {
            routeLeg
        } else {
            val newAnnotation = legAnnotations?.getOrNull(index)
            val startingLegGeometryIndex =
                if (index == initialLegIndex && currentLegGeometryIndex != null) {
                    currentLegGeometryIndex
                } else {
                    0
                }
            val lastLegRefreshIndex = try {
                startingLegGeometryIndex + newAnnotation.size() - 1
            } catch (ex: IllegalArgumentException) {
                // refresh is only possible if annotations are requested, so this is not expected to happen
                logE(ROUTE_REFRESH_LOG_CATEGORY) {
                    ex.message ?: "Unknown error"
                }
                startingLegGeometryIndex
            }
            val mergedAnnotation = AnnotationsRefresher.getRefreshedAnnotations(
                routeLeg.annotation(),
                newAnnotation,
                startingLegGeometryIndex,
                this.overriddenTraffic?.takeIf { it.legIndex == index }?.startIndex,
                this.overriddenTraffic?.takeIf { it.legIndex == index }?.length,
            )
            val mergedIncidents = incidentsRefresher.getRefreshedRoadObjects(
                routeLeg.incidents(),
                incidents?.getOrNull(index),
                startingLegGeometryIndex,
                lastLegRefreshIndex,
            )
            val mergedClosures = closuresRefresher.getRefreshedRoadObjects(
                routeLeg.closures(),
                closures?.getOrNull(index),
                startingLegGeometryIndex,
                lastLegRefreshIndex,
            )

            val mergedNotifications = notificationsRefresher.getRefreshedNotifications(
                routeLeg.notifications(),
                notifications?.getOrNull(index),
                startingLegGeometryIndex,
                lastLegRefreshIndex,
            )

            routeLeg.toBuilder()
                .duration(mergedAnnotation?.duration()?.sumOf { it } ?: routeLeg.duration())
                .annotation(mergedAnnotation)
                .incidents(mergedIncidents)
                .closures(mergedClosures)
                .notifications(mergedNotifications)
                .steps(routeLeg.steps()?.updateSteps(directionsRoute, mergedAnnotation))
                .build()
        }
    }
    val directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute = {
        toBuilder()
            .legs(updateLegs)
            .waypoints(buildNewWaypoints(this.waypoints(), waypoints))
            .updateRouteDurationBasedOnLegsDurationAndChargeTime(
                updateLegs = updateLegs,
                waypoints = buildNewWaypoints(this@refreshRoute.waypoints, waypoints),
            )
            .updateRefreshTtl(this.unrecognizedJsonProperties, refreshTtl)
            .build()
    }
    val waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>? = {
        buildNewWaypoints(this, waypoints)
    }
    val newExpirationTimeElapsedSeconds = refreshTtl?.plus(responseTimeElapsedSeconds)
    return update(
        directionsRouteBlock,
        waypointsBlock,
        newExpirationTimeElapsedSeconds ?: expirationTimeElapsedSeconds,
        routeRefreshMetadata = RouteRefreshMetadata(isUpToDate = true),
    )
}

val NavigationRoute.routeOptions get() = this.routeOptions

val NavigationRoute.overriddenTraffic get() = this.overriddenTraffic

private fun DirectionsRoute.Builder.updateRefreshTtl(
    oldUnrecognizedProperties: Map<String, JsonElement>?,
    newRefreshTtl: Int?,
): DirectionsRoute.Builder {
    return if (newRefreshTtl == null) {
        if (oldUnrecognizedProperties.isNullOrEmpty()) {
            this
        } else {
            unrecognizedJsonProperties(
                oldUnrecognizedProperties.toMutableMap().also {
                    it.remove(Constants.RouteResponse.KEY_REFRESH_TTL)
                },
            )
        }
    } else {
        unrecognizedJsonProperties(
            oldUnrecognizedProperties.orEmpty().toMutableMap().also {
                it[Constants.RouteResponse.KEY_REFRESH_TTL] = JsonPrimitive(newRefreshTtl)
            },
        )
    }
}

/**
 * Updates only java representation of route.
 * The native route should later be updated through [Navigator.refreshRoute].
 */
fun NavigationRoute.update(
    directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
    waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
    newExpirationTimeElapsedSeconds: Long? = this.expirationTimeElapsedSeconds,
    overriddenTraffic: CongestionNumericOverride? = this.overriddenTraffic,
    routeRefreshMetadata: RouteRefreshMetadata? = this.routeRefreshMetadata,
): NavigationRoute {
    val refreshedRoute = directionsRoute.directionsRouteBlock()
    return copy(
        directionsRoute = refreshedRoute,
        waypoints = waypoints.waypointsBlock(),
        expirationTimeElapsedSeconds = newExpirationTimeElapsedSeconds,
        overriddenTraffic = overriddenTraffic,
        routeRefreshMetadata = routeRefreshMetadata,
    )
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

/**
 * Internal API to create a new [NavigationRoute] from a native peer.
 */
fun RouteInterface.toNavigationRoute(
    responseTimeElapsedSeconds: Long,
    directionsResponse: DirectionsResponse,
): NavigationRoute {
    return this.toNavigationRoute(
        responseTimeElapsedSeconds,
        directionsResponse,
    )
}

private fun List<LegStep>.updateSteps(
    route: DirectionsRoute,
    mergedAnnotation: LegAnnotation?,
): List<LegStep> {
    val mergedDurations = mergedAnnotation?.duration() ?: return this
    val result = mutableListOf<LegStep>()
    var previousStepsAnnotationsCount = 0
    forEachIndexed { index, step ->
        val stepPointsSize = route.stepGeometryToPoints(step).size
        if (stepPointsSize < 2) {
            logE(
                "step at $index has less than 2 points, unable to update duration",
                ROUTE_REFRESH_LOG_CATEGORY,
            )
            return this
        }
        val stepAnnotationsCount = stepPointsSize - 1
        val updatedDuration = mergedDurations
            .drop(previousStepsAnnotationsCount)
            .take(stepAnnotationsCount)
            .sum()
        result.add(step.toBuilder().duration(updatedDuration).build())
        previousStepsAnnotationsCount += stepAnnotationsCount
    }
    return result
}

private fun buildNewWaypoints(
    oldWaypoints: List<DirectionsWaypoint>?,
    updatedWaypoints: List<DirectionsWaypoint?>?,
): List<DirectionsWaypoint>? {
    if (oldWaypoints == null || updatedWaypoints == null) {
        return oldWaypoints
    }
    return oldWaypoints.mapIndexed { index, oldWaypoint ->
        updatedWaypoints.getOrNull(index) ?: oldWaypoint
    }
}

private fun DirectionsRoute.Builder.updateRouteDurationBasedOnLegsDurationAndChargeTime(
    updateLegs: List<RouteLeg>?,
    waypoints: List<DirectionsWaypoint?>?,
): DirectionsRoute.Builder {
    updateLegs ?: return this
    var result = 0.0
    for (leg in updateLegs) {
        result += leg.duration() ?: return this
    }
    waypoints?.forEach { waypoint ->
        waypoint?.unrecognizedJsonProperties
            ?.get("metadata")
            ?.asJsonObject
            ?.get("charge_time")?.asDouble?.let { chargeTime ->
                result += chargeTime
            }
    }
    duration(result)
    return this
}
