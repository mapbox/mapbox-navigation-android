package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerInstructionsFBWrapper private constructor(
    private val fb: FBBannerInstructions,
) : BannerInstructions(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun distanceAlongGeometry(): Double = fb.distanceAlongGeometry

    override fun primary(): BannerText {
        return BannerTextFBWrapper.wrap(fb.primary)!!
    }

    override fun secondary(): BannerText? {
        return BannerTextFBWrapper.wrap(fb.secondary)
    }

    override fun sub(): BannerText? {
        return BannerTextFBWrapper.wrap(fb.sub)
    }

    override fun view(): BannerView? {
        return BannerViewFBWrapper.wrap(fb.view)
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerInstructions#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BannerInstructionsFBWrapper && other.fb === fb) return true
        if (other is BannerInstructionsFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
        internal fun wrap(fb: FBBannerInstructions?): BannerInstructions? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> BannerInstructionsFBWrapper(fb)
            }
        }
    }
}
