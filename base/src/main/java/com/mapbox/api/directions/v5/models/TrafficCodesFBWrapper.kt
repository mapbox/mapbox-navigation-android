package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TrafficCodesFBWrapper(
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
        if (other is TrafficCodesFBWrapper && other.fb === fb) return true
        if (other is TrafficCodesFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "TrafficCodes(" +
            "jarticCauseCode=${jarticCauseCode()}, " +
            "jarticRegulationCode=${jarticRegulationCode()}" +
            ")"
    }
}
