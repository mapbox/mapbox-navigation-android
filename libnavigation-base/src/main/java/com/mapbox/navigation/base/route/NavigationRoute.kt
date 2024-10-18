@file:JvmName("NavigationRouteEx")
@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.route

import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.PointAsCoordinatesTypeAdapter
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.toUpcomingRoadObjects
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.internal.utils.mapToSdk
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.toReader
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

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
class NavigationRoute private constructor(
    val directionsRoute: DirectionsRoute,
    val waypoints: List<DirectionsWaypoint>?,
    internal val routeOptions: RouteOptions,
    internal val nativeRoute: RouteInterface,
    internal val unavoidableClosures: List<List<Closure>>,
    internal var expirationTimeElapsedSeconds: Long?,
    internal val overriddenTraffic: CongestionNumericOverride? = null,
    @ExperimentalMapboxNavigationAPI
    @ResponseOriginAPI
    val responseOriginAPI: String,
    @ExperimentalMapboxNavigationAPI
    val routeRefreshMetadata: RouteRefreshMetadata?,
) {

    internal constructor(
        directionsRoute: DirectionsRoute,
        waypoints: List<DirectionsWaypoint>?,
        routeOptions: RouteOptions,
        nativeRoute: RouteInterface,
        expirationTimeElapsedSeconds: Long?,
        @ResponseOriginAPI
        responseOriginAPI: String,
        overriddenTraffic: CongestionNumericOverride? = null,
    ) : this(
        directionsRoute,
        waypoints,
        routeOptions,
        nativeRoute,
        directionsRoute.legs()
            ?.map { leg -> leg.closures().orEmpty() }
            .orEmpty(),
        expirationTimeElapsedSeconds,
        overriddenTraffic,
        responseOriginAPI,
        null,
    )

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
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(DirectionsAdapterFactory.create())
            .registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
            .create()

        val state = SerialisationState(
            directionsRoute,
            routeOptions,
            waypoints,
            routeIndex,
            routerOrigin,
            unavoidableClosures,
            responseOriginAPI,
            responseUUID,
            expirationTimeElapsedSeconds,
            routeRefreshMetadata,
        )
        return gson.toJson(state)
    }

    companion object {

        private const val LOG_CATEGORY = "NavigationRoute"

        /**
         * Deserializes instance of [NavigationRoute] from string so that it could be passed to a
         * different application which uses the same version of Navigation Core Framework.
         * Warning: compatibility is guaranteed only between the same versions of Core Framework
         * @return string representation of navigation route
         */
        @WorkerThread
        internal fun deserializeFrom(value: String): Expected<Throwable, NavigationRoute> {
            return try {
                val gson = GsonBuilder()
                    .registerTypeAdapterFactory(DirectionsAdapterFactory.create())
                    .registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
                    .create()

                val state = gson.fromJson(value, SerialisationState::class.java)

                val nativeRoute = restoreNativeRoute(state)

                val route = NavigationRoute(
                    state.directionRoute,
                    state.waypoints,
                    state.routeOptions,
                    nativeRoute,
                    state.unavoidableClosures,
                    state.expirationTimeElapsedSeconds,
                    responseOriginAPI = state.responseOriginAPI,
                    overriddenTraffic = null,
                    routeRefreshMetadata = state.routeRefreshMetadata,
                )
                ExpectedFactory.createValue(route)
            } catch (t: Throwable) {
                ExpectedFactory.createError(t)
            }
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponse].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponse response to be parsed into [NavigationRoute]s
         * @param routeOptions options used to generate the [directionsResponse]
         * @param routerOrigin origin where route was fetched from
         */
        @JvmStatic
        internal fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            @RouterOrigin
            routerOrigin: String,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString(),
                routerOrigin,
                null,
                ResponseOriginAPI.DIRECTIONS_API,
            )
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
                        .build()
                }?.toMutableList()
                val directionsResponse = DirectionsResponse.builder()
                    .routes(directionRoutes ?: emptyList())
                    // TODO: NAVAND-1737 introduce uuid in map matching response model
                    // .uuid(model.uuid())
                    .code(model.code())
                    .message(model.message())
                    .waypoints(
                        model.tracepoints()
                            ?.filterNotNull()
                            ?.filter { it.waypointIndex() != null }
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
                // TODO: NAVAND-1732 parse map matching response in NN without converting it
                // to directions response
                val routesResult = create(
                    directionsResponse,
                    directionsResponse.toJson(),
                    routeOptions,
                    requestUrl,
                    RouterOrigin.ONLINE,
                    // map matched routes don't expire
                    responseTimeElapsedSeconds = Long.MAX_VALUE,
                    ResponseOriginAPI.MAP_MATCHING_API,
                )
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

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponseJson].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponseJson response to be parsed into [NavigationRoute]s
         * @param routeRequestUrl URL used to generate the [directionsResponseJson]
         * @param routerOrigin origin where route was fetched from
         *
         * @throws DirectionsResponseParsingException if `directionsResponseJson` is invalid
         */
        @JvmStatic
        internal fun create(
            directionsResponseJson: String,
            routeRequestUrl: String,
            @RouterOrigin
            routerOrigin: String,
        ): List<NavigationRoute> {
            val directionsResponse = try {
                DirectionsResponse.fromJson(directionsResponseJson)
            } catch (ex: Throwable) {
                logE(LOG_CATEGORY) { "Error parsing directions response" }
                throw DirectionsResponseParsingException(ex)
            }
            return create(
                directionsResponse = directionsResponse,
                directionsResponseJson = directionsResponseJson,
                routeOptions = RouteOptions.fromUrl(URL(routeRequestUrl)),
                routeOptionsUrlString = routeRequestUrl,
                routerOrigin = routerOrigin,
                responseTimeElapsedSeconds = null,
                ResponseOriginAPI.DIRECTIONS_API,
            )
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponseJson].
         *
         * This function parallelizes response parsing and native navigator parsing.
         *
         * @param directionsResponseJson response to be parsed into [NavigationRoute]s
         * @param routeRequestUrl URL used to generate the [directionsResponseJson]
         */
        internal suspend fun createAsync(
            directionsResponseJson: DataRef,
            routeRequestUrl: String,
            @RouterOrigin routerOrigin: String,
            responseTimeElapsedMillis: Long,
            routeParser: SDKRouteParser = SDKRouteParser.default,
        ): RoutesResponse {
            logI("NavigationRoute.createAsync is called", LOG_CATEGORY)

            return coroutineScope {
                data class ParseResult<T>(
                    val value: T,
                    val waitMillis: Long,
                    val parseMillis: Long,
                    val threadName: String = "",
                )

                fun currentElapsedMillis() = Time.SystemClockImpl.millis()

                val deferredResponseParsing = async(ThreadController.DefaultDispatcher) {
                    val startElapsedMillis = currentElapsedMillis()
                    val waitMillis = startElapsedMillis - responseTimeElapsedMillis

                    directionsResponseJson.toDirectionsResponse().let {
                        val parseMillis = currentElapsedMillis() - startElapsedMillis
                        val parseThread = Thread.currentThread().name
                        logD(
                            "parsed directions response to java model for ${it.uuid()}, " +
                                "parse time ${parseMillis}ms",
                            LOG_CATEGORY,
                        )
                        ParseResult(it, waitMillis, parseMillis, parseThread)
                    }
                }
                val deferredNativeParsing = async(ThreadController.DefaultDispatcher) {
                    val startElapsedMillis = currentElapsedMillis()
                    val waitMillis = startElapsedMillis - responseTimeElapsedMillis

                    routeParser.parseDirectionsResponse(
                        directionsResponseJson,
                        routeRequestUrl,
                        routerOrigin,
                    ).let {
                        val parseMillis = currentElapsedMillis() - startElapsedMillis
                        logD(
                            "parsed directions response to RouteInterface " +
                                "for ${it.value?.firstOrNull()?.responseUuid}, " +
                                "parse time ${parseMillis}ms",
                            LOG_CATEGORY,
                        )
                        ParseResult(it, waitMillis, parseMillis)
                    }
                }
                val deferredRouteOptionsParsing = async(ThreadController.DefaultDispatcher) {
                    val startElapsedMillis = currentElapsedMillis()
                    val waitMillis = startElapsedMillis - responseTimeElapsedMillis

                    RouteOptions.fromUrl(URL(routeRequestUrl)).let {
                        val parseMillis = currentElapsedMillis() - startElapsedMillis
                        logD(LOG_CATEGORY) {
                            "parsed request url to RouteOptions: ${it.toUrl("***")}, " +
                                "parse time ${parseMillis}ms"
                        }
                        ParseResult(it, waitMillis, parseMillis)
                    }
                }

                val nativeParseResult = deferredNativeParsing.await()
                val responseParseResult = deferredResponseParsing.await()
                val routeOptionsResult = deferredRouteOptionsParsing.await()

                create(
                    nativeParseResult.value,
                    responseParseResult.value,
                    routeOptionsResult.value,
                    responseTimeElapsedMillis.milliseconds.inWholeSeconds,
                    ResponseOriginAPI.DIRECTIONS_API,
                ).let { routes ->
                    val totalParseMillis = responseParseResult.parseMillis +
                        nativeParseResult.parseMillis + routeOptionsResult.parseMillis

                    logD(
                        "NavigationRoute.createAsync finished " +
                            "for ${routes.firstOrNull()?.responseUUID}," +
                            "total parse time ${totalParseMillis}ms",
                        LOG_CATEGORY,
                    )

                    RoutesResponse(
                        routes = routes,
                        meta = RoutesResponse.Metadata(
                            createdAtElapsedMillis = currentElapsedMillis(),
                            responseWaitMillis = responseParseResult.waitMillis,
                            responseParseMillis = responseParseResult.parseMillis,
                            responseParseThread = responseParseResult.threadName,
                            nativeWaitMillis = nativeParseResult.waitMillis,
                            nativeParseMillis = nativeParseResult.parseMillis,
                            routeOptionsWaitMillis = routeOptionsResult.waitMillis,
                            routeOptionsParseMillis = routeOptionsResult.parseMillis,
                        ),
                    )
                }
            }
        }

        internal fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            routeParser: SDKRouteParser,
            @RouterOrigin routerOrigin: String,
            responseTimeElapsedSeconds: Long?,
            @ResponseOriginAPI responseOriginAPI: String = ResponseOriginAPI.DIRECTIONS_API,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString(),
                routerOrigin,
                responseTimeElapsedSeconds,
                responseOriginAPI,
                routeParser,
            )
        }

        private fun create(
            directionsResponse: DirectionsResponse,
            directionsResponseJson: String,
            routeOptions: RouteOptions,
            routeOptionsUrlString: String,
            @RouterOrigin
            routerOrigin: String,
            responseTimeElapsedSeconds: Long?,
            @ResponseOriginAPI
            responseOriginAPI: String,
            routeParser: SDKRouteParser = SDKRouteParser.default,
        ): List<NavigationRoute> {
            return routeParser.parseDirectionsResponse(
                directionsResponseJson,
                routeOptionsUrlString,
                routerOrigin,
            ).run {
                create(
                    this,
                    directionsResponse,
                    routeOptions,
                    responseTimeElapsedSeconds,
                    responseOriginAPI = responseOriginAPI,
                )
            }
        }

        private fun create(
            expected: Expected<String, List<RouteInterface>>,
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            responseTimeElapsedSeconds: Long?,
            @ResponseOriginAPI
            responseOriginAPI: String,
        ): List<NavigationRoute> {
            return expected.fold({ error ->
                logE("NavigationRoute", "Failed to parse a route. Reason: $error")
                listOf()
            }, { value ->
                value
            },).mapIndexed { index, routeInterface ->
                NavigationRoute(
                    getDirectionsRoute(directionsResponse, index, routeOptions),
                    getDirectionsWaypoint(directionsResponse, index, routeOptions),
                    routeOptions,
                    routeInterface,
                    ifNonNull(
                        directionsResponse.routes().getOrNull(index)?.refreshTtl(),
                        responseTimeElapsedSeconds,
                    ) { refreshTtl, responseTimeElapsedSeconds ->
                        refreshTtl + responseTimeElapsedSeconds
                    },
                    responseOriginAPI = responseOriginAPI,
                    overriddenTraffic = null,
                )
            }
        }

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
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationRoute

        if (id != other.id) return false
        if (directionsRoute != other.directionsRoute) return false
        if (waypoints != other.waypoints) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + directionsRoute.hashCode()
        result = 31 * result + waypoints.hashCode()
        return result
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
    ): NavigationRoute = NavigationRoute(
        directionsRoute,
        waypoints,
        routeOptions,
        nativeRoute,
        this.unavoidableClosures,
        expirationTimeElapsedSeconds,
        overriddenTraffic,
        this.responseOriginAPI,
        routeRefreshMetadata,
    )

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
}

