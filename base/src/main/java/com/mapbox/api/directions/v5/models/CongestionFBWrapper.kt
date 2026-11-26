package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbCongestion
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class CongestionFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbCongestion,
) : Congestion(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun value(): Int = fb.value(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Congestion#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Congestion && other !is CongestionFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is CongestionFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Congestion(value=${value()})"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbCongestion): Congestion? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> CongestionFBWrapper(b, fb)
            }
        }
    }
}
