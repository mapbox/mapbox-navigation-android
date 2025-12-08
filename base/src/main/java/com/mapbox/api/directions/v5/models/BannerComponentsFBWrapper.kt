package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerComponentsFBWrapper(
    private val fb: FBBannerComponents,
) : BannerComponents(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun text(): String = fb.text

    override fun type(): String {
        return when (fb.type) {
            FBBannerComponentType.Text -> TEXT
            FBBannerComponentType.Icon -> ICON
            FBBannerComponentType.Delimiter -> DELIMITER
            FBBannerComponentType.ExitNumber -> EXIT_NUMBER
            FBBannerComponentType.Exit -> EXIT
            FBBannerComponentType.Lane -> LANE
            FBBannerComponentType.GuidanceView -> GUIDANCE_VIEW
            FBBannerComponentType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
                ?: "unknown"
            else -> unhandledEnumMapping("BannerComponents#type", fb.type)
        }
    }

    override fun subType(): String? {
        return when (fb.subType) {
            FBBannerComponentSubType.Aftertoll -> AFTERTOLL
            FBBannerComponentSubType.Cityreal -> CITYREAL
            FBBannerComponentSubType.ExpresswayEntrance -> EXPRESSWAY_ENTRANCE
            FBBannerComponentSubType.ExpresswayExit -> EXPRESSWAY_EXIT
            FBBannerComponentSubType.Jct -> JCT
            FBBannerComponentSubType.Sapa -> SAPA
            FBBannerComponentSubType.Sapaguidemap -> SAPAGUIDEMAP
            FBBannerComponentSubType.Signboard -> SIGNBOARD
            FBBannerComponentSubType.Tollbranch -> TOLLBRANCH
            FBBannerComponentSubType.Unknown -> unrecognizeFlexBufferMap?.get("subType")?.asString()
            else -> unhandledEnumMapping("BannerComponents#subType", fb.subType)
        }
    }

    override fun abbreviation(): String? = fb.abbreviation

    override fun abbreviationPriority(): Int? = fb.abbreviationPriority

    override fun imageBaseUrl(): String? = fb.imageBaseUrl

    override fun mapboxShield(): MapboxShield? {
        return fb.mapboxShield?.let { MapboxShieldFBWrapper(it) }
    }

    override fun imageUrl(): String? = fb.imageUrl

    override fun directions(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.directionsLength) {
            fb.directions(it)
        }
    }

    override fun active(): Boolean? = fb.active

    override fun activeDirection(): String? = fb.activeDirection

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerComponents#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BannerComponentsFBWrapper && other.fb === fb) return true
        if (other is BannerComponentsFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "BannerComponents(" +
            "text=${text()}, " +
            "type=${type()}, " +
            "subType=${subType()}, " +
            "abbreviation=${abbreviation()}, " +
            "abbreviationPriority=${abbreviationPriority()}, " +
            "imageBaseUrl=${imageBaseUrl()}, " +
            "mapboxShield=${mapboxShield()}, " +
            "imageUrl=${imageUrl()}, " +
            "directions=${directions()}, " +
            "active=${active()}, " +
            "activeDirection=${activeDirection()}" +
            ")"
    }
}
