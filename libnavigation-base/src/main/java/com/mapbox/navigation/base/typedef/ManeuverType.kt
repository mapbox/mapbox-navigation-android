package com.mapbox.navigation.base.typedef

import androidx.annotation.StringDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        RouteConstants.STEP_MANEUVER_TYPE_TURN,
        RouteConstants.STEP_MANEUVER_TYPE_NEW_NAME,
        RouteConstants.STEP_MANEUVER_TYPE_DEPART,
        RouteConstants.STEP_MANEUVER_TYPE_ARRIVE,
        RouteConstants.STEP_MANEUVER_TYPE_MERGE,
        RouteConstants.STEP_MANEUVER_TYPE_ON_RAMP,
        RouteConstants.STEP_MANEUVER_TYPE_OFF_RAMP,
        RouteConstants.STEP_MANEUVER_TYPE_FORK,
        RouteConstants.STEP_MANEUVER_TYPE_END_OF_ROAD,
        RouteConstants.STEP_MANEUVER_TYPE_CONTINUE,
        RouteConstants.STEP_MANEUVER_TYPE_ROUNDABOUT,
        RouteConstants.STEP_MANEUVER_TYPE_ROTARY,
        RouteConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN,
        RouteConstants.STEP_MANEUVER_TYPE_NOTIFICATION,
        RouteConstants.STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT,
        RouteConstants.STEP_MANEUVER_TYPE_EXIT_ROTARY
)
annotation class ManeuverType
