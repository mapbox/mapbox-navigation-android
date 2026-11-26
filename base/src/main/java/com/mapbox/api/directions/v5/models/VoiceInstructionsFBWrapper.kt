package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbVoiceInstructions
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class VoiceInstructionsFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbVoiceInstructions,
) : VoiceInstructions(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun distanceAlongGeometry(): Double? = fb.distanceAlongGeometryOrNull(b)

    override fun announcement(): String? = fb.announcement(b)

    override fun ssmlAnnouncement(): String? = fb.ssmlAnnouncement(b)

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
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "VoiceInstructions(" +
            "distanceAlongGeometry=${distanceAlongGeometry()}, " +
            "announcement=${announcement()}, " +
            "ssmlAnnouncement=${ssmlAnnouncement()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbVoiceInstructions): VoiceInstructions? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> VoiceInstructionsFBWrapper(b, fb)
            }
        }
    }
}
