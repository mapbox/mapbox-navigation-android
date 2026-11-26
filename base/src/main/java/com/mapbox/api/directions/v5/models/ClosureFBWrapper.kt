package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbClosure
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class ClosureFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbClosure,
) : Closure(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun geometryIndexStart(): Int = fb.geometryIndexStart(b)

    override fun geometryIndexEnd(): Int = fb.geometryIndexEnd(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Closure#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Closure && other !is ClosureFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is ClosureFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Closure(" +
            "geometryIndexStart=${geometryIndexStart()}, " +
            "geometryIndexEnd=${geometryIndexEnd()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbClosure): Closure? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> ClosureFBWrapper(b, fb)
            }
        }
    }
}
