package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TrafficCodesFBWrapper private constructor(
    private val fb: FBTrafficCodes,
) : TrafficCodes(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun jarticCauseCode(): Int? = fb.jarticCauseCode

    override fun jarticRegulationCode(): Int? = fb.jarticRegulationCode

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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "TrafficCodes(" +
            "jarticCauseCode=${jarticCauseCode()}, " +
            "jarticRegulationCode=${jarticRegulationCode()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBTrafficCodes?): TrafficCodes? {
            return fb?.let { TrafficCodesFBWrapper(it) }
        }
    }
}
