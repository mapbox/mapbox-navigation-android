package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import com.mapbox.navigation.base.internal.StateOfCharge
import java.nio.ByteBuffer

internal class LegAnnotationFBWrapper private constructor(
    private val fb: FBLegAnnotation,
) : LegAnnotation(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    internal val stateOfCharge
        get(): StateOfCharge? {
            return unrecognizeFlexBufferMap?.let {
                val soc = it["state_of_charge"]
                if (soc.isVector) {
                    FBStateOfCharge(soc.asVector())
                } else {
                    null
                }
            }
        }

    override fun distance(): List<Double>? {
        return FlatbuffersListWrapper.get(fb.distanceLength) {
            fb.distance(it)
        }
    }

    override fun duration(): List<Double>? {
        return FlatbuffersListWrapper.get(fb.durationLength) {
            fb.duration(it)
        }
    }

    override fun speed(): List<Double>? {
        return FlatbuffersListWrapper.get(fb.speedLength) {
            fb.speed(it)
        }
    }

    override fun congestion(): List<String?>? {
        return FlatbuffersListWrapper.get(fb.congestionLength) {
            fb.congestion(it)?.toCongestionLevelString()
        }
    }

    override fun congestionNumeric(): List<Int?>? {
        return FlatbuffersListWrapper.get(fb.congestionNumericLength) {
            val value = fb.congestionNumeric(it)
            if (value == -1) {
                null
            } else {
                value
            }
        }
    }

    override fun maxspeed(): List<MaxSpeed?>? {
        return FlatbuffersListWrapper.get(fb.maxspeedLength) {
            MaxSpeedFBWrapper.wrap(fb.maxspeed(it))
        }
    }

    override fun trafficTendency(): List<Int>? {
        return FlatbuffersListWrapper.get(fb.trafficTendencyLength) {
            fb.trafficTendency(it)
        }
    }

    override fun freeflowSpeed(): List<Int?>? {
        return FlatbuffersListWrapper.get(fb.freeflowSpeedLength) {
            fb.freeflowSpeed(it).let {
                if (it == -1) null else it
            }
        }
    }

    override fun currentSpeed(): List<Int?>? {
        return FlatbuffersListWrapper.get(fb.currentSpeedLength) {
            fb.currentSpeed(it).let {
                if (it == -1) null else it
            }
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("LegAnnotation#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LegAnnotationFBWrapper && other.fb === fb) return true
        if (other is LegAnnotationFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "LegAnnotation(" +
            "distance=${distance()}, " +
            "duration=${duration()}, " +
            "speed=${speed()}, " +
            "congestion=${congestion()}, " +
            "congestionNumeric=${congestionNumeric()}, " +
            "maxspeed=${maxspeed()}, " +
            "trafficTendency=${trafficTendency()}, " +
            "freeflowSpeed=${freeflowSpeed()}, " +
            "currentSpeed=${currentSpeed()}" +
            ")"
    }

    internal companion object {

        internal fun wrap(fb: FBLegAnnotation?): LegAnnotation? {
            return fb?.let { LegAnnotationFBWrapper(it) }
        }

        private fun FBCongestionLevelEnumWrapper.toCongestionLevelString(): String? {
            return if (this.isNull) {
                null
            } else {
                when (this.value) {
                    FBCongestionLevel.Low -> "low"
                    FBCongestionLevel.Moderate -> "moderate"
                    FBCongestionLevel.Heavy -> "heavy"
                    FBCongestionLevel.Severe -> "severe"
                    FBCongestionLevel.UnknownCongestion -> "unknown"
                    FBCongestionLevel.Unknown ->
                        this.unrecognizedValue
                            ?: throw IllegalStateException(
                                "Unknown congestion level with no unrecognized value",
                            )

                    else -> unhandledEnumMapping("congestion", this.value)
                }
            }
        }
    }
}

private class FBStateOfCharge(
    val socVector: FlexBuffers.Vector,
) : StateOfCharge {
    override val size: Int
        get() = socVector.size()

    override fun get(index: Int): Double {
        return socVector[index].asFloat()
    }
}
