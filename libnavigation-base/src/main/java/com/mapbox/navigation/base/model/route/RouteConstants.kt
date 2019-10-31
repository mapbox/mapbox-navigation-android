package com.mapbox.navigation.base.model.route

object RouteConstants {

    internal const val NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG = 1500
    internal const val ROUTE_REFRESH_INTERVAL = 5 * 60 * 1000L

    // Step Maneuver Types
    const val STEP_MANEUVER_TYPE_TURN = "turn"
    const val STEP_MANEUVER_TYPE_NEW_NAME = "new name"
    const val STEP_MANEUVER_TYPE_DEPART = "depart"
    const val STEP_MANEUVER_TYPE_ARRIVE = "arrive"
    const val STEP_MANEUVER_TYPE_MERGE = "merge"
    const val STEP_MANEUVER_TYPE_ON_RAMP = "on ramp"
    const val STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp"
    const val STEP_MANEUVER_TYPE_FORK = "fork"
    const val STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road"
    const val STEP_MANEUVER_TYPE_CONTINUE = "continue"
    const val STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout"
    const val STEP_MANEUVER_TYPE_ROTARY = "rotary"
    const val STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn"
    const val STEP_MANEUVER_TYPE_NOTIFICATION = "notification"
    const val STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout"
    const val STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary"

    // Step Maneuver Modifiers
    const val STEP_MANEUVER_MODIFIER_UTURN = "uturn"
    const val STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right"
    const val STEP_MANEUVER_MODIFIER_RIGHT = "right"
    const val STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right"
    const val STEP_MANEUVER_MODIFIER_STRAIGHT = "straight"
    const val STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left"
    const val STEP_MANEUVER_MODIFIER_LEFT = "left"
    const val STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left"

    // Turn Lane Indication
    const val TURN_LANE_INDICATION_LEFT = "left"
    const val TURN_LANE_INDICATION_SLIGHT_LEFT = "slight left"
    const val TURN_LANE_INDICATION_STRAIGHT = "straight"
    const val TURN_LANE_INDICATION_RIGHT = "right"
    const val TURN_LANE_INDICATION_SLIGHT_RIGHT = "slight right"
    const val TURN_LANE_INDICATION_UTURN = "uturn"

    // Distance Rounding Increments
    const val ROUNDING_INCREMENT_FIVE = 5
    const val ROUNDING_INCREMENT_TEN = 10
    const val ROUNDING_INCREMENT_TWENTY_FIVE = 25
    const val ROUNDING_INCREMENT_FIFTY = 50
    const val ROUNDING_INCREMENT_ONE_HUNDRED = 100

    // Profiles
    const val PROFILE_DRIVING_TRAFFIC = "driving-traffic"
    const val PROFILE_DRIVING = "driving"
    const val PROFILE_WALKING = "walking"
    const val PROFILE_CYCLING = "cycling"

    // Distance Metrics
    const val IMPERIAL = "imperial"
    const val METRIC = "metric"

    // TimeFormat
    const val NONE_SPECIFIED = -1
    const val TWELVE_HOURS = 0
    const val TWENTY_FOUR_HOURS = 1
}
