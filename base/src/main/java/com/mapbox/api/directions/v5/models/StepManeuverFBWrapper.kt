package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.fbToManeuverModifierType
import com.mapbox.api.directions.v5.models.utils.fbToStepManeuverType
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbStepManeuver
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class StepManeuverFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbStepManeuver,
) : StepManeuver(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun rawLocation(): DoubleArray {
        return fb.location(b).toDoubleArrayOrEmpty(b)
    }

    override fun bearingBefore(): Double? = fb.bearingBeforeOrNull(b)

    override fun bearingAfter(): Double? = fb.bearingAfterOrNull(b)

    override fun instruction(): String? = fb.instruction(b)

    override fun type(): String? = fb.typeOrNull(b)?.let { FBManeuverType.fromByteOrThrow(it) }
        .fbToStepManeuverType("type", unrecognizeFlexBufferMap)

    override fun modifier(): String? = fb.modifierOrNull(b)
        ?.fbToManeuverModifierType("modifier", unrecognizeFlexBufferMap)

    override fun exit(): Int? = fb.exitOrNull(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("StepManeuver#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is StepManeuver && other !is StepManeuverFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is StepManeuverFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "StepManeuver(" +
            "rawLocation=${rawLocation().contentToString()}, " +
            "bearingBefore=${bearingBefore()}, " +
            "bearingAfter=${bearingAfter()}, " +
            "instruction=${instruction()}, " +
            "type=${type()}, " +
            "modifier=${modifier()}, " +
            "exit=${exit()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbStepManeuver): StepManeuver? {
            return if (fb.isPresent) StepManeuverFBWrapper(b, fb) else null
        }
    }
}
