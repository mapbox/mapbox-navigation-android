package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import java.nio.ByteBuffer

internal class SilentWaypointFBWrapper(
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
        if (other is SilentWaypointFBWrapper && other.fb === fb) return true
        if (other is SilentWaypointFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "SilentWaypoint(" +
            "waypointIndex=${waypointIndex()}, " +
            "distanceFromStart=${distanceFromStart()}, " +
            "geometryIndex=${geometryIndex()}" +
            ")"
    }
}
