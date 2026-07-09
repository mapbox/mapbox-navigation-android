package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import java.nio.ByteBuffer

internal class SilentWaypointFBWrapper private constructor(
    private val fb: FBSilentWaypoint,
) : SilentWaypoint(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun waypointIndex(): Int = fb.waypointIndex

    override fun distanceFromStart(): Double = fb.distanceFromStart

    override fun geometryIndex(): Int = fb.geometryIndex

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is SilentWaypoint && other !is SilentWaypointFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is SilentWaypointFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "SilentWaypoint(" +
            "waypointIndex=${waypointIndex()}, " +
            "distanceFromStart=${distanceFromStart()}, " +
            "geometryIndex=${geometryIndex()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBSilentWaypoint?): SilentWaypoint? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> SilentWaypointFBWrapper(fb)
            }
        }
    }
}
