package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class LegStepFBWrapper private constructor(
    private val fb: FBLegStep,
) : LegStep(), BaseFBWrapper {

    internal val geometryNumeric
        get(): List<Point>? =
            FlatbuffersListWrapper.get(fb.geometryNumericLength) {
                val coordinate = fb.geometryNumeric(it)!!
                Point.fromLngLat(coordinate.longitude, coordinate.latitude)
            }

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun distance(): Double = fb.distance

    override fun duration(): Double = fb.duration

    override fun durationTypical(): Double? = fb.durationTypical

    override fun speedLimitUnit(): String? {
        return when (FBSpeedLimitUnit.fromByteOrThrow(fb.speedLimitUnit ?: return null)) {
            FBSpeedLimitUnit.Kmph -> SpeedLimit.KMPH
            FBSpeedLimitUnit.Mph -> SpeedLimit.MPH
            FBSpeedLimitUnit.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitUnit")?.asString()
        }
    }

    override fun speedLimitSign(): String? {
        return when (FBSpeedLimitSign.fromByteOrThrow(fb.speedLimitSign ?: return null)) {
            FBSpeedLimitSign.Mutcd -> LegStep.MUTCD
            FBSpeedLimitSign.Vienna -> LegStep.VIENNA
            FBSpeedLimitSign.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitSign")?.asString()
        }
    }

    override fun geometry(): String? = fb.geometry

    override fun name(): String? = fb.name

    override fun ref(): String? = fb.ref

    override fun destinations(): String? = fb.destinations

    override fun mode(): String {
        return when (FBTransportMode.fromByteOrThrow(fb.mode)) {
            FBTransportMode.Driving -> "driving"
            FBTransportMode.Walking -> "walking"
            FBTransportMode.Cycling -> "cycling"
            FBTransportMode.Ferry -> "ferry"
            FBTransportMode.Train -> "train"
            FBTransportMode.Unaccessible -> "unaccessible"
            FBTransportMode.Unknown -> unrecognizeFlexBufferMap?.get("mode")?.asString()
                ?: throw IllegalStateException(
                    "LegStep#mode is Unknown but mode is not found in unrecognized map",
                )
        }
    }

    override fun pronunciation(): String? = fb.pronunciation

    override fun rotaryName(): String? = fb.rotaryName

    override fun rotaryPronunciation(): String? = fb.rotaryPronunciation

    override fun maneuver(): StepManeuver {
        return StepManeuverFBWrapper.wrap(fb.maneuver)!!
    }

    override fun voiceInstructions(): List<VoiceInstructions?>? {
        return FlatbuffersListWrapper.get(fb.voiceInstructionsLength) {
            VoiceInstructionsFBWrapper.wrap(fb.voiceInstructions(it))
        }
    }

    override fun bannerInstructions(): List<BannerInstructions?>? {
        return FlatbuffersListWrapper.get(fb.bannerInstructionsLength) {
            BannerInstructionsFBWrapper.wrap(fb.bannerInstructions(it))
        } ?: emptyList() // TODO: https://mapbox.atlassian.net/browse/NAVAND-6540
    }

    override fun drivingSide(): String? {
        return when (FBDrivingSide.fromByteOrThrow(fb.drivingSide ?: return null)) {
            FBDrivingSide.Left -> "left"
            FBDrivingSide.Right -> "right"
            FBDrivingSide.Unknown -> unrecognizeFlexBufferMap?.get("driving_side")?.asString()
        }
    }

    override fun weight(): Double = fb.weight

    override fun intersections(): List<StepIntersection?>? {
        return FlatbuffersListWrapper.get(fb.intersectionsLength) {
            StepIntersectionFBWrapper.wrap(fb.intersections(it))
        }
    }

    override fun exits(): String? = fb.exits

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("LegStep#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LegStepFBWrapper && other.fb === fb) return true
        if (other is LegStepFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "LegStep(" +
            "distance=${distance()}, " +
            "duration=${duration()}, " +
            "durationTypical=${durationTypical()}, " +
            "speedLimitUnit=${speedLimitUnit()}, " +
            "speedLimitSign=${speedLimitSign()}, " +
            "geometry=${geometry()}, " +
            "name=${name()}, " +
            "ref=${ref()}, " +
            "destinations=${destinations()}, " +
            "mode=${mode()}, " +
            "pronunciation=${pronunciation()}, " +
            "rotaryName=${rotaryName()}, " +
            "rotaryPronunciation=${rotaryPronunciation()}, " +
            "maneuver=${maneuver()}, " +
            "voiceInstructions=${voiceInstructions()}, " +
            "bannerInstructions=${bannerInstructions()}, " +
            "drivingSide=${drivingSide()}, " +
            "weight=${weight()}, " +
            "intersections=${intersections()}, " +
            "exits=${exits()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBLegStep?): LegStep? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> LegStepFBWrapper(fb)
            }
        }
    }
}
