package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.fbToManeuverModifierType
import com.mapbox.api.directions.v5.models.utils.fbToStepManeuverType
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBannerView
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerViewFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbBannerView,
) : BannerView(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun text(): String = fb.text(b)

    override fun components(): List<BannerComponents?>? {
        val components = fb.components(b)
        return FlatbuffersListWrapper.get(components.size) {
            BannerComponentsFBWrapper.wrap(b, components.get(b, it))
        }
    }

    override fun type(): String? = fb.typeOrNull(b)?.let { FBManeuverType.fromByteOrThrow(it) }.fbToStepManeuverType(
        "type",
        unrecognizeFlexBufferMap,
    )

    override fun modifier(): String? = fb.modifierOrNull(b)?.fbToManeuverModifierType(
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
        if (other == null) return false
        if (other is BannerView && other !is BannerViewFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is BannerViewFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "BannerView(" +
            "text=${text()}, " +
            "components=${components()}, " +
            "type=${type()}, " +
            "modifier=${modifier()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbBannerView): BannerView? {
            return if (fb.isPresent) BannerViewFBWrapper(b, fb) else null
        }
    }
}
