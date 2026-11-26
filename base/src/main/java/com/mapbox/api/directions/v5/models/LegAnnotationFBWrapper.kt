package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbCongestionLevelEnumWrapper
import com.mapbox.directions.accessors.FbLegAnnotation
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import com.mapbox.navigation.base.internal.StateOfCharge
import java.nio.ByteBuffer

internal class LegAnnotationFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbLegAnnotation,
) : LegAnnotation(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

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
        val distance = fb.distance(b)
        return FlatbuffersListWrapper.get(distance.size) {
            distance.get(b, it)
        }
    }

    override fun duration(): List<Double>? {
        val duration = fb.duration(b)
        return FlatbuffersListWrapper.get(duration.size) {
            duration.get(b, it)
        }
    }

    override fun speed(): List<Double>? {
        val speed = fb.speed(b)
        return FlatbuffersListWrapper.get(speed.size) {
            speed.get(b, it)
        }
    }

    override fun congestion(): List<String?>? {
        val congestion = fb.congestion(b)
        return FlatbuffersListWrapper.get(congestion.size) {
            congestion.get(b, it).toCongestionLevelString(b)
        }
    }

    override fun congestionNumeric(): List<Int?>? {
        val congestionNumeric = fb.congestionNumeric(b)
        return FlatbuffersListWrapper.get(congestionNumeric.size) {
            val value = congestionNumeric.get(b, it)
            if (value == -1) {
                null
            } else {
                value
            }
        }
    }

    override fun maxspeed(): List<MaxSpeed?>? {
        val maxspeed = fb.maxspeed(b)
        return FlatbuffersListWrapper.get(maxspeed.size) {
            MaxSpeedFBWrapper.wrap(b, maxspeed.get(b, it))
        }
    }

    override fun trafficTendency(): List<Int>? {
        val trafficTendency = fb.trafficTendency(b)
        return FlatbuffersListWrapper.get(trafficTendency.size) {
            trafficTendency.get(b, it)
        }
    }

    override fun freeflowSpeed(): List<Int?>? {
        val freeflowSpeed = fb.freeflowSpeed(b)
        return FlatbuffersListWrapper.get(freeflowSpeed.size) {
            freeflowSpeed.get(b, it).let {
                if (it == -1) null else it
            }
        }
    }

    override fun currentSpeed(): List<Int?>? {
        val currentSpeed = fb.currentSpeed(b)
        return FlatbuffersListWrapper.get(currentSpeed.size) {
            currentSpeed.get(b, it).let {
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
        if (other == null) return false
        if (other is LegAnnotation && other !is LegAnnotationFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is LegAnnotationFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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

        internal fun wrap(b: FbBuffer, fb: FbLegAnnotation): LegAnnotation? {
            return if (fb.isPresent) LegAnnotationFBWrapper(b, fb) else null
        }

        private fun FbCongestionLevelEnumWrapper.toCongestionLevelString(b: FbBuffer): String? {
            return if (!isPresent || isNull(b)) {
                null
            } else {
                when (FBCongestionLevel.fromByteOrThrow(value(b))) {
                    FBCongestionLevel.Low -> "low"
                    FBCongestionLevel.Moderate -> "moderate"
                    FBCongestionLevel.Heavy -> "heavy"
                    FBCongestionLevel.Severe -> "severe"
                    FBCongestionLevel.UnknownCongestion -> "unknown"
                    FBCongestionLevel.Unknown ->
                        unrecognizedValue(b)
                            ?: throw IllegalStateException(
                                "Unknown congestion level with no unrecognized value",
                            )
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
