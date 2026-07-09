package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class VoiceInstructionsFBWrapper private constructor(
    private val fb: FBVoiceInstructions,
) : VoiceInstructions(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun distanceAlongGeometry(): Double? = fb.distanceAlongGeometry

    override fun announcement(): String? = fb.announcement

    override fun ssmlAnnouncement(): String? = fb.ssmlAnnouncement

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("VoiceInstructions#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is VoiceInstructions && other !is VoiceInstructionsFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is VoiceInstructionsFBWrapper) return false
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "VoiceInstructions(" +
            "distanceAlongGeometry=${distanceAlongGeometry()}, " +
            "announcement=${announcement()}, " +
            "ssmlAnnouncement=${ssmlAnnouncement()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBVoiceInstructions?): VoiceInstructions? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> VoiceInstructionsFBWrapper(fb)
            }
        }
    }
}
