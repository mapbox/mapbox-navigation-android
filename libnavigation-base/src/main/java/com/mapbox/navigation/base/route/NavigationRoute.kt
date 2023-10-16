@file:JvmName("NavigationRouteEx")

package com.mapbox.navigation.base.route

import android.util.Log
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.toUpcomingRoadObjects
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.DirectionsRouteMissingConditionsCheck
import com.mapbox.navigation.base.internal.utils.RoutesParsingQueue
import com.mapbox.navigation.base.internal.utils.mapToSdk
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.ByteBuffer

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
    val directionsRoute: DirectionsRoute,
    internal val nativeRoute: RouteInterface,
    internal val unavoidableClosures: List<List<Closure>>,
    internal var expirationTimeElapsedSeconds: Long?,
) {

    internal constructor(
        directionsResponse: DirectionsResponse,
        routeIndex: Int,
        routeOptions: RouteOptions,
        directionsRoute: DirectionsRoute,
        nativeRoute: RouteInterface,
        expirationTimeElapsedSeconds: Long?,
    ) : this(
        directionsResponse,
        routeIndex,
        routeOptions,
        directionsRoute,
        nativeRoute,
        directionsResponse.routes().getOrNull(routeIndex)?.legs()
            ?.map { leg -> leg.closures().orEmpty() }
            .orEmpty(),
        expirationTimeElapsedSeconds,
    )



    companion object {

        private const val LOG_CATEGORY = "NavigationRoute"

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponse].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponse response to be parsed into [NavigationRoute]s
         * @param routeOptions options used to generate the [directionsResponse]
         */
        @Deprecated(
            "Navigation route requires RouterOrigin. " +
                "If RouterOrigin is not set uses RouterOrigin.Custom()",
            ReplaceWith("create(directionsResponse, routeOptions, routerOrigin)")
        )
        @JvmStatic
        fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                routeOptions,
                RouterOrigin.Custom()
            )
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
        fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            routerOrigin: RouterOrigin,
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString(),
                routerOrigin,
                null,
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
        @Deprecated(
            "Navigation route requires RouterOrigin. " +
                "If RouterOrigin is not set uses RouterOrigin.Custom()",
            ReplaceWith("create(directionsResponseJson, routeRequestUrl, routerOrigin)")
        )
        @JvmStatic
        fun create(
            directionsResponseJson: String,
            routeRequestUrl: String,
        ): List<NavigationRoute> {
            return create(
                directionsResponseJson,
                routeRequestUrl,
                RouterOrigin.Custom()
            )
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponseJson].
         *
         * Should not be called from UI thread. Contains serialisation and deserialisation under the hood.
         *
         * @param directionsResponseJson response to be parsed into [NavigationRoute]s
         * @param routeRequestUrl URL used to generate the [directionsResponse]
         * @param routerOrigin origin where route was fetched from
         */
        @JvmStatic
        fun create(
            directionsResponseJson: String,
            routeRequestUrl: String,
            routerOrigin: RouterOrigin,
        ): List<NavigationRoute> {
            return create(
                directionsResponse = DirectionsResponse.fromJson(directionsResponseJson),
                directionsResponseJson = directionsResponseJson,
                routeOptions = RouteOptions.fromUrl(URL(routeRequestUrl)),
                routeOptionsUrlString = routeRequestUrl,
                routerOrigin = routerOrigin,
                responseTimeElapsedSeconds = null,
            )
        }

        /**
         * Creates new instances of [NavigationRoute] based on the routes found in the [directionsResponseJson].
         *
         * This function parallelizes response parsing and native navigator parsing.
         *
         * @param directionsResponseJson response to be parsed into [NavigationRoute]s
         * @param routeRequestUrl URL used to generate the [directionsResponse]
         */
        internal suspend fun createAsync(
            directionsResponseJson: DataRef,
            routeRequestUrl: String,
            routerOrigin: RouterOrigin,
            responseTimeElapsedSeconds: Long?,
            routeParser: SDKRouteParser = SDKRouteParser.default
        ): List<NavigationRoute> {
            logI("NavigationRoute.createAsync is called, enqueued", LOG_CATEGORY)
            return RoutesParsingQueue.instance.parseRouteResponse {
                logI("NavigationRoute.createAsync: starting parsing", LOG_CATEGORY)
                coroutineScope {
                    val deferredResponseParsing = async(ThreadController.DefaultDispatcher) {
                        directionsResponseJson.toDirectionsResponse().also {
                            logD(
                                "parsed directions response to java model for ${it.uuid()}",
                                LOG_CATEGORY
                            )
                        }
                    }
                    val deferredNativeParsing = async(ThreadController.DefaultDispatcher) {
                        routeParser.parseDirectionsResponse(
                            directionsResponseJson,
                            routeRequestUrl,
                            routerOrigin,
                        ).also {
                            logD(
                                "parsed directions response to RouteInterface " +
                                    "for ${it.value?.firstOrNull()?.responseUuid}",
                                LOG_CATEGORY
                            )
                        }
                    }
                    val deferredRouteOptionsParsing = async(ThreadController.DefaultDispatcher) {
                        RouteOptions.fromUrl(URL(routeRequestUrl)).also {
                            logD(LOG_CATEGORY) {
                                "parsed request url to RouteOptions: ${it.toUrl("***")}"
                            }
                        }
                    }
                    create(
                        deferredNativeParsing.await(),
                        deferredResponseParsing.await(),
                        deferredRouteOptionsParsing.await(),
                        responseTimeElapsedSeconds,
                    ).also {
                        logD(
                            "NavigationRoute.createAsync finished " +
                                "for ${it.firstOrNull()?.directionsResponse?.uuid()}",
                            LOG_CATEGORY
                        )
                    }
                }
            }
        }

        internal fun create(
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            routeParser: SDKRouteParser,
            routerOrigin: RouterOrigin,
            responseTimeElapsedSeconds: Long?
        ): List<NavigationRoute> {
            return create(
                directionsResponse,
                directionsResponseJson = directionsResponse.toJson(),
                routeOptions,
                routeOptionsUrlString = routeOptions.toUrl("").toString(),
                routerOrigin,
                responseTimeElapsedSeconds,
                routeParser,
            )
        }

        private fun create(
            directionsResponse: DirectionsResponse,
            directionsResponseJson: String,
            routeOptions: RouteOptions,
            routeOptionsUrlString: String,
            routerOrigin: RouterOrigin,
            responseTimeElapsedSeconds: Long?,
            routeParser: SDKRouteParser = SDKRouteParser.default
        ): List<NavigationRoute> {
            return routeParser.parseDirectionsResponse(
                directionsResponseJson,
                routeOptionsUrlString,
                routerOrigin
            ).run {
                create(this, directionsResponse, routeOptions, responseTimeElapsedSeconds)
            }
        }

        private fun create(
            expected: Expected<String, List<RouteInterface>>,
            directionsResponse: DirectionsResponse,
            routeOptions: RouteOptions,
            responseTimeElapsedSeconds: Long?,
        ): List<NavigationRoute> {
            return expected.fold({ error ->
                logE("NavigationRoute", "Failed to parse a route. Reason: $error")
                listOf()
            }, { value ->
                value
            }).mapIndexed { index, routeInterface ->
                val directionResponseWithoutOtherRoutes = directionsResponse.toBuilder().routes(
                    emptyList()
                ).build()
                NavigationRoute(
                    directionResponseWithoutOtherRoutes,
                    index,
                    routeOptions,
                    getDirectionsRoute(directionsResponse, index, routeOptions),
                    routeInterface,
                    ifNonNull(
                        directionsResponse.routes().getOrNull(index)?.refreshTtl(),
                        responseTimeElapsedSeconds
                    ) { refreshTtl, responseTimeElapsedSeconds ->
                        refreshTtl + responseTimeElapsedSeconds
                    }
                )
            }.cache()
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
    val origin: RouterOrigin = nativeRoute.routerOrigin.mapToSdkRouteOrigin()

    /**
     * Returns a list of [UpcomingRoadObject] present in a route.
     */
    val upcomingRoadObjects = nativeRoute.routeInfo.alerts.toUpcomingRoadObjects()

    /**
     * Compatibility function to always access the valid [DirectionsWaypoint]s collection.
     *
     * Returns [DirectionsResponse.waypoints] or [DirectionsRoute.waypoints]
     * depending on whether [RouteOptions.waypointsPerRoute] was set.
     */
    val waypoints: List<DirectionsWaypoint>? by lazy {
        if (routeOptions.waypointsPerRoute() == true) {
            if (directionsRoute.waypoints() != null) {
                directionsRoute.waypoints()
            } else { // TODO: remove this fallback when NN-731 is fixed
                directionsResponse.waypoints()
            }
        } else {
            directionsResponse.waypoints()
        }
    }

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
        directionsResponse: DirectionsResponse = this.directionsResponse,
        routeIndex: Int = this.routeIndex,
        routeOptions: RouteOptions = this.routeOptions,
        nativeRoute: RouteInterface = this.nativeRoute,
        expirationTimeElapsedSeconds: Long? = this.expirationTimeElapsedSeconds,
    ): NavigationRoute = NavigationRoute(
        directionsResponse,
        routeIndex,
        routeOptions,
        directionsRoute,
        nativeRoute,
        this.unavoidableClosures,
        expirationTimeElapsedSeconds,
    ).cache()
}

