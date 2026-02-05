@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.operations

import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.Notification
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.bindgen.DataRef
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.PointAsCoordinatesTypeAdapter
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.route.AnnotationsRefresher
import com.mapbox.navigation.base.internal.route.ClosuresRefresher
import com.mapbox.navigation.base.internal.route.IncidentsRefresher
import com.mapbox.navigation.base.internal.route.NavigationRouteData
import com.mapbox.navigation.base.internal.route.NotificationsRefresher
import com.mapbox.navigation.base.internal.route.WaypointsParser
import com.mapbox.navigation.base.internal.route.parsing.models.ParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.RouteModelParsingResult
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.internal.route.size
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.toByteArray
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouteRefreshMetadata
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.RouteInterface
import kotlin.collections.set

private const val LOG_CATEGORY = "JAVA-ROUTES-OPERATIONS"

internal class JavaRouteOperations(
    private val routeData: ParsedRouteData,
    private val overriddenTraffic: CongestionNumericOverride? = null,
) : RouteOperations {

    private val routeId: String = routeData.route.requestUuid() + "#" + routeData.route.routeIndex()

    @WorkerThread
    override fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
    ): Result<RouteUpdate> =
        parseDirectionsRouteRefresh(refreshResponse)
            .onSuccess {
                logD(
                    "Parsed route refresh response for " +
                        "route($routeId)",
                    LOG_CATEGORY,
                )
            }
            .onFailure {
                logD(
                    "Failed to parse route refresh response for " +
                        "route($routeId)",
                    LOG_CATEGORY,
                )
            }
            .map { routeRefresh ->
                val updatedWaypoints = WaypointsParser.parse(
                    routeRefresh.unrecognizedJsonProperties
                        ?.get(Constants.RouteResponse.KEY_WAYPOINTS),
                )
                val refreshTtl = routeRefresh.unrecognizedJsonProperties
                    ?.get(Constants.RouteResponse.KEY_REFRESH_TTL)?.asInt
                val updateLegs = routeData.route.legs()?.mapIndexed { index, routeLeg ->
                    if (index < legIndex) {
                        routeLeg
                    } else {
                        val newAnnotation = routeRefresh.legs()
                            ?.map<RouteLegRefresh, LegAnnotation?> { it.annotation() }
                            ?.getOrNull(index)
                        val startingLegGeometryIndex =
                            if (index == legIndex && legGeometryIndex != null) {
                                legGeometryIndex
                            } else {
                                0
                            }
                        val lastLegRefreshIndex = try {
                            startingLegGeometryIndex + newAnnotation.size() - 1
                        } catch (ex: IllegalArgumentException) {
                            // refresh is only possible if annotations are requested, so this is not expected to happen
                            logE(LOG_CATEGORY) {
                                ex.message ?: "Unknown error"
                            }
                            startingLegGeometryIndex
                        }
                        val mergedAnnotation = AnnotationsRefresher.getRefreshedAnnotations(
                            routeLeg.annotation(),
                            newAnnotation,
                            startingLegGeometryIndex,
                            overriddenTraffic?.takeIf { it.legIndex == index }?.startIndex,
                            overriddenTraffic?.takeIf { it.legIndex == index }?.length,
                        )
                        val mergedIncidents = IncidentsRefresher.getRefreshedRoadObjects(
                            routeLeg.incidents(),
                            routeRefresh.legs()
                                ?.map<RouteLegRefresh, MutableList<Incident>?> { it.incidents() }
                                ?.getOrNull<List<Incident>?>(index),
                            startingLegGeometryIndex,
                            lastLegRefreshIndex,
                        )
                        val mergedClosures = ClosuresRefresher.getRefreshedRoadObjects(
                            routeLeg.closures(),
                            routeRefresh.legs()
                                ?.map<RouteLegRefresh, MutableList<Closure>?> { it.closures() }
                                ?.getOrNull<List<Closure>?>(index),
                            startingLegGeometryIndex,
                            lastLegRefreshIndex,
                        )

                        val mergedNotifications =
                            NotificationsRefresher().getRefreshedNotifications(
                                routeLeg.notifications(),
                                routeRefresh.legs()
                                    ?.map<RouteLegRefresh, MutableList<Notification>?> {
                                        it.notifications()
                                    }
                                    ?.getOrNull<List<Notification>?>(index),
                                startingLegGeometryIndex,
                                lastLegRefreshIndex,
                            )

                        routeLeg.toBuilder()
                            .duration(
                                mergedAnnotation?.duration()?.sumOf { it }
                                    ?: routeLeg.duration(),
                            )
                            .annotation(mergedAnnotation)
                            .incidents(mergedIncidents)
                            .closures(mergedClosures)
                            .notifications(mergedNotifications)
                            .steps(
                                routeLeg.steps()
                                    ?.updateSteps(routeData.route, mergedAnnotation),
                            )
                            .build()
                    }
                }
                val newWaypoints = buildNewWaypoints(routeData.routesWaypoint, updatedWaypoints)
                val updatedDirectionRoute = routeData.route.toBuilder()
                    .legs(updateLegs)
                    .waypoints(buildNewWaypoints(routeData.route.waypoints(), updatedWaypoints))
                    .updateRouteDurationBasedOnLegsDurationAndChargeTime(
                        updateLegs = updateLegs,
                        waypoints = newWaypoints,
                    )
                    .updateRefreshTtl(routeData.route.unrecognizedJsonProperties, refreshTtl)
                    .build()

                val refreshedWaypoints =
                    buildNewWaypoints(routeData.routesWaypoint, updatedWaypoints)
                val newExpirationTimeElapsedSeconds = if (refreshTtl != null) {
                    OptionallyRefreshedData.Updated<Long?>(
                        refreshTtl.plus(
                            responseTimeElapsedSeconds,
                        ),
                    )
                } else {
                    OptionallyRefreshedData.NoUpdates()
                }

                val refreshedRouteData = routeData.copy(
                    route = updatedDirectionRoute,
                    routesWaypoint = refreshedWaypoints,
                )
                RouteUpdate(
                    RouteModelParsingResult(
                        refreshedRouteData,
                        JavaRouteOperations(
                            refreshedRouteData,
                            overriddenTraffic,
                        ),
                    ),
                    routeRefreshMetadata = RouteRefreshMetadata(isUpToDate = true),
                    newExpirationTimeElapsedSeconds = newExpirationTimeElapsedSeconds,
                    overriddenTraffic = OptionallyRefreshedData.NoUpdates(),
                )
            }

    override fun clientSideRouteUpdate(
        directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
        waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
        overriddenTraffic: CongestionNumericOverride?,
        routeRefreshMetadata: RouteRefreshMetadata?,
    ): Result<RouteUpdate> {
        val refreshedRoute = routeData.route.directionsRouteBlock()
        val waypoints = routeData.routesWaypoint.waypointsBlock()

        val refreshedRouteData = routeData.copy(
            route = refreshedRoute,
            routesWaypoint = waypoints,
        )
        return Result.success(
            RouteUpdate(
                RouteModelParsingResult(
                    refreshedRouteData,
                    JavaRouteOperations(
                        refreshedRouteData,
                        overriddenTraffic,
                    ),
                ),
                routeRefreshMetadata,
                // client side refresh never changes route expiration
                OptionallyRefreshedData.NoUpdates(),
                OptionallyRefreshedData.Updated(overriddenTraffic),
            ),
        )
    }

    override fun toDirectionsRefreshResponse(): Result<DirectionsRefreshResponse> {
        val refreshedLegs: List<RouteLegRefresh>? = routeData.route.legs()?.map { routeLeg ->
            RouteLegRefresh.builder()
                .annotation(routeLeg.annotation())
                .incidents(routeLeg.incidents())
                .notifications(routeLeg.notifications())
                .build()
        }
        val refreshedWaypoints = routeData.routesWaypoint
        val refreshRoute: DirectionsRouteRefresh = DirectionsRouteRefresh.builder()
            .legs(refreshedLegs)
            .unrecognizedJsonProperties(
                refreshedWaypoints?.let { waypoints ->
                    mapOf(
                        Constants.RouteResponse.KEY_WAYPOINTS to JsonArray().apply {
                            waypoints.forEach { waypoint ->
                                add(JsonParser.parseString(waypoint.toJson()))
                            }
                        },
                    )
                },
            )
            .build()

        return Result.success(
            DirectionsRefreshResponse.builder()
                .code("200")
                .route(refreshRoute)
                .build(),
        )
    }

    override fun serialize(navigationRouteData: NavigationRouteData): Result<String> =
        Result.runCatching {
            val gson = GsonBuilder()
                .registerTypeAdapterFactory(DirectionsAdapterFactory.create())
                .registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
                .create()

            val state = SerialisationState(
                routeData.route,
                routeData.routeOptions,
                routeData.routesWaypoint,
                routeData.routeIndex,
                routeData.routerOrigin.mapToNativeRouteOrigin(),
                navigationRouteData.unavoidableClosures, // TODO: don't loose unavoidable closures
                routeData.responseOriginAPI,
                routeData.requestUUID ?: "unknown-uuid",
                // TODO: it doesn't make sense to pass expiration time as elapsed seconds between devices
                // https://mapbox.atlassian.net/browse/NAVAND-6775
                navigationRouteData.expirationTimeElapsedSeconds,
                // always consider restored route as outdated
                RouteRefreshMetadata(isUpToDate = false),
            )
            gson.toJson(state)
        }

    companion object {
        fun deserializeFrom(value: String): Result<NavigationRoute> {
            return Result.runCatching {
                val gson = GsonBuilder()
                    .registerTypeAdapterFactory(DirectionsAdapterFactory.create())
                    .registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
                    .create()
                val state = gson.fromJson(value, SerialisationState::class.java)
                val nativeRoute = restoreNativeRoute(state)
                NavigationRoute(
                    directionsRoute = state.directionRoute,
                    waypoints = state.waypoints,
                    routeOptions = state.routeOptions,
                    nativeRoute = nativeRoute,
                    expirationTimeElapsedSeconds = state.expirationTimeElapsedSeconds,
                    responseOriginAPI = state.responseOriginAPI,
                    overriddenTraffic = null,
                    unavoidableClosures = state.unavoidableClosures,
                    routeRefreshMetadata = RouteRefreshMetadata(isUpToDate = false),
                    operations = JavaRouteOperations(
                        ParsedRouteData(
                            state.directionRoute,
                            state.waypoints,
                            state.responseUUID,
                            state.routeOptions,
                            state.routeIndex,
                            state.routerOrigin.mapToSdkRouteOrigin(),
                            state.responseOriginAPI,
                        ),
                    ),
                )
            }
        }
    }
}

