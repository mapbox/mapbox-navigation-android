package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.fbToManeuverModifierType
import com.mapbox.api.directions.v5.models.utils.fbToStepManeuverType
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerViewFBWrapper private constructor(
    private val fb: FBBannerView,
) : BannerView(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun text(): String = fb.text

    override fun components(): List<BannerComponents?>? {
        return FlatbuffersListWrapper.get(fb.componentsLength) {
            BannerComponentsFBWrapper.wrap(fb.components(it))
        }
    }

    override fun type(): String? = fb.type.fbToStepManeuverType("type", unrecognizeFlexBufferMap)

    override fun modifier(): String? = fb.modifier?.fbToManeuverModifierType(
        "modifier",
        unrecognizeFlexBufferMap,
    )

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerView#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BannerViewFBWrapper && other.fb === fb) return true
        if (other is BannerViewFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "BannerView(" +
            "text=${text()}, " +
            "components=${components()}, " +
            "type=${type()}, " +
            "modifier=${modifier()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBBannerView?): BannerView? {
            return fb?.let { BannerViewFBWrapper(it) }
        }
    }
}
