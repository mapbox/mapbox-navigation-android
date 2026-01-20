@file:JvmName("NavigationRouteEx")
@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.route

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.toUpcomingRoadObjects
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.NavigationRouteData
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.operations.JavaRouteOperations
import com.mapbox.navigation.base.internal.route.operations.RouteOperations
import com.mapbox.navigation.base.internal.route.operations.RouteUpdate
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.createResponseParsingResult
import com.mapbox.navigation.base.internal.utils.mapToSdk
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.RouteInterface
import java.net.URL

/**
 * Wraps a route object used across the Navigation SDK features.
 * @param directionsRoute [DirectionsRoute] that this [NavigationRoute] represents
 * @param waypoints waypoints associated with the route.
 * Nav SDK retrieves [waypoints] from various sources depending on the situation,
 * such as [DirectionsRoute], [DirectionsResponse], or [MapMatchingResponse].
 * Prefer using [waypoints] instead of [DirectionsRoute.waypoints] from [directionsRoute].
 * @param responseOriginAPI describes API which generated data for [NavigationRoute].
 * @param overriddenTraffic describes a segment of [NavigationRoute] with overridden
 * @param routeRefreshMetadata contains data that describes refreshing of this route object.
 * The field is null until the first refresh.
 */
class NavigationRoute internal constructor(
    val directionsRoute: DirectionsRoute,
    val waypoints: List<DirectionsWaypoint>?,
    // TODO: NAVAND-1732, receive via nativeRoute.getMapboxAPI() instead of storing
    @ExperimentalMapboxNavigationAPI
    @ResponseOriginAPI
    val responseOriginAPI: String,
    @ExperimentalMapboxNavigationAPI
    val routeRefreshMetadata: RouteRefreshMetadata? = null,
    internal val routeOptions: RouteOptions,
    internal val nativeRoute: RouteInterface,
    // TODO: NAVAND-6774
    internal var expirationTimeElapsedSeconds: Long?,
    private val operations: RouteOperations,
    // TODO: NAVAND-6774
    internal val unavoidableClosures: List<List<Closure>> = directionsRoute.legs()
        ?.map { leg -> leg.closures().orEmpty() }
        .orEmpty(),
    internal val overriddenTraffic: CongestionNumericOverride? = null,
) {
    /**
     * The index of the route that this wrapper tracks from the collection of routes returned
     * in the original response.
     */
    val routeIndex: Int get() = nativeRoute.routeIndex

    /**
     * Id of the response from which this [NavigationRoute] is created
     */
    val responseUUID: String get() = nativeRoute.responseUuid

    // [routeOptions] are hidden because they can't represent request to
    // Mapbox Map Matching API.
    // Users in general don't need route options, but max charge value is important
    // for one customer.
    // This property is experimental because of assumption that we will create a
    // route request structure which can represent requests to both Mapbox Directions
    // API and Mapbox Map Matching API.
    // See follow up work in https://mapbox.atlassian.net/browse/NAVAND-1729
    /**
     * Maximum possible charge of vehicle this route was requested for.
     * It's null for non ev routes.
     */
    @ExperimentalMapboxNavigationAPI
    val evMaxCharge: Int?
        get() {
            return try {
                routeOptions
                    .getUnrecognizedProperty("ev_max_charge")
                    ?.asInt
            } catch (t: Throwable) {
                null
            }
        }

    /**
     * Serialises instance of [NavigationRoute] into string. Use [deserializeFrom] to deserialize
     * string into [NavigationRoute] back.
     * Warning: compatibility is guaranteed only between the same versions of Core Framework
     * @return string representation of navigation route
     */
    @WorkerThread
    internal fun serialize(): String {
        return operations.serialize(
            NavigationRouteData(
                unavoidableClosures,
                expirationTimeElapsedSeconds,
            ),
        ).getOrThrow()
    }

    @WorkerThread
    internal fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
    ): Result<NavigationRoute> {
        return operations.refresh(
            refreshResponse,
            legIndex,
            legGeometryIndex,
            responseTimeElapsedSeconds,
        ).map {
            refresh(it)
        }
    }

    internal fun clientSideUpdate(
        directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
        waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
        overriddenTraffic: CongestionNumericOverride?,
        routeRefreshMetadata: RouteRefreshMetadata?,
    ): Result<NavigationRoute> {
        return operations.clientSideRouteUpdate(
            directionsRouteBlock,
            waypointsBlock,
            overriddenTraffic,
            routeRefreshMetadata,
        ).map { refresh(it) }
    }

    internal fun toDirectionsRefreshResponse() = operations.toDirectionsRefreshResponse()

    private fun refresh(route: RouteUpdate): NavigationRoute = this.copy(
        directionsRoute = route.routeModelsParsingResult.data.route,
        waypoints = route.routeModelsParsingResult.data.routesWaypoint,
        operations = route.routeModelsParsingResult.operations,
        routeRefreshMetadata = route.routeRefreshMetadata,
        expirationTimeElapsedSeconds = route.newExpirationTimeElapsedSeconds.update(
            expirationTimeElapsedSeconds,
        ),
        overriddenTraffic = route.overriddenTraffic.update(overriddenTraffic),
    )

    companion object {
        /**
         * Deserializes instance of [NavigationRoute] from string so that it could be passed to a
         * different application which uses the same version of Navigation Core Framework.
         * Warning: compatibility is guaranteed only between the same versions of Core Framework
         * @return string representation of navigation route
         */
        @WorkerThread
        internal fun deserializeFrom(value: String): Expected<Throwable, NavigationRoute> {
            // TODO: https://mapbox.atlassian.net/browse/NAVAND-6775
            return JavaRouteOperations.deserializeFrom(value).map {
                ExpectedFactory.createValue<Throwable, NavigationRoute>(it)
            }.getOrElse {
                ExpectedFactory.createError(it)
            }
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [mapMatchingResponse].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param mapMatchingResponse response to be parsed into [NavigationRoute]s
         * @param requestUrl url which were used to request [mapMatchingResponse]
         */
        @ExperimentalPreviewMapboxNavigationAPI
        @WorkerThread
        internal fun createMatchedRoutes(
            mapMatchingResponse: String,
            requestUrl: String,
        ): Expected<Throwable, List<MapMatchingMatch>> {
            return try {
                val model = MapMatchingResponse.fromJson(mapMatchingResponse)
                val routeOptions = RouteOptions.fromUrl(URL(requestUrl))
                val directionRoutes = model.matchings()?.mapIndexed { index, matching ->
                    matching.toDirectionRoute()
                        .toBuilder()
                        .routeIndex("$index")
                        .routeOptions(routeOptions)
                        .waypoints(
                            model.tracepoints()
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
                val directionsResponse = DirectionsResponse.builder()
                    .routes(directionRoutes ?: emptyList())
                    // TODO: NAVAND-1737 introduce uuid in map matching response model
                    // .uuid(model.uuid())
                    .code(model.code())
                    .message(model.message())
                    .build()
                // TODO: NAVAND-1732 parse map matching response in NN without converting it
                // to directions response
                val routesResult = SDKRouteParser.default.parseDirectionsResponse(
                    directionsResponse.toJson(),
                    requestUrl,
                    RouterOrigin.ONLINE,
                    // map matched routes don't expire
                ).run {
                    create(
                        this,
                        createResponseParsingResult(
                            directionsResponse,
                            routeOptions,
                            RouterOrigin.ONLINE,
                            ResponseOriginAPI.MAP_MATCHING_API,
                        ),
                        responseTimeElapsedSeconds = Long.MAX_VALUE,
                        responseOriginAPI = ResponseOriginAPI.MAP_MATCHING_API,
                    )
                }
                val matchesResult = routesResult.mapIndexed { index, navigationRoute ->
                    MapMatchingMatch(
                        navigationRoute,
                        model.matchings()!![index].confidence(),
                    )
                }
                ExpectedFactory.createValue(matchesResult)
            } catch (t: Throwable) {
                ExpectedFactory.createError(t)
            }
        }

        internal fun create(
            expected: Expected<String, List<RouteInterface>>,
            directionsResponseParsingResult: DirectionsResponseParsingResult,
            responseTimeElapsedSeconds: Long?,
            @ResponseOriginAPI
            responseOriginAPI: String,
        ): List<NavigationRoute> {
            return expected.fold(
                { error ->
                    logE("NavigationRoute", "Failed to parse a route. Reason: $error")
                    listOf()
                },
                { value ->
                    value
                },
            ).mapIndexed { index, routeInterface ->
                NavigationRoute(
                    directionsRoute = directionsResponseParsingResult
                        .routesParsingResult[index].data.route,
                    waypoints = directionsResponseParsingResult
                        .routesParsingResult[index].data.routesWaypoint,
                    routeOptions = directionsResponseParsingResult.routeOptions,
                    nativeRoute = routeInterface,
                    expirationTimeElapsedSeconds = ifNonNull(
                        directionsResponseParsingResult
                            .routesParsingResult[index].data.route.refreshTtl(),
                        responseTimeElapsedSeconds,
                    ) { refreshTtl, responseTimeElapsedSeconds ->
                        refreshTtl + responseTimeElapsedSeconds
                    },
                    responseOriginAPI = responseOriginAPI,
                    overriddenTraffic = null,
                    operations = directionsResponseParsingResult
                        .routesParsingResult[index].operations,
                )
            }
        }
    }

    /**
     * Unique local identifier of the route instance.
     *
     * For routes which contain server-side UUID it's equal to: `UUID + "#" + routeIndex`, for example: `d77PcddF8rhGUc3ORYGfcwcDfS_8QW6r1iXugXD0HOgmr9CWL8wn0g==#0`.
     * For routes which do not have the server-side UUID, for example routes which were generated onboard, it's equal to: `"local@" + generateUuid() + "#" + routeIndex`, for example: `local@84438c3e-f608-47e9-88cc-cddf341d2fb1#0`.
     *
     * There can be two or more [NavigationRoute]s that have an equal ID but fail the [equals] check.
     * This can occur if a route has been refreshed and contains updated:
     * - [RouteLeg.annotation]
     * - [RouteLeg.incidents]
     * - [RouteLeg.closures]
     * - duration fields
     *
     * If two [NavigationRoute]s do have the same ID, they are guaranteed to be originating from the same route response, and follow the exact same path.
     */
    val id: String = nativeRoute.routeId

    /**
     * Describes which router type generated the route.
     */
    @RouterOrigin
    val origin: String = nativeRoute.routerOrigin.mapToSdkRouteOrigin()

    /**
     * Returns a list of [UpcomingRoadObject] present in a route.
     */
    val upcomingRoadObjects = nativeRoute.routeInfo.alerts.toUpcomingRoadObjects()

    internal val nativeWaypoints: List<Waypoint> = nativeRoute.waypoints.mapToSdk()

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
        PerformanceTracker.trackPerformanceSync("NavRoute#equals") {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NavigationRoute

            if (id != other.id) return false
            if (directionsRoute != other.directionsRoute) return false
            if (waypoints != other.waypoints) return false

            return true
        }
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        PerformanceTracker.trackPerformanceSync("NavRoute#hashCode") {
            var result = id.hashCode()
            result = 31 * result + directionsRoute.hashCode()
            result = 31 * result + waypoints.hashCode()
            return result
        }
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationRoute(id=$id)"
    }

    internal fun copy(
        directionsRoute: DirectionsRoute = this.directionsRoute,
        waypoints: List<DirectionsWaypoint>? = this.waypoints,
        routeOptions: RouteOptions = this.routeOptions,
        nativeRoute: RouteInterface = this.nativeRoute,
        overriddenTraffic: CongestionNumericOverride? = this.overriddenTraffic,
        expirationTimeElapsedSeconds: Long? = this.expirationTimeElapsedSeconds,
        routeRefreshMetadata: RouteRefreshMetadata? = this.routeRefreshMetadata,
        operations: RouteOperations = this.operations,
    ): NavigationRoute = NavigationRoute(
        directionsRoute = directionsRoute,
        waypoints = waypoints,
        routeOptions = routeOptions,
        nativeRoute = nativeRoute,
        unavoidableClosures = this.unavoidableClosures,
        expirationTimeElapsedSeconds = expirationTimeElapsedSeconds,
        overriddenTraffic = overriddenTraffic,
        responseOriginAPI = this.responseOriginAPI,
        routeRefreshMetadata = routeRefreshMetadata,
        operations = operations,
    )
}