private fun parseDirectionsRouteRefresh(
    dataRef: DataRef,
): Result<DirectionsRouteRefresh> {
    return Result.runCatching {
        /**
         * TODO support DirectionsRefreshResponse creation from DataRef.
         * See DirectionsResponse.fromJson(reader)
         */
        val route =
            DirectionsRefreshResponse.fromJson(dataRef.toByteArray().decodeToString()).route()
        route ?: throw IllegalStateException("no route refresh returned")
    }
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
                LOG_CATEGORY,
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

@Keep
private data class SerialisationState(
    val directionRoute: DirectionsRoute,
    val routeOptions: RouteOptions,
    val waypoints: List<DirectionsWaypoint>?,
    val routeIndex: Int,
    val routerOrigin: com.mapbox.navigator.RouterOrigin,
    val unavoidableClosures: List<List<Closure>>,
    @ResponseOriginAPI
    val responseOriginAPI: String,
    val responseUUID: String,
    val expirationTimeElapsedSeconds: Long?,
    val routeRefreshMetadata: RouteRefreshMetadata?,
)

// TODO: adopt native serialization/deserialization NAVAND-1764
private fun restoreNativeRoute(state: SerialisationState): RouteInterface {
    val directionsResponse = DirectionsResponse.builder()
        .routes(
            MutableList(state.routeIndex + 1) {
                if (it == state.routeIndex) {
                    state.directionRoute
                } else {
                    fakeDirectionsRoute
                }
            },
        )
        .waypoints(
            if (state.routeOptions.waypointsPerRoute() != true) {
                state.waypoints
            } else {
                null
            },
        )
        .code("Ok")
        .uuid(state.responseUUID)
        .build()

    val nativeRoute = SDKRouteParser.default.parseDirectionsResponse(
        directionsResponse.toJson(),
        state.routeOptions.toUrl("***").toString(),
        state.routerOrigin.mapToSdkRouteOrigin(),
    ).value!![state.routeIndex]
    return nativeRoute
}

private val fakeDirectionsRoute: DirectionsRoute by lazy {
    val fakeIntersection = StepIntersection.builder()
        .rawLocation(doubleArrayOf(0.0, 0.0))
        .build()
    val fakeManeuver = StepManeuver.builder()
        .rawLocation(doubleArrayOf(0.0, 0.0))
        .type(StepManeuver.ARRIVE)
        .build()
    val fakeSteps = listOf(
        LegStep.builder()
            .distance(0.0)
            .duration(0.0)
            .mode("driving")
            .maneuver(fakeManeuver)
            .weight(0.0)
            .geometry(
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(0.0, 0.0),
                        Point.fromLngLat(0.0, 0.0),
                    ),
                ).toPolyline(6),
            )
            .intersections(listOf(fakeIntersection))
            .build(),
    )
    val fakeLegs = listOf(
        RouteLeg.builder()
            .steps(fakeSteps)
            .build(),
    )
    DirectionsRoute.builder()
        .distance(0.0)
        .duration(0.0)
        .legs(fakeLegs)
        .build()
}
