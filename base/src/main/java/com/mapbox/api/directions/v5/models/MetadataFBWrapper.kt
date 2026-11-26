package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbResponseMetadata
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MetadataFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbResponseMetadata,
) : Metadata(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun infoMap(): Map<String?, String?>? {
        val map = fb.map(b)
        return (0 until map.size).associate {
            map.get(b, it).let {
                Pair(it.key(b), it.value(b))
            }
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Metadata#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Metadata && other !is MetadataFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is MetadataFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Metadata(infoMap=${infoMap()})"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbResponseMetadata): Metadata? {
            return if (fb.isPresent) MetadataFBWrapper(b, fb) else null
        }
    }
}