internal fun RouteInterface.toNavigationRoute(
    responseTimeElapsedSeconds: Long,
    directionsResponse: DirectionsResponse,
): NavigationRoute {
    val refreshTtl = directionsResponse.routes().getOrNull(routeIndex)?.refreshTtl()
    val routeOptions = RouteOptions.fromUrl(URL(requestUri))
    return NavigationRoute(
        routeOptions = routeOptions,
        directionsRoute = getDirectionsRoute(directionsResponse, routeIndex, routeOptions),
        waypoints = getDirectionsWaypoint(directionsResponse, routeIndex, routeOptions),
        nativeRoute = this,
        expirationTimeElapsedSeconds = refreshTtl?.plus(responseTimeElapsedSeconds),
        // Continuous alternatives are always from Directions API
        responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
        overriddenTraffic = null,
    )
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

internal fun DataRef.toDirectionsResponse(): DirectionsResponse {
    return this.toReader().use { reader ->
        DirectionsResponse.fromJson(reader)
    }
}

private fun getDirectionsRoute(
    response: DirectionsResponse,
    routeIndex: Int,
    routeOptions: RouteOptions,
): DirectionsRoute =
    response.routes()[routeIndex].toBuilder()
        .requestUuid(response.uuid())
        .routeIndex(routeIndex.toString())
        .routeOptions(routeOptions)
        .build()

private fun getDirectionsWaypoint(
    directionsResponse: DirectionsResponse,
    routeIndex: Int,
    routeOptions: RouteOptions,
): List<DirectionsWaypoint>? {
    return if (routeOptions.waypointsPerRoute() == true) {
        directionsResponse.routes()[routeIndex].waypoints()
    } else {
        directionsResponse.waypoints()
    }
}
