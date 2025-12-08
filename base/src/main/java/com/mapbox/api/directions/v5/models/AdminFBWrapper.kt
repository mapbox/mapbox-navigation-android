package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class AdminFBWrapper(
    private val fb: FBAdmin,
) : Admin(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun countryCode(): String? {
        return fb.countryCode
    }

    override fun countryCodeAlpha3(): String? {
        return fb.countryCodeAlpha3
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Admin#toBuilder()")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is AdminFBWrapper && other.fb === fb) return true
        if (other is AdminFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "Admin(" +
            "countryCode=${countryCode()}, " +
            "countryCodeAlpha3=${countryCodeAlpha3()}" +
            ")"
    }
}
