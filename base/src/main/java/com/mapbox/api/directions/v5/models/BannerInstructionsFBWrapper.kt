package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerInstructionsFBWrapper(
    private val fb: FBBannerInstructions,
) : BannerInstructions(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun distanceAlongGeometry(): Double = fb.distanceAlongGeometry

    override fun primary(): BannerText {
        return BannerTextFBWrapper(fb.primary)
    }

    override fun secondary(): BannerText? {
        return fb.secondary?.let { BannerTextFBWrapper(it) }
    }

    override fun sub(): BannerText? {
        return fb.sub?.let { BannerTextFBWrapper(it) }
    }

    override fun view(): BannerView? {
        return fb.view?.let { BannerViewFBWrapper(it) }
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
}
