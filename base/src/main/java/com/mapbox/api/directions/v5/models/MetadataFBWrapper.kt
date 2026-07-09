package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MetadataFBWrapper private constructor(
    private val fb: FBResponseMetadata,
) : Metadata(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun infoMap(): Map<String?, String?>? {
        return (0 until fb.mapLength).associate {
            fb.map(it).let {
                Pair(it?.key, it?.value)
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
        return infoMap() == other.infoMap()
    }

    override fun hashCode() = infoMap()?.hashCode() ?: 0

    override fun toString(): String {
        return "Metadata(infoMap=${infoMap()})"
    }

    internal companion object {
        internal fun wrap(fb: FBResponseMetadata?): Metadata? {
            return fb?.let { MetadataFBWrapper(it) }
        }
    }
}
