package com.mapbox.navigation.base.typedef

import androidx.annotation.StringDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        RouteConstants.STEP_MANEUVER_MODIFIER_UTURN,
        RouteConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        RouteConstants.STEP_MANEUVER_MODIFIER_RIGHT,
        RouteConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        RouteConstants.STEP_MANEUVER_MODIFIER_STRAIGHT,
        RouteConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        RouteConstants.STEP_MANEUVER_MODIFIER_LEFT,
        RouteConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT
)
annotation class ManeuverModifier
