package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.fbToLineIndication
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IntersectionLanesFBWrapper(
    private val fb: FBIntersectionLanes,
) : IntersectionLanes(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun valid(): Boolean? = fb.valid

    override fun active(): Boolean? = fb.active

    override fun validIndication(): String? {
        return fb.validIndication?.fbToLineIndication(
            "valid_indication",
        ) {
            unrecognizeFlexBufferMap?.get("valid_indication")?.asString()
        }
    }

    override fun indications(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.indicationsLength) { index ->
            fb.indications(index)?.let { indication ->
                indication.value.fbToLineIndication("indications") {
                    indication.unrecognizedValue
                }
            }
        }
    }

    override fun paymentMethods(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.paymentMethodsLength) {
            fb.paymentMethods(it)?.fbToPaymentMethodCriteria("payment_methods")
        }
    }

    override fun access(): IntersectionLaneAccess? {
        return fb.access?.let {
            IntersectionLaneAccessFBWrapper(it)
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("IntersectionLanes#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is IntersectionLanesFBWrapper && other.fb === fb) return true
        if (other is IntersectionLanesFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "IntersectionLanes(" +
            "valid=${valid()}, " +
            "active=${active()}, " +
            "validIndication=${validIndication()}, " +
            "indications=${indications()}, " +
            "paymentMethods=${paymentMethods()}, " +
            "access=${access()}" +
            ")"
    }

    private companion object {

        @DirectionsCriteria.PaymentMethodsCriteria
        fun FBLanePaymentMethodEnumWrapper.fbToPaymentMethodCriteria(
            propertyName: String,
        ): String? {
            return when (this.value) {
                FBLanePaymentMethod.General -> DirectionsCriteria.PAYMENT_METHOD_GENERAL
                FBLanePaymentMethod.Etc -> DirectionsCriteria.PAYMENT_METHOD_ETC
                FBLanePaymentMethod.Etcx -> DirectionsCriteria.PAYMENT_METHOD_ETCX
                FBLanePaymentMethod.Cash -> DirectionsCriteria.PAYMENT_METHOD_CASH
                FBLanePaymentMethod.ExactCash -> DirectionsCriteria.PAYMENT_METHOD_EXACT_CASH
                FBLanePaymentMethod.Coins -> DirectionsCriteria.PAYMENT_METHOD_COINS
                FBLanePaymentMethod.Notes -> DirectionsCriteria.PAYMENT_METHOD_NOTES
                FBLanePaymentMethod.DebitCards -> DirectionsCriteria.PAYMENT_METHOD_DEBIT_CARDS
                FBLanePaymentMethod.PassCard -> DirectionsCriteria.PAYMENT_METHOD_PASS_CARD
                FBLanePaymentMethod.CreditCards -> DirectionsCriteria.PAYMENT_METHOD_CREDIT_CARDS
                FBLanePaymentMethod.Video -> DirectionsCriteria.PAYMENT_METHOD_VIDEO
                FBLanePaymentMethod.Cryptocurrencies ->
                    DirectionsCriteria.PAYMENT_METHOD_CRYPTOCURRENCIES
                FBLanePaymentMethod.App -> DirectionsCriteria.PAYMENT_METHOD_APP
                FBLanePaymentMethod.Etc2 -> DirectionsCriteria.PAYMENT_METHOD_ETC2
                FBLanePaymentMethod.Unknown -> this.unrecognizedValue
                else -> unhandledEnumMapping(propertyName, this.value)
            }
        }
    }
}
