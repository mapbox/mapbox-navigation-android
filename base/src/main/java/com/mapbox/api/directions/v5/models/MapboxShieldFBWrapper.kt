package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MapboxShieldFBWrapper(
    private val fb: FBMapboxShield,
) : MapboxShield(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun baseUrl(): String? = fb.baseUrl

    override fun displayRef(): String? = fb.displayRef

    override fun name(): String? = fb.name

    override fun textColor(): String? = fb.textColor

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MapboxShield#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MapboxShieldFBWrapper && other.fb === fb) return true
        if (other is MapboxShieldFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "MapboxShield(" +
            "baseUrl=${baseUrl()}, " +
            "displayRef=${displayRef()}, " +
            "name=${name()}, " +
            "textColor=${textColor()}" +
            ")"
    }
}
