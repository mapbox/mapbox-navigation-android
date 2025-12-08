package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.fbToManeuverModifierType
import com.mapbox.api.directions.v5.models.utils.fbToStepManeuverType
import com.mapbox.api.directions.v5.models.utils.toDoubleArrayOrEmpty
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class StepManeuverFBWrapper(
    private val fb: FBStepManeuver,
) : StepManeuver(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun rawLocation(): DoubleArray {
        return fb.location.toDoubleArrayOrEmpty()
    }

    override fun bearingBefore(): Double? = fb.bearingBefore

    override fun bearingAfter(): Double? = fb.bearingAfter

    override fun instruction(): String? = fb.instruction

    override fun type(): String? = fb.type
        .fbToStepManeuverType("type", unrecognizeFlexBufferMap)

    override fun modifier(): String? = fb.modifier
        ?.fbToManeuverModifierType("modifier", unrecognizeFlexBufferMap)

    override fun exit(): Int? = fb.exit

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("StepManeuver#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is StepManeuverFBWrapper && other.fb === fb) return true
        if (other is StepManeuverFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
}
