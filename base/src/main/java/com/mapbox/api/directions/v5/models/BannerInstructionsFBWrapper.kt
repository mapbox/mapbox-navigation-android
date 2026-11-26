package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBannerInstructions
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerInstructionsFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbBannerInstructions,
) : BannerInstructions(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun distanceAlongGeometry(): Double = fb.distanceAlongGeometry(b)

    override fun primary(): BannerText {
        return BannerTextFBWrapper.wrap(b, fb.primary(b))!!
    }

    override fun secondary(): BannerText? {
        return BannerTextFBWrapper.wrap(b, fb.secondary(b))
    }

    override fun sub(): BannerText? {
        return BannerTextFBWrapper.wrap(b, fb.sub(b))
    }

    override fun view(): BannerView? {
        return BannerViewFBWrapper.wrap(b, fb.view(b))
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerInstructions#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is BannerInstructions && other !is BannerInstructionsFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is BannerInstructionsFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "BannerInstructions(" +
            "distanceAlongGeometry=${distanceAlongGeometry()}, " +
            "primary=${primary()}, " +
            "secondary=${secondary()}, " +
            "sub=${sub()}, " +
            "view=${view()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbBannerInstructions): BannerInstructions? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> BannerInstructionsFBWrapper(b, fb)
            }
        }
    }
}
