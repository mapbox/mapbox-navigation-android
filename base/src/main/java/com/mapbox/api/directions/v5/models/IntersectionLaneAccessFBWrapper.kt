package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbIntersectionLaneAccess
import com.mapbox.directions.accessors.FbLaneDesignatedVehicleTypeEnumWrapper
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IntersectionLaneAccessFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbIntersectionLaneAccess,
) : IntersectionLaneAccess(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun designated(): List<String?>? {
        val designated = fb.designated(b)
        return FlatbuffersListWrapper.get(designated.size) {
            designated.get(b, it).toVehicleType(b)
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("IntersectionLaneAccess#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is IntersectionLaneAccess && other !is IntersectionLaneAccessFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is IntersectionLaneAccessFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "IntersectionLaneAccess(designated=${designated()})"
    }

    internal companion object {

        internal fun wrap(b: FbBuffer, fb: FbIntersectionLaneAccess): IntersectionLaneAccess? {
            return if (fb.isPresent) IntersectionLaneAccessFBWrapper(b, fb) else null
        }

        fun FbLaneDesignatedVehicleTypeEnumWrapper.toVehicleType(b: FbBuffer): String? {
            return if (!isPresent || isNull(b)) {
                null
            } else {
                when (FBLaneDesignatedVehicleType.fromByteOrThrow(value(b))) {
                    FBLaneDesignatedVehicleType.Unknown -> unrecognizedValue(b)
                    FBLaneDesignatedVehicleType.Bus -> IntersectionLaneAccess.BUS
                    FBLaneDesignatedVehicleType.Hov -> IntersectionLaneAccess.HOV
                    FBLaneDesignatedVehicleType.Taxi -> IntersectionLaneAccess.TAXI
                    FBLaneDesignatedVehicleType.Motorcycle -> IntersectionLaneAccess.MOTORCYCLE
                    FBLaneDesignatedVehicleType.Bicycle -> IntersectionLaneAccess.BICYCLE
                    FBLaneDesignatedVehicleType.Moped -> IntersectionLaneAccess.MOPED
                }
            }
        }
    }
}
