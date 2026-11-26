package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbMapboxStreetsV8
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MapboxStreetsV8FBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbMapboxStreetsV8,
) : MapboxStreetsV8(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun roadClass(): String? = fb.`class`(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MapboxStreetsV8#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is MapboxStreetsV8 && other !is MapboxStreetsV8FBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is MapboxStreetsV8FBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "MapboxStreetsV8(roadClass=${roadClass()})"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbMapboxStreetsV8): MapboxStreetsV8? {
            return if (fb.isPresent) MapboxStreetsV8FBWrapper(b, fb) else null
        }
    }
}
