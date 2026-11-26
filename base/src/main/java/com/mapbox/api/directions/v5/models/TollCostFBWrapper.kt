package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbTollCost
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TollCostFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbTollCost,
) : TollCost(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun currency(): String? = fb.currency(b)

    override fun paymentMethods(): PaymentMethods? {
        return PaymentMethodsFBWrapper.wrap(b, fb.paymentMethods(b))
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("TollCost#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is TollCost && other !is TollCostFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is TollCostFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "TollCost(" +
            "currency=${currency()}, " +
            "paymentMethods=${paymentMethods()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbTollCost): TollCost? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> TollCostFBWrapper(b, fb)
            }
        }
    }
}
