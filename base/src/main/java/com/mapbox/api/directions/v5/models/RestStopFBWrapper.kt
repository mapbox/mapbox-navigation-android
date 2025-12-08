package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RestStopFBWrapper(
    private val fb: FBRestStop,
) : RestStop(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String? {
        return when (fb.type) {
            FBRestStopType.RestArea -> "rest_area"
            FBRestStopType.ServiceArea -> "service_area"
            FBRestStopType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
            null -> null
            else -> unhandledEnumMapping("type", fb.type)
        }
    }

    override fun amenities(): List<Amenity?>? {
        return FlatbuffersListWrapper.get(fb.amenitiesLength) {
            fb.amenities(it)?.let { amenity ->
                AmenityFBWrapper(amenity)
            }
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
        if (other is RestStopFBWrapper && other.fb === fb) return true
        if (other is RestStopFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "RestStop(" +
            "type=${type()}, " +
            "amenities=${amenities()}, " +
            "guideMap=${guideMap()}, " +
            "name=${name()}" +
            ")"
    }
}
