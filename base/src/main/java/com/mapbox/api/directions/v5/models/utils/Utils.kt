package com.mapbox.api.directions.v5.models.utils

import androidx.annotation.RestrictTo
import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.models.FBCoordinate
import com.mapbox.api.directions.v5.models.FBLaneIndication
import com.mapbox.api.directions.v5.models.FBManeuverModifier
import com.mapbox.api.directions.v5.models.FBManeuverType
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal fun FBCoordinate.toDoubleArray(): DoubleArray? {
    return doubleArrayOf(longitude, latitude)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal fun FBCoordinate?.toDoubleArrayOrEmpty(): DoubleArray {
    return this?.toDoubleArray() ?: doubleArrayOf()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal fun FBCoordinate.toPoint(): Point {
    return Point.fromLngLat(longitude, latitude)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@StepManeuver.StepManeuverType
internal fun Byte?.fbToStepManeuverType(
    propertyName: String,
    unrecognized: FlexBuffers.Map?,
): String? {
    return when (this) {
        FBManeuverType.Turn -> StepManeuver.TURN
        FBManeuverType.NewName -> StepManeuver.NEW_NAME
        FBManeuverType.Depart -> StepManeuver.DEPART
        FBManeuverType.Arrive -> StepManeuver.ARRIVE
        FBManeuverType.Merge -> StepManeuver.MERGE
        FBManeuverType.OnRamp -> StepManeuver.ON_RAMP
        FBManeuverType.OffRamp -> StepManeuver.OFF_RAMP
        FBManeuverType.Fork -> StepManeuver.FORK
        FBManeuverType.EndOfRoad -> StepManeuver.END_OF_ROAD
        FBManeuverType.Continue -> StepManeuver.CONTINUE
        FBManeuverType.Roundabout -> StepManeuver.ROUNDABOUT
        FBManeuverType.Rotary -> StepManeuver.ROTARY
        FBManeuverType.RoundaboutTurn -> StepManeuver.ROUNDABOUT_TURN
        FBManeuverType.Notification -> StepManeuver.NOTIFICATION
        FBManeuverType.ExitRoundabout -> StepManeuver.EXIT_ROUNDABOUT
        FBManeuverType.ExitRotary -> StepManeuver.EXIT_ROTARY
        FBManeuverType.Unknown -> unrecognized?.get(propertyName)?.asString()
        null -> null
        else -> unhandledEnumMapping(propertyName, this)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@ManeuverModifier.Type
internal fun Byte.fbToManeuverModifierType(
    propertyName: String,
    unrecognized: FlexBuffers.Map?,
): String? {
    return when (this) {
        FBManeuverModifier.Straight -> ManeuverModifier.STRAIGHT
        FBManeuverModifier.Left -> ManeuverModifier.LEFT
        FBManeuverModifier.Right -> ManeuverModifier.RIGHT
        FBManeuverModifier.SlightLeft -> ManeuverModifier.SLIGHT_LEFT
        FBManeuverModifier.SlightRight -> ManeuverModifier.SLIGHT_RIGHT
        FBManeuverModifier.SharpLeft -> ManeuverModifier.SHARP_LEFT
        FBManeuverModifier.SharpRight -> ManeuverModifier.SHARP_RIGHT
        FBManeuverModifier.Uturn -> ManeuverModifier.UTURN
        FBManeuverModifier.Unknown -> unrecognized?.get(propertyName)?.asString()
        else -> unhandledEnumMapping(propertyName, this)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@ManeuverModifier.Type
internal inline fun Byte.fbToLineIndication(
    propertyName: String,
    unrecognizedGetter: () -> String?,
): String? {
    return when (this) {
        FBLaneIndication.Unknown -> unrecognizedGetter()
        FBLaneIndication.Straight -> ManeuverModifier.STRAIGHT
        FBLaneIndication.Left -> ManeuverModifier.LEFT
        FBLaneIndication.Right -> ManeuverModifier.RIGHT
        FBLaneIndication.SlightLeft -> ManeuverModifier.SLIGHT_LEFT
        FBLaneIndication.SlightRight -> ManeuverModifier.SLIGHT_RIGHT
        FBLaneIndication.SharpLeft -> ManeuverModifier.SHARP_LEFT
        FBLaneIndication.SharpRight -> ManeuverModifier.SHARP_RIGHT
        FBLaneIndication.Uturn -> ManeuverModifier.UTURN
        FBLaneIndication.None -> null
        else -> unhandledEnumMapping(propertyName, this)
    }
}
