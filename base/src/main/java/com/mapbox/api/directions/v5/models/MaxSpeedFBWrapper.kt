package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MaxSpeedFBWrapper private constructor(
    private val fb: FBMaxSpeed,
) : MaxSpeed(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun speed(): Int? = fb.speed

    override fun unit(): String? {
        val unit = fb.unit ?: return null
        return when (unit) {
            FBSpeedLimitUnit.Kmph -> SpeedLimit.KMPH
            FBSpeedLimitUnit.Mph -> SpeedLimit.MPH
            FBSpeedLimitUnit.Unknown -> unrecognizeFlexBufferMap?.get("unit")?.asString()
            else -> unhandledEnumMapping("unit", fb.unit)
        }
    }

    override fun unknown(): Boolean? = fb.unknown

    override fun none(): Boolean? = fb.none

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MaxSpeed#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MaxSpeedFBWrapper && other.fb === fb) return true
        if (other is MaxSpeedFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "MaxSpeed(" +
            "speed=${speed()}, " +
            "unit=${unit()}, " +
            "unknown=${unknown()}, " +
            "none=${none()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBMaxSpeed?): MaxSpeed? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> MaxSpeedFBWrapper(fb)
            }
        }
    }
}
