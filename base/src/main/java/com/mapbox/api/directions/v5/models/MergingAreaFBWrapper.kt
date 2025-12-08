package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class MergingAreaFBWrapper(
    private val fb: FBMergingArea,
) : MergingArea(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String? = fb.type
        ?.fbToMergingAreaType("type", unrecognizeFlexBufferMap)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("MergingArea#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MergingAreaFBWrapper && other.fb === fb) return true
        if (other is MergingAreaFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "MergingArea(type=${type()})"
    }

    private companion object {

        @Type
        fun Byte.fbToMergingAreaType(
            propertyName: String,
            unrecognized: FlexBuffers.Map? = null,
        ): String? {
            return when (this) {
                FBMergingAreaType.FromLeft -> TYPE_FROM_LEFT
                FBMergingAreaType.FromRight -> TYPE_FROM_RIGHT
                FBMergingAreaType.FromBothSides -> TYPE_FROM_BOTH_SIDES
                FBMergingAreaType.Unknown -> unrecognized?.get(propertyName)?.asString()
                else -> unhandledEnumMapping(propertyName, this)
            }
        }
    }
}
