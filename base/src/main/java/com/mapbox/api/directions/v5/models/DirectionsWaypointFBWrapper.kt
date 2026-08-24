package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.api.directions.v5.models.utils.toPoint
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class DirectionsWaypointFBWrapper private constructor(
    private val fb: FBDirectionsWaypoint,
) : DirectionsWaypoint(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun name(): String = fb.name

    override fun rawLocation(): DoubleArray {
        return fb.location.toDoubleArrayOrEmpty()
    }

    override fun location(): Point {
        return fb.location.toPoint()
    }

    override fun distance(): Double? = fb.distance

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("DirectionsWaypoint#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is DirectionsWaypoint && other !is DirectionsWaypointFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is DirectionsWaypointFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "DirectionsWaypoint(" +
            "name=${name()}, " +
            "rawLocation=${rawLocation().contentToString()}, " +
            "location=${location()}, " +
            "distance=${distance()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBDirectionsWaypoint?): DirectionsWaypoint? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> DirectionsWaypointFBWrapper(fb)
            }
        }
    }
}
