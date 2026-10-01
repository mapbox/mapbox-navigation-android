package com.mapbox.api.directions.v5.models

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class DirectionsRouteFBWrapper private constructor(
    private val fb: FBDirectionsRoute,
    private val routeOptions: RouteOptions?,
    val fbContext: FBDirectionsRouteContext,
    val context: DirectionsRouteContext,
) : DirectionsRoute(), BaseFBWrapper {

    internal val stepsCountWithGeometry: Int by lazy {
        legs()?.sumOf { leg ->
            leg?.steps()?.count { it.geometry() != null } ?: 0
        } ?: 0
    }

    private val _geometry by lazy {
        fb.geometry
    }

    init {
        // TODO: https://mapbox.atlassian.net/browse/NAVSDKCPP-836
        // warming up java side caches
        stepsCountWithGeometry
        _geometry
    }

    internal val refreshTtl: Int? get() = fb.refreshTtl

    internal val geometryNumeric
        get(): List<Point>? =
            FlatbuffersListWrapper.get(fb.geometryNumericLength) {
                val coordinate = fb.geometryNumeric(it)!!
                Point.fromLngLat(coordinate.longitude, coordinate.latitude)
            }

    /**
     * Content based 64 bit hash of the whole route. Native code computes it once, while building
     * the buffer, and stores it in the table, so this is a plain field read.
     */
    internal val contentHash: Long = fb.hash.toLong()

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun routeIndex(): String? = fb.routeIndex.toString()

    override fun distance(): Double = fb.distance

    override fun duration(): Double = fb.duration

    override fun durationTypical(): Double? = fb.durationTypical

    override fun geometry(): String? = _geometry

    override fun weight(): Double? = fb.weight

    override fun weightTypical(): Double? = fb.weightTypical

    override fun weightName(): String? = fb.weightName

    override fun legs(): List<RouteLeg?>? {
        return FlatbuffersListWrapper.get(fb.legsLength) {
            RouteLegFBWrapper.wrap(fb.legs(it))
        }
    }

    override fun waypoints(): List<DirectionsWaypoint?>? {
        return FlatbuffersListWrapper.get(fb.waypointsLength) {
            DirectionsWaypointFBWrapper.wrap(fb.waypoints(it))
        }
    }

    // TODO: https://mapbox.atlassian.net/browse/NAVAND-6590
    override fun routeOptions(): RouteOptions? = routeOptions

    override fun voiceLanguage(): String? = fb.voiceLocale

    override fun requestUuid(): String? = fb.requestUuid

    override fun tollCosts(): List<TollCost?>? {
        return FlatbuffersListWrapper.get(fb.tollCostsLength) {
            TollCostFBWrapper.wrap(fb.tollCosts(it))
        }
    }

    fun mapMatchingConfidence(): Double? = fb.confidence

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
        val refreshTtlValue = fb.refreshTtl
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
        if (fb.routeIndex != other.fb.routeIndex) return false
        if (fb.requestUuid != other.fb.requestUuid) return false
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
            val routeContext = FBDirectionsRouteContext.getRootAsDirectionsRouteContext(
                bindgenContext.getData().buffer,
            )
            val fb = routeContext.route
            return when {
                fb.isNull -> null
                else -> DirectionsRouteFBWrapper(
                    fb,
                    routeOptions,
                    routeContext,
                    bindgenContext,
                )
            }
        }
    }
}
