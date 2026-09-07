package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RestStopFBWrapper private constructor(
    private val fb: FBRestStop,
) : RestStop(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String? {
        return when (fb.type ?: return null) {
            FBRestStopType.RestArea -> "rest_area"
            FBRestStopType.ServiceArea -> "service_area"
            FBRestStopType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
        }
    }

    override fun amenities(): List<Amenity?>? {
        return FlatbuffersListWrapper.get(fb.amenitiesLength) {
            AmenityFBWrapper.wrap(fb.amenities(it))
        }
    }

    override fun guideMap(): String? = fb.guidemap

    override fun name(): String? = fb.name

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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "RestStop(" +
            "type=${type()}, " +
            "amenities=${amenities()}, " +
            "guideMap=${guideMap()}, " +
            "name=${name()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBRestStop?): RestStop? {
            return fb?.let { RestStopFBWrapper(it) }
        }
    }
}
