package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbCostPerVehicleSize
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class CostPerVehicleSizeFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbCostPerVehicleSize,
) : CostPerVehicleSize(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun small(): Double? = fb.smallOrNull(b)

    override fun standard(): Double? = fb.standardOrNull(b)

    override fun middle(): Double? = fb.middleOrNull(b)

    override fun large(): Double? = fb.largeOrNull(b)

    override fun jumbo(): Double? = fb.jumboOrNull(b)

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
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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
        internal fun wrap(b: FbBuffer, fb: FbCostPerVehicleSize): CostPerVehicleSize? {
            return if (fb.isPresent) CostPerVehicleSizeFBWrapper(b, fb) else null
        }
    }
}
