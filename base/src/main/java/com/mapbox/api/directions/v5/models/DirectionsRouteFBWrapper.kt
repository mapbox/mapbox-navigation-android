package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class DirectionsRouteFBWrapper private constructor(
    private val fb: FBDirectionsRoute,
    private val routeOptions: RouteOptions? = null,
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

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("DirectionsRoute#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DirectionsRouteFBWrapper && other.fb === fb) return true
        if (other is DirectionsRouteFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
            fb: FBDirectionsRoute?,
            routeOptions: RouteOptions? = null,
        ): DirectionsRoute? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> DirectionsRouteFBWrapper(fb, routeOptions)
            }
        }
    }
}
