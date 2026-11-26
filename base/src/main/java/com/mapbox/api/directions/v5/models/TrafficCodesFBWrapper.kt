package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbTrafficCodes
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TrafficCodesFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbTrafficCodes,
) : TrafficCodes(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun jarticCauseCode(): Int? = fb.jarticCauseCodeOrNull(b)

    override fun jarticRegulationCode(): Int? = fb.jarticRegulationCodeOrNull(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("TrafficCodes#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is TrafficCodes && other !is TrafficCodesFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is TrafficCodesFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "TrafficCodes(" +
            "jarticCauseCode=${jarticCauseCode()}, " +
            "jarticRegulationCode=${jarticRegulationCode()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbTrafficCodes): TrafficCodes? {
            return if (fb.isPresent) TrafficCodesFBWrapper(b, fb) else null
        }
    }
}
