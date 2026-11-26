package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbRestStop
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RestStopFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbRestStop,
) : RestStop(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun type(): String? {
        return when (fb.typeOrNull(b)?.let { FBRestStopType.fromByteOrThrow(it) } ?: return null) {
            FBRestStopType.RestArea -> "rest_area"
            FBRestStopType.ServiceArea -> "service_area"
            FBRestStopType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
        }
    }

    override fun amenities(): List<Amenity?>? {
        val amenities = fb.amenities(b)
        return FlatbuffersListWrapper.get(amenities.size) {
            AmenityFBWrapper.wrap(b, amenities.get(b, it))
        }
    }

    override fun guideMap(): String? = fb.guidemap(b)

    override fun name(): String? = fb.name(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("RestStop#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is RestStop && other !is RestStopFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is RestStopFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "RestStop(" +
            "type=${type()}, " +
            "amenities=${amenities()}, " +
            "guideMap=${guideMap()}, " +
            "name=${name()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbRestStop): RestStop? {
            return if (fb.isPresent) RestStopFBWrapper(b, fb) else null
        }
    }
}
