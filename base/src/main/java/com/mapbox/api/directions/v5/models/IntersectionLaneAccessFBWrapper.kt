package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IntersectionLaneAccessFBWrapper private constructor(
    private val fb: FBIntersectionLaneAccess,
) : IntersectionLaneAccess(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun designated(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.designatedLength) {
            fb.designated(it)?.toVehicleType()
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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "IntersectionLaneAccess(designated=${designated()})"
    }

    internal companion object {

        internal fun wrap(fb: FBIntersectionLaneAccess?): IntersectionLaneAccess? {
            return fb?.let { IntersectionLaneAccessFBWrapper(it) }
        }

        fun FBLaneDesignatedVehicleTypeEnumWrapper.toVehicleType(): String? {
            return if (this.isNull) {
                null
            } else {
                when (value) {
                    FBLaneDesignatedVehicleType.Unknown -> this.unrecognizedValue
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
