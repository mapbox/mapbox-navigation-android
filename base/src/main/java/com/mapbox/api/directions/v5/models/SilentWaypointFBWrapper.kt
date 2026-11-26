package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbSilentWaypoint
import java.nio.ByteBuffer

internal class SilentWaypointFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbSilentWaypoint,
) : SilentWaypoint(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun waypointIndex(): Int = fb.waypointIndex(b)

    override fun distanceFromStart(): Double = fb.distanceFromStart(b)

    override fun geometryIndex(): Int = fb.geometryIndex(b)

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
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "SilentWaypoint(" +
            "waypointIndex=${waypointIndex()}, " +
            "distanceFromStart=${distanceFromStart()}, " +
            "geometryIndex=${geometryIndex()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbSilentWaypoint): SilentWaypoint? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> SilentWaypointFBWrapper(b, fb)
            }
        }
    }
}
