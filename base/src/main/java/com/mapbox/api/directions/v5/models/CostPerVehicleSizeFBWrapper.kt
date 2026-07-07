package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class CostPerVehicleSizeFBWrapper private constructor(
    private val fb: FBCostPerVehicleSize,
) : CostPerVehicleSize(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun small(): Double? = fb.small

    override fun standard(): Double? = fb.standard

    override fun middle(): Double? = fb.middle

    override fun large(): Double? = fb.large

    override fun jumbo(): Double? = fb.jumbo

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("CostPerVehicleSize#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is CostPerVehicleSize && other !is CostPerVehicleSizeFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is CostPerVehicleSizeFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "CostPerVehicleSize(" +
            "small=${small()}, " +
            "standard=${standard()}, " +
            "middle=${middle()}, " +
            "large=${large()}, " +
            "jumbo=${jumbo()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBCostPerVehicleSize?): CostPerVehicleSize? {
            return fb?.let { CostPerVehicleSizeFBWrapper(it) }
        }
    }
}
