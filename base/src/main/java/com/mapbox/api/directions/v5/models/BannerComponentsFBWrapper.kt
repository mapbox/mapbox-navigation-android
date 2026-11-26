package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBannerComponents
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class BannerComponentsFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbBannerComponents,
) : BannerComponents(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun text(): String = fb.text(b)

    override fun type(): String {
        return when (FBBannerComponentType.fromByteOrThrow(fb.type(b))) {
            FBBannerComponentType.Text -> TEXT
            FBBannerComponentType.Icon -> ICON
            FBBannerComponentType.Delimiter -> DELIMITER
            FBBannerComponentType.ExitNumber -> EXIT_NUMBER
            FBBannerComponentType.Exit -> EXIT
            FBBannerComponentType.Lane -> LANE
            FBBannerComponentType.GuidanceView -> GUIDANCE_VIEW
            FBBannerComponentType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
                ?: "unknown"
        }
    }

    override fun subType(): String? {
        val subType = fb.subTypeOrNull(b)?.let { FBBannerComponentSubType.fromByteOrThrow(it) } ?: return null
        return when (subType) {
            FBBannerComponentSubType.Aftertoll -> AFTERTOLL
            FBBannerComponentSubType.Cityreal -> CITYREAL
            FBBannerComponentSubType.ExpresswayEntrance -> EXPRESSWAY_ENTRANCE
            FBBannerComponentSubType.ExpresswayExit -> EXPRESSWAY_EXIT
            FBBannerComponentSubType.Jct -> JCT
            FBBannerComponentSubType.Sapa -> SAPA
            FBBannerComponentSubType.Sapaguidemap -> SAPAGUIDEMAP
            FBBannerComponentSubType.Signboard -> SIGNBOARD
            FBBannerComponentSubType.Tollbranch -> TOLLBRANCH
            FBBannerComponentSubType.Unknown ->
                unrecognizeFlexBufferMap?.get("subType")?.asString()
        }
    }

    override fun abbreviation(): String? = fb.abbreviation(b)

    override fun abbreviationPriority(): Int? = fb.abbreviationPriorityOrNull(b)

    override fun imageBaseUrl(): String? = fb.imageBaseUrl(b)

    override fun mapboxShield(): MapboxShield? {
        return MapboxShieldFBWrapper.wrap(b, fb.mapboxShield(b))
    }

    override fun imageUrl(): String? = fb.imageUrl(b)

    override fun directions(): List<String?>? {
        val directions = fb.directions(b)
        return FlatbuffersListWrapper.get(directions.size) {
            directions.get(b, it)
        }
    }

    override fun active(): Boolean? = fb.activeOrNull(b)

    override fun activeDirection(): String? = fb.activeDirection(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("BannerComponents#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is BannerComponents && other !is BannerComponentsFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is BannerComponentsFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbBannerComponents): BannerComponents? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> BannerComponentsFBWrapper(b, fb)
            }
        }
    }
}
