package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class ClosureFBWrapper(
    private val fb: FBClosure,
) : Closure(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun geometryIndexStart(): Int = fb.geometryIndexStart

    override fun geometryIndexEnd(): Int = fb.geometryIndexEnd

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Closure#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ClosureFBWrapper && other.fb === fb) return true
        if (other is ClosureFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "Closure(" +
            "geometryIndexStart=${geometryIndexStart()}, " +
            "geometryIndexEnd=${geometryIndexEnd()}" +
            ")"
    }
}
