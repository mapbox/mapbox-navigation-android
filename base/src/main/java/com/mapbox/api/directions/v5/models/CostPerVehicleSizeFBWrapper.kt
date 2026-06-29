package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
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
        if (other is CostPerVehicleSizeFBWrapper && other.fb === fb) return true
        if (other is CostPerVehicleSizeFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
