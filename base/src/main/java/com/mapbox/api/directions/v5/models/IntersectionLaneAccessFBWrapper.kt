package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
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
            fb.designated(it)?.toVehicleType("designated")
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
        if (other is IntersectionLaneAccessFBWrapper && other.fb === fb) return true
        if (other is IntersectionLaneAccessFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "IntersectionLaneAccess(designated=${designated()})"
    }

    internal companion object {

        internal fun wrap(fb: FBIntersectionLaneAccess?): IntersectionLaneAccess? {
            return fb?.let { IntersectionLaneAccessFBWrapper(it) }
        }

        private fun FBLaneDesignatedVehicleTypeEnumWrapper.toVehicleType(propertyName: String):
            String? {
            return if (this.isNull) {
                null
            } else {
                when (this.value) {
                    FBLaneDesignatedVehicleType.Unknown -> this.unrecognizedValue
                    FBLaneDesignatedVehicleType.Bus -> IntersectionLaneAccess.BUS
                    FBLaneDesignatedVehicleType.Hov -> IntersectionLaneAccess.HOV
                    FBLaneDesignatedVehicleType.Taxi -> IntersectionLaneAccess.TAXI
                    FBLaneDesignatedVehicleType.Motorcycle -> IntersectionLaneAccess.MOTORCYCLE
                    FBLaneDesignatedVehicleType.Bicycle -> IntersectionLaneAccess.BICYCLE
                    FBLaneDesignatedVehicleType.Moped -> IntersectionLaneAccess.MOPED
                    else -> unhandledEnumMapping(propertyName, this.value)
                }
            }
        }
    }
}
