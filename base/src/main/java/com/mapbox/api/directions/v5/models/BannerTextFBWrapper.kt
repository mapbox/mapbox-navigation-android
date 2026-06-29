package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.fbToManeuverModifierType
import com.mapbox.api.directions.v5.models.utils.fbToStepManeuverType
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerTextFBWrapper private constructor(
    private val fb: FBBannerText,
) : BannerText(), BaseFBWrapper {

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

    override fun degrees(): Double? = fb.degrees

    override fun drivingSide(): String? {
        return when (FBDrivingSide.fromByteOrThrow(fb.drivingSide ?: return null)) {
            FBDrivingSide.Left -> "left"
            FBDrivingSide.Right -> "right"
            FBDrivingSide.Unknown -> unrecognizeFlexBufferMap?.get("driving_side")?.asString()
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerText#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BannerTextFBWrapper && other.fb === fb) return true
        if (other is BannerTextFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "BannerText(" +
            "text=${text()}, " +
            "components=${components()}, " +
            "type=${type()}, " +
            "modifier=${modifier()}, " +
            "degrees=${degrees()}, " +
            "drivingSide=${drivingSide()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBBannerText?): BannerText? {
            return fb?.let { BannerTextFBWrapper(it) }
        }
    }
}
