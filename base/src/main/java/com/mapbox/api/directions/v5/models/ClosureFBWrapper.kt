package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class ClosureFBWrapper private constructor(
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
        if (other == null) return false
        if (other is Closure && other !is ClosureFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is ClosureFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "Closure(" +
            "geometryIndexStart=${geometryIndexStart()}, " +
            "geometryIndexEnd=${geometryIndexEnd()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBClosure?): Closure? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> ClosureFBWrapper(fb)
            }
        }
    }
}