/**
 * Returns all [NavigationRoute.directionsRoute]s.
 *
 * This is a lossy mapping since the [DirectionsRoute] cannot carry the same amount of information as [NavigationRoute].
 *
 * @throws IllegalStateException when [DirectionsRoute] and [RouteOptions] don't contain enough
 * data to create a new instance of [NavigationRoute] (required for forward compatibility). For
 * instance, EV routing adds additional waypoints and metadata, to handle a route it requires original
 * [DirectionsResponse].
 */
fun List<NavigationRoute>.toDirectionsRoutes(): List<DirectionsRoute> = map {
    it.directionsRoute.also { route ->
        DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(route)
    }
}

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
 *
 * @throws IllegalStateException when [DirectionsRoute] and [RouteOptions] don't contain enough
 * data to create a new instance of [NavigationRoute]. For instance, EV routing adds additional waypoints
 * and metadata, to handle a route it requires original [DirectionsResponse].
 */
@Deprecated(
    "NavigationRoute requires routerOrigin. If RouterOrigin is not set uses RouterOrigin.Custom()",
    ReplaceWith("toNavigationRoutes(routerOrigin)")
)
fun List<DirectionsRoute>.toNavigationRoutes(): List<NavigationRoute> =
    map { it.toNavigationRoute(RouterOrigin.Custom()) }

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
 *
 * @param routerOrigin origin where route was fetched from
 *
 * @throws IllegalStateException when [DirectionsRoute] and [RouteOptions] don't contain enough
 * data to create a new instance of [NavigationRoute]. For instance, EV routing adds additional waypoints
 * and metadata, to handle a route it requires original [DirectionsResponse].
 */
