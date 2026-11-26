package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbMaxSpeed
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MaxSpeedFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbMaxSpeed,
) : MaxSpeed(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun speed(): Int? = fb.speedOrNull(b)

    override fun unit(): String? {
        return when (FBSpeedLimitUnit.fromByteOrThrow(fb.unitOrNull(b) ?: return null)) {
            FBSpeedLimitUnit.Kmph -> SpeedLimit.KMPH
            FBSpeedLimitUnit.Mph -> SpeedLimit.MPH
            FBSpeedLimitUnit.Unknown -> unrecognizeFlexBufferMap?.get("unit")?.asString()
        }
    }

    override fun unknown(): Boolean? = fb.unknownOrNull(b)

    override fun none(): Boolean? = fb.noneOrNull(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MaxSpeed#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is MaxSpeed && other !is MaxSpeedFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is MaxSpeedFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "MaxSpeed(" +
            "speed=${speed()}, " +
            "unit=${unit()}, " +
            "unknown=${unknown()}, " +
            "none=${none()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbMaxSpeed): MaxSpeed? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> MaxSpeedFBWrapper(b, fb)
            }
        }
    }
}
