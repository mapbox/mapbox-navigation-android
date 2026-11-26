package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.api.directions.v5.models.utils.toPoint
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbDirectionsWaypoint
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class DirectionsWaypointFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbDirectionsWaypoint,
) : DirectionsWaypoint(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override val platformAddedUnrecognizedProperties: Map<String, Any?>?
        get() = fb.externalId(b)?.let { mapOf(KEY_EXTERNAL_ID to it) }

    override fun name(): String = fb.name(b)

    override fun rawLocation(): DoubleArray {
        return fb.location(b).toDoubleArrayOrEmpty(b)
    }

    override fun location(): Point {
        return fb.location(b).toPoint(b)
    }

    override fun distance(): Double? = fb.distanceOrNull(b)

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
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "DirectionsWaypoint(" +
            "name=${name()}, " +
            "rawLocation=${rawLocation().contentToString()}, " +
            "location=${location()}, " +
            "distance=${distance()}" +
            ")"
    }

    internal companion object {
        internal const val KEY_EXTERNAL_ID = "external_id"

        internal fun wrap(b: FbBuffer, fb: FbDirectionsWaypoint): DirectionsWaypoint? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> DirectionsWaypointFBWrapper(b, fb)
            }
        }
    }
}
