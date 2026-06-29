package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class PaymentMethodsFBWrapper private constructor(
    private val fb: FBPaymentMethods,
) : PaymentMethods(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun etc(): CostPerVehicleSize? {
        return CostPerVehicleSizeFBWrapper.wrap(fb.etc)
    }

    override fun etc2(): CostPerVehicleSize? {
        return CostPerVehicleSizeFBWrapper.wrap(fb.etc2)
    }

    override fun cash(): CostPerVehicleSize? {
        return CostPerVehicleSizeFBWrapper.wrap(fb.cash)
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("PaymentMethods#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is PaymentMethodsFBWrapper && other.fb === fb) return true
        if (other is PaymentMethodsFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "PaymentMethods(" +
            "etc=${etc()}, " +
            "etc2=${etc2()}, " +
            "cash=${cash()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBPaymentMethods?): PaymentMethods? {
            return fb?.let { PaymentMethodsFBWrapper(it) }
        }
    }
}
