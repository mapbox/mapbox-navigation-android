package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbMergingArea
import com.mapbox.directions.generated.MergingAreaType
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MergingAreaFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbMergingArea,
) : MergingArea(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun type(): String? = fb.typeOrNull(b)?.let { FBMergingAreaType.fromByteOrThrow(it) }
        ?.fbToMergingAreaType("type", unrecognizeFlexBufferMap)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MergingArea#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is MergingArea && other !is MergingAreaFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is MergingAreaFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "MergingArea(type=${type()})"
    }

    internal companion object {

        internal fun wrap(b: FbBuffer, fb: FbMergingArea): MergingArea? {
            return if (fb.isPresent) MergingAreaFBWrapper(b, fb) else null
        }

        @Type
        private fun MergingAreaType.fbToMergingAreaType(
            propertyName: String,
            unrecognized: FlexBuffers.Map? = null,
        ): String? {
            return when (this) {
                FBMergingAreaType.FromLeft -> TYPE_FROM_LEFT
                FBMergingAreaType.FromRight -> TYPE_FROM_RIGHT
                FBMergingAreaType.FromBothSides -> TYPE_FROM_BOTH_SIDES
                FBMergingAreaType.Unknown -> unrecognized?.get(propertyName)?.asString()
            }
        }
    }
}
