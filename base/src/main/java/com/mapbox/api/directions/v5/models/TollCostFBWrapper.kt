package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TollCostFBWrapper private constructor(
    private val fb: FBTollCost,
) : TollCost(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun currency(): String? = fb.currency

    override fun paymentMethods(): PaymentMethods? {
        return PaymentMethodsFBWrapper.wrap(fb.paymentMethods)
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("TollCost#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is TollCostFBWrapper && other.fb === fb) return true
        if (other is TollCostFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "TollCost(" +
            "currency=${currency()}, " +
            "paymentMethods=${paymentMethods()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBTollCost?): TollCost? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> TollCostFBWrapper(fb)
            }
        }
    }
}
