package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class CongestionFBWrapper private constructor(
    private val fb: FBCongestion,
) : Congestion(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun value(): Int = fb.value

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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "Congestion(value=${value()})"
    }

    internal companion object {
        internal fun wrap(fb: FBCongestion?): Congestion? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> CongestionFBWrapper(fb)
            }
        }
    }
}
