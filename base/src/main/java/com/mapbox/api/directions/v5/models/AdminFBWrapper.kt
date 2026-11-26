package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbAdmin
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class AdminFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbAdmin,
) : Admin(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun countryCode(): String? {
        return fb.countryCode(b)
    }

    override fun countryCodeAlpha3(): String? {
        return fb.countryCodeAlpha3(b)
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Admin#toBuilder()")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Admin && other !is AdminFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is AdminFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Admin(" +
            "countryCode=${countryCode()}, " +
            "countryCodeAlpha3=${countryCodeAlpha3()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbAdmin): Admin? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> AdminFBWrapper(b, fb)
            }
        }
    }
}
