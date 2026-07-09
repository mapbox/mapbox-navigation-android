package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class JunctionFBWrapper private constructor(
    private val fb: FBJunction,
) : Junction(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun name(): String? = fb.name

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Junction#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Junction && other !is JunctionFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is JunctionFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "Junction(name=${name()})"
    }

    internal companion object {
        internal fun wrap(fb: FBJunction?): Junction? {
            return fb?.let { JunctionFBWrapper(it) }
        }
    }
}
