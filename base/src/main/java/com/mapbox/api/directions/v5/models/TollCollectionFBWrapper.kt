package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TollCollectionFBWrapper private constructor(
    private val fb: FBTollCollection,
) : TollCollection(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String? {
        return when (fb.type ?: return null) {
            FBTollCollectionType.TollBooth -> "toll_booth"
            FBTollCollectionType.TollGantry -> "toll_gantry"
            FBTollCollectionType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
        }
    }

    override fun name(): String? = fb.name

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("TollCollection#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is TollCollection && other !is TollCollectionFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is TollCollectionFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "TollCollection(" +
            "type=${type()}, " +
            "name=${name()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBTollCollection?): TollCollection? {
            return fb?.let { TollCollectionFBWrapper(it) }
        }
    }
}
