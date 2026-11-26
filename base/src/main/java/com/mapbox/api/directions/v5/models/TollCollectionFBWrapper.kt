package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbTollCollection
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class TollCollectionFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbTollCollection,
) : TollCollection(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun type(): String? {
        return when (fb.typeOrNull(b)?.let { FBTollCollectionType.fromByteOrThrow(it) } ?: return null) {
            FBTollCollectionType.TollBooth -> "toll_booth"
            FBTollCollectionType.TollGantry -> "toll_gantry"
            FBTollCollectionType.Unknown -> unrecognizeFlexBufferMap?.get("type")?.asString()
        }
    }

    override fun name(): String? = fb.name(b)

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
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "TollCollection(" +
            "type=${type()}, " +
            "name=${name()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbTollCollection): TollCollection? {
            return if (fb.isPresent) TollCollectionFBWrapper(b, fb) else null
        }
    }
}
