package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MapboxStreetsV8FBWrapper private constructor(
    private val fb: FBMapboxStreetsV8,
) : MapboxStreetsV8(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun roadClass(): String? = fb.class_

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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "MapboxStreetsV8(roadClass=${roadClass()})"
    }

    internal companion object {
        internal fun wrap(fb: FBMapboxStreetsV8?): MapboxStreetsV8? {
            return fb?.let { MapboxStreetsV8FBWrapper(it) }
        }
    }
}
