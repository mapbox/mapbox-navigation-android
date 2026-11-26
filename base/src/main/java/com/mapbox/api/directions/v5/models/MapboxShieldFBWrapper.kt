package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbMapboxShield
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MapboxShieldFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbMapboxShield,
) : MapboxShield(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun baseUrl(): String? = fb.baseUrl(b)

    override fun displayRef(): String? = fb.displayRef(b)

    override fun name(): String? = fb.name(b)

    override fun textColor(): String? = fb.textColor(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MapboxShield#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is MapboxShield && other !is MapboxShieldFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is MapboxShieldFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "MapboxShield(" +
            "baseUrl=${baseUrl()}, " +
            "displayRef=${displayRef()}, " +
            "name=${name()}, " +
            "textColor=${textColor()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbMapboxShield): MapboxShield? {
            return if (fb.isPresent) MapboxShieldFBWrapper(b, fb) else null
        }
    }
}
