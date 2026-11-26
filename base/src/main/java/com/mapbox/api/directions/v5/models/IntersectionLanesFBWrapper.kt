package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.fbToLineIndication
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbIntersectionLanes
import com.mapbox.directions.accessors.FbLanePaymentMethodEnumWrapper
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class IntersectionLanesFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbIntersectionLanes,
) : IntersectionLanes(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun valid(): Boolean? = fb.validOrNull(b)

    override fun active(): Boolean? = fb.activeOrNull(b)

    override fun validIndication(): String? {
        return fb.validIndicationOrNull(b)?.fbToLineIndication {
            unrecognizeFlexBufferMap?.get("valid_indication")?.asString()
        }
    }

    override fun indications(): List<String?>? {
        val indications = fb.indications(b)
        return FlatbuffersListWrapper.get(indications.size) { index ->
            val indication = indications.get(b, index)
            if (!indication.isPresent || indication.isNull(b)) {
                null
            } else {
                indication.value(b).fbToLineIndication {
                    indication.unrecognizedValue(b)
                }
            }
        }
    }

    override fun paymentMethods(): List<String?>? {
        val paymentMethods = fb.paymentMethods(b)
        return FlatbuffersListWrapper.get(paymentMethods.size) {
            paymentMethods.get(b, it).fbToPaymentMethodCriteria(b)
        }
    }

    override fun access(): IntersectionLaneAccess? {
        return IntersectionLaneAccessFBWrapper.wrap(b, fb.access(b))
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("IntersectionLanes#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is IntersectionLanes && other !is IntersectionLanesFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is IntersectionLanesFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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

    internal companion object {

        internal fun wrap(b: FbBuffer, fb: FbIntersectionLanes): IntersectionLanes? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> IntersectionLanesFBWrapper(b, fb)
            }
        }

        @DirectionsCriteria.PaymentMethodsCriteria
        fun FbLanePaymentMethodEnumWrapper.fbToPaymentMethodCriteria(b: FbBuffer): String? {
            return if (!isPresent || isNull(b)) {
                null
            } else {
                when (FBLanePaymentMethod.fromByteOrThrow(value(b))) {
                    FBLanePaymentMethod.General -> DirectionsCriteria.PAYMENT_METHOD_GENERAL
                    FBLanePaymentMethod.Etc -> DirectionsCriteria.PAYMENT_METHOD_ETC
                    FBLanePaymentMethod.Etcx -> DirectionsCriteria.PAYMENT_METHOD_ETCX
                    FBLanePaymentMethod.Cash -> DirectionsCriteria.PAYMENT_METHOD_CASH
                    FBLanePaymentMethod.ExactCash -> DirectionsCriteria.PAYMENT_METHOD_EXACT_CASH
                    FBLanePaymentMethod.Coins -> DirectionsCriteria.PAYMENT_METHOD_COINS
                    FBLanePaymentMethod.Notes -> DirectionsCriteria.PAYMENT_METHOD_NOTES
                    FBLanePaymentMethod.DebitCards -> DirectionsCriteria.PAYMENT_METHOD_DEBIT_CARDS
                    FBLanePaymentMethod.PassCard -> DirectionsCriteria.PAYMENT_METHOD_PASS_CARD
                    FBLanePaymentMethod.CreditCards ->
                        DirectionsCriteria.PAYMENT_METHOD_CREDIT_CARDS
                    FBLanePaymentMethod.Video -> DirectionsCriteria.PAYMENT_METHOD_VIDEO
                    FBLanePaymentMethod.Cryptocurrencies ->
                        DirectionsCriteria.PAYMENT_METHOD_CRYPTOCURRENCIES
                    FBLanePaymentMethod.App -> DirectionsCriteria.PAYMENT_METHOD_APP
                    FBLanePaymentMethod.Etc2 -> DirectionsCriteria.PAYMENT_METHOD_ETC2
                    FBLanePaymentMethod.Unknown -> unrecognizedValue(b)
                }
            }
        }
    }
}