fun List<DirectionsRoute>.toNavigationRoutes(
    routerOrigin: RouterOrigin,
): List<NavigationRoute> = map { it.toNavigationRoute(routerOrigin) }

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
 *
 * @throws IllegalStateException when [DirectionsRoute] and [RouteOptions] don't contain enough
 * data to create a new instance of [NavigationRoute]. For instance, EV routing adds additional waypoints
 * and metadata, to handle a route it requires original [DirectionsResponse].
 */
@Deprecated(
    "NavigationRoute requires routerOrigin. If RouterOrigin is not set uses RouterOrigin.Custom()",
    ReplaceWith("toNavigationRoute(routerOrigin)")
)
fun DirectionsRoute.toNavigationRoute(): NavigationRoute = this.toNavigationRoute(
    SDKRouteParser.default,
    RouterOrigin.Custom(),
    true,
)

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
 *
 * @throws IllegalStateException when [DirectionsRoute] and [RouteOptions] don't contain enough
 * data to create a new instance of [NavigationRoute]. For instance, EV routing adds additional waypoints
 * and metadata, to handle a route it requires original [DirectionsResponse].
 */
fun DirectionsRoute.toNavigationRoute(
    routerOrigin: RouterOrigin,
): NavigationRoute = this.toNavigationRoute(SDKRouteParser.default, routerOrigin, true)

internal fun DirectionsRoute.toNavigationRoute(
    sdkRouteParser: SDKRouteParser,
    routerOrigin: RouterOrigin,
    useCache: Boolean,
): NavigationRoute {
    DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(this)

    if (useCache) {
        RouteCompatibilityCache.getFor(this)?.let {
            return it
        }
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
    return NavigationRoute.create(response, options, sdkRouteParser, routerOrigin, null)[routeIndex]
}

internal fun RouteInterface.toNavigationRoute(responseTimeElapsedSeconds: Long, directionsResponse: DirectionsResponse? = null): NavigationRoute {
    val response = if (directionsResponse != null) {
        Log.d("vadzim-test", "creating navigation route from parsed response")
        directionsResponse
    } else {
        Log.d("vadzim-test", "parsing directions response from scratch")
        responseJsonRef.toDirectionsResponse()
    }
    val refreshTtl = response.routes().getOrNull(routeIndex)?.refreshTtl()
    val routeOptions = RouteOptions.fromUrl(URL(requestUri))
    return NavigationRoute(
        directionsResponse = response.toBuilder().routes(emptyList()).build(), //TODO: optimise response
        routeOptions = routeOptions,
        routeIndex = routeIndex,
        directionsRoute = getDirectionsRoute(response, routeIndex, routeOptions),
        nativeRoute = this,
        expirationTimeElapsedSeconds = refreshTtl?.plus(responseTimeElapsedSeconds)
    ).cache()
}

private fun List<NavigationRoute>.cache(): List<NavigationRoute> {
    //RouteCompatibilityCache.cacheCreationResult(this)
    return this
}

private fun NavigationRoute.cache(): NavigationRoute {
    //RouteCompatibilityCache.cacheCreationResult(listOf(this))
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

fun DataRef.toDirectionsResponse(): DirectionsResponse {
    val stream = ByteBufferBackedInputStream(buffer)
    val reader = InputStreamReader(stream)
    return reader.use { reader ->
        DirectionsResponse.fromJson(reader)
    }
}

private class ByteBufferBackedInputStream(
    private val buffer: ByteBuffer
) : InputStream() {

    init {
        buffer.position(0)
    }

    override fun read(): Int {
        return if (!buffer.hasRemaining()) {
            -1
        } else {
            buffer.get().toInt()
        }
    }

    override fun available(): Int {
        return buffer.remaining()
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }
        val bytesToRead = len.coerceAtMost(buffer.remaining())
        buffer.get(bytes, off, bytesToRead)
        return bytesToRead
    }
}

private fun getDirectionsRoute(response: DirectionsResponse, routeIndex: Int, routeOptions: RouteOptions): DirectionsRoute =
    response.routes()[routeIndex].toBuilder()
        .requestUuid(response.uuid())
        .routeIndex(routeIndex.toString())
        .routeOptions(routeOptions)
        .build()