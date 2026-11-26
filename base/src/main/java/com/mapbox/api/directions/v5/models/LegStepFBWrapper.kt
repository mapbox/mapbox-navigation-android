package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbLegStep
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class LegStepFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbLegStep,
) : LegStep(), BaseFBWrapper {

    internal val geometryNumeric: List<Point>?
        get() {
            val geometry = fb.geometryNumeric(b)
            return FlatbuffersListWrapper.get(geometry.size) {
                val coordinate = geometry.get(b, it)
                Point.fromLngLat(coordinate.longitude(b), coordinate.latitude(b))
            }
        }

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun distance(): Double = fb.distance(b)

    override fun duration(): Double = fb.duration(b)

    override fun durationTypical(): Double? = fb.durationTypicalOrNull(b)

    override fun speedLimitUnit(): String? {
        return when (FBSpeedLimitUnit.fromByteOrThrow(fb.speedLimitUnitOrNull(b) ?: return null)) {
            FBSpeedLimitUnit.Kmph -> SpeedLimit.KMPH
            FBSpeedLimitUnit.Mph -> SpeedLimit.MPH
            FBSpeedLimitUnit.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitUnit")?.asString()
        }
    }

    override fun speedLimitSign(): String? {
        return when (FBSpeedLimitSign.fromByteOrThrow(fb.speedLimitSignOrNull(b) ?: return null)) {
            FBSpeedLimitSign.Mutcd -> LegStep.MUTCD
            FBSpeedLimitSign.Vienna -> LegStep.VIENNA
            FBSpeedLimitSign.Unknown -> unrecognizeFlexBufferMap?.get("speedLimitSign")?.asString()
        }
    }

    override fun geometry(): String? = fb.geometry(b)

    override fun name(): String? = fb.name(b)

    override fun ref(): String? = fb.ref(b)

    override fun destinations(): String? = fb.destinations(b)

    override fun mode(): String {
        return when (FBTransportMode.fromByteOrThrow(fb.mode(b))) {
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

    override fun pronunciation(): String? = fb.pronunciation(b)

    override fun rotaryName(): String? = fb.rotaryName(b)

    override fun rotaryPronunciation(): String? = fb.rotaryPronunciation(b)

    override fun maneuver(): StepManeuver {
        return StepManeuverFBWrapper.wrap(b, fb.maneuver(b))!!
    }

    override fun voiceInstructions(): List<VoiceInstructions?>? {
        val voiceInstructions = fb.voiceInstructions(b)
        return FlatbuffersListWrapper.get(voiceInstructions.size) {
            VoiceInstructionsFBWrapper.wrap(b, voiceInstructions.get(b, it))
        }
    }

    override fun bannerInstructions(): List<BannerInstructions?>? {
        val bannerInstructions = fb.bannerInstructions(b)
        return FlatbuffersListWrapper.get(bannerInstructions.size) {
            BannerInstructionsFBWrapper.wrap(b, bannerInstructions.get(b, it))
        } ?: emptyList() // TODO: https://mapbox.atlassian.net/browse/NAVAND-6540
    }

    override fun drivingSide(): String? {
        return when (FBDrivingSide.fromByteOrThrow(fb.drivingSideOrNull(b) ?: return null)) {
            FBDrivingSide.Left -> "left"
            FBDrivingSide.Right -> "right"
            FBDrivingSide.Unknown -> unrecognizeFlexBufferMap?.get("driving_side")?.asString()
        }
    }

    override fun weight(): Double = fb.weight(b)

    override fun intersections(): List<StepIntersection?>? {
        val intersections = fb.intersections(b)
        return FlatbuffersListWrapper.get(intersections.size) {
            StepIntersectionFBWrapper.wrap(b, intersections.get(b, it))
        }
    }

    override fun exits(): String? = fb.exits(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("LegStep#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is LegStep && other !is LegStepFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is LegStepFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

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
        internal fun wrap(b: FbBuffer, fb: FbLegStep): LegStep? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> LegStepFBWrapper(b, fb)
            }
        }
    }
}
