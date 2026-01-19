package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class LegStepFBWrapper(
    private val fb: FBLegStep,
) : LegStep(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun distance(): Double = fb.distance

    override fun duration(): Double = fb.duration

    override fun durationTypical(): Double? = fb.durationTypical

    override fun speedLimitUnit(): String? {
        return when (fb.speedLimitUnit) {
            null -> null
            FBSpeedLimitUnit.Kmph -> SpeedLimit.KMPH
            FBSpeedLimitUnit.Mph -> SpeedLimit.MPH
            FBSpeedLimitUnit.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitUnit")?.asString()
            else -> unhandledEnumMapping("speedLimitUnit", fb.speedLimitUnit)
        }
    }

    override fun speedLimitSign(): String? {
        return when (fb.speedLimitSign) {
            null -> null
            FBSpeedLimitSign.Mutcd -> LegStep.MUTCD
            FBSpeedLimitSign.Vienna -> LegStep.VIENNA
            FBSpeedLimitSign.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitSign")?.asString()
            else -> unhandledEnumMapping("speedLimitSign", fb.speedLimitSign)
        }
    }

    override fun geometry(): String? = fb.geometry

    override fun name(): String? = fb.name

    override fun ref(): String? = fb.ref

    override fun destinations(): String? = fb.destinations

    override fun mode(): String {
        return when (fb.mode) {
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
            else -> unhandledEnumMapping("mode", fb.mode)
        }
    }

    override fun pronunciation(): String? = fb.pronunciation

    override fun rotaryName(): String? = fb.rotaryName

    override fun rotaryPronunciation(): String? = fb.rotaryPronunciation

    override fun maneuver(): StepManeuver {
        return StepManeuverFBWrapper(fb.maneuver)
    }

    override fun voiceInstructions(): List<VoiceInstructions?>? {
        return FlatbuffersListWrapper.get(fb.voiceInstructionsLength) {
            fb.voiceInstructions(it)?.let { instruction ->
                VoiceInstructionsFBWrapper(instruction)
            }
        }
    }

    override fun bannerInstructions(): List<BannerInstructions?>? {
        return FlatbuffersListWrapper.get(fb.bannerInstructionsLength) {
            fb.bannerInstructions(it)?.let { instructions ->
                BannerInstructionsFBWrapper(instructions)
            }
        } ?: emptyList() // TODO: https://mapbox.atlassian.net/browse/NAVAND-6540
    }

    override fun drivingSide(): String? {
        val drivingSide = fb.drivingSide ?: return null
        return when (drivingSide) {
            FBDrivingSide.Left -> "left"
            FBDrivingSide.Right -> "right"
            FBDrivingSide.Unknown -> unrecognizeFlexBufferMap?.get("driving_side")?.asString()
            else -> unhandledEnumMapping("driving_side", fb.drivingSide)
        }
    }

    override fun weight(): Double = fb.weight

    override fun intersections(): List<StepIntersection?>? {
        return FlatbuffersListWrapper.get(fb.intersectionsLength) {
            fb.intersections(it)?.let { intersections ->
                StepIntersectionFBWrapper(intersections)
            }
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
}
