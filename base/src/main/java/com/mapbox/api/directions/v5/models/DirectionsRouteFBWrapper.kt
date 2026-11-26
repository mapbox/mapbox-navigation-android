package com.mapbox.api.directions.v5.models

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbDirectionsRoute
import com.mapbox.directions.accessors.FbDirectionsRouteContext
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class DirectionsRouteFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbDirectionsRoute,
    private val routeOptions: RouteOptions?,
    private val fbContext: FbDirectionsRouteContext,
    val context: DirectionsRouteContext,
) : DirectionsRoute(), BaseFBWrapper {

    internal val stepsCountWithGeometry: Int by lazy {
        legs()?.sumOf { leg ->
            leg?.steps()?.count { it.geometry() != null } ?: 0
        } ?: 0
    }

    private val _geometry by lazy {
        fb.geometry(b)
    }

    init {
        // TODO: https://mapbox.atlassian.net/browse/NAVSDKCPP-836
        // warming up java side caches
        stepsCountWithGeometry
        _geometry
    }

    internal val refreshTtl: Int? get() = fb.refreshTtlOrNull(b)

    /** Buffer holding the whole DirectionsRouteContext. */
    internal val contextByteBuffer: ByteBuffer get() = b.byteBuffer

    internal val contextUuid: String? get() = fbContext.uuid(b)

    internal val contextRouteIndex: Int get() = fb.routeIndex(b).toInt()

    /** Waypoints of the response this route belongs to. */
    internal fun contextWaypoints(): List<DirectionsWaypoint?>? {
        val waypoints = fbContext.waypoints(b)
        return FlatbuffersListWrapper.get(waypoints.size) {
            DirectionsWaypointFBWrapper.wrap(b, waypoints.get(b, it))
        }
    }

    internal val geometryNumeric: List<Point>?
        get() {
            val geometry = fb.geometryNumeric(b)
            return FlatbuffersListWrapper.get(geometry.size) {
                val coordinate = geometry.get(b, it)
                Point.fromLngLat(coordinate.longitude(b), coordinate.latitude(b))
            }
        }

    /**
     * Content based 64 bit hash of the whole route. Native code computes it once, while building
     * the buffer, and stores it in the table, so this is a plain field read.
     */
    internal val contentHash: Long = fb.hash(b).toLong()

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun routeIndex(): String? = fb.routeIndex(b).toString()

    override fun distance(): Double = fb.distance(b)

    override fun duration(): Double = fb.duration(b)

    override fun durationTypical(): Double? = fb.durationTypicalOrNull(b)

    override fun geometry(): String? = _geometry

    override fun weight(): Double? = fb.weightOrNull(b)

    override fun weightTypical(): Double? = fb.weightTypicalOrNull(b)

    override fun weightName(): String? = fb.weightName(b)

    override fun legs(): List<RouteLeg?>? {
        val legs = fb.legs(b)
        return FlatbuffersListWrapper.get(legs.size) {
            RouteLegFBWrapper.wrap(b, legs.get(b, it))
        }
    }

    override fun waypoints(): List<DirectionsWaypoint?>? {
        val waypoints = fb.waypoints(b)
        return FlatbuffersListWrapper.get(waypoints.size) {
            DirectionsWaypointFBWrapper.wrap(b, waypoints.get(b, it))
        }
    }

    // TODO: https://mapbox.atlassian.net/browse/NAVAND-6590
    override fun routeOptions(): RouteOptions? = routeOptions

    override fun voiceLanguage(): String? = fb.voiceLocale(b)

    override fun requestUuid(): String? = fb.requestUuid(b)

    override fun tollCosts(): List<TollCost?>? {
        val tollCosts = fb.tollCosts(b)
        return FlatbuffersListWrapper.get(tollCosts.size) {
            TollCostFBWrapper.wrap(b, tollCosts.get(b, it))
        }
    }

    fun mapMatchingConfidence(): Double? = fb.confidenceOrNull(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("DirectionsRoute#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        val nroUnrecognizedProperties = super<BaseFBWrapper>.unrecognized()?.let {
            if (it.contains("requestUuid")) {
                it.toMutableMap().apply {
                    // TODO: could be removed once change below is adapted
                    // https://github.com/mapbox/mapbox-sdk/pull/10279
                    remove("requestUuid")
                }
            } else {
                it
            }
        }
        val refreshTtlValue = fb.refreshTtlOrNull(b)
        return if (refreshTtlValue != null) {
            nroUnrecognizedProperties.orEmpty() +
                mapOf(
                    "refresh_ttl" to SerializableJsonElement(
                        JsonPrimitive(refreshTtlValue),
                    ),
                )
        } else {
            nroUnrecognizedProperties
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is DirectionsRoute && other !is DirectionsRouteFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is DirectionsRouteFBWrapper) return false

        // Index and UUID identify the route, so comparing them first is exact: routes from
        // different responses, or different routes of one response, can never be reported equal
        // by a hash collision.
        if (fb.routeIndex(b) != other.fb.routeIndex(other.b)) return false
        if (fb.requestUuid(b) != other.fb.requestUuid(other.b)) return false
        return contentHash == other.contentHash
    }

    override fun hashCode(): Int = contentHash.toHashCode()

    override fun toString(): String {
        return "DirectionsRoute(" +
            "routeIndex=${routeIndex()}, " +
            "distance=${distance()}, " +
            "duration=${duration()}, " +
            "durationTypical=${durationTypical()}, " +
            "geometry=${geometry()}, " +
            "weight=${weight()}, " +
            "weightTypical=${weightTypical()}, " +
            "weightName=${weightName()}, " +
            "legs=${legs()}, " +
            "waypoints=${waypoints()}, " +
            "routeOptions=${routeOptions()}, " +
            "voiceLanguage=${voiceLanguage()}, " +
            "requestUuid=${requestUuid()}, " +
            "tollCosts=${tollCosts()}" +
            ")"
    }

    internal companion object {

        internal fun wrap(
            routeOptions: RouteOptions?,
            bindgenContext: DirectionsRouteContext,
        ): DirectionsRouteFBWrapper? {
            val buffer = bindgenContext.getData().buffer.order(ByteOrder.LITTLE_ENDIAN)
            val b = FbBuffer(buffer)
            val routeContext = FbDirectionsRouteContext(b.indirect(buffer.position()))
            val fb = routeContext.route(b)
            return when {
                !fb.isPresent || fb.isNull(b) -> null
                else -> DirectionsRouteFBWrapper(
                    b,
                    fb,
                    routeOptions,
                    routeContext,
                    bindgenContext,
                )
            }
        }
    }
}
