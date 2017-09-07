package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.HashMap;
import java.util.Map;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_NONE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_UTURN;

class TurnLaneMap {

  private Map<String, Integer> turnLaneMap;

  TurnLaneMap() {
    turnLaneMap = new HashMap<>();

    // Left
    turnLaneMap.put(TURN_LANE_INDICATION_LEFT, R.drawable.lane_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SHARP_LEFT, R.drawable.lane_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SLIGHT_LEFT, R.drawable.lane_right);

    // Left / Straight valid - maneuver modifier left
    turnLaneMap.put(TURN_LANE_INDICATION_LEFT + TURN_LANE_INDICATION_STRAIGHT
        + STEP_MANEUVER_MODIFIER_LEFT, R.drawable.lane_right_only);
    turnLaneMap.put(TURN_LANE_INDICATION_SHARP_LEFT + TURN_LANE_INDICATION_STRAIGHT
      + STEP_MANEUVER_MODIFIER_LEFT, R.drawable.lane_right_only);
    turnLaneMap.put(TURN_LANE_INDICATION_SLIGHT_LEFT + TURN_LANE_INDICATION_STRAIGHT
      + STEP_MANEUVER_MODIFIER_LEFT, R.drawable.lane_right_only);

    // Left / Straight valid - maneuver modifier straight
    turnLaneMap.put(TURN_LANE_INDICATION_LEFT + TURN_LANE_INDICATION_STRAIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SHARP_LEFT + TURN_LANE_INDICATION_STRAIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SLIGHT_LEFT + TURN_LANE_INDICATION_STRAIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);

    // Straight
    turnLaneMap.put(TURN_LANE_INDICATION_NONE, R.drawable.lane_straight);
    turnLaneMap.put(TURN_LANE_INDICATION_NONE + STEP_MANEUVER_MODIFIER_RIGHT, R.drawable.lane_right);
    turnLaneMap.put(TURN_LANE_INDICATION_NONE + STEP_MANEUVER_MODIFIER_LEFT, R.drawable.lane_right);

    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT, R.drawable.lane_straight);
    turnLaneMap.put(TURN_LANE_INDICATION_UTURN, R.drawable.lane_uturn);

    // Right valid
    turnLaneMap.put(TURN_LANE_INDICATION_RIGHT, R.drawable.lane_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SHARP_RIGHT, R.drawable.lane_right);
    turnLaneMap.put(TURN_LANE_INDICATION_SLIGHT_RIGHT, R.drawable.lane_right);

    // Right / Straight valid - maneuver modifier right
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_RIGHT
      + STEP_MANEUVER_MODIFIER_RIGHT, R.drawable.lane_right_only);
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_SHARP_RIGHT
      + STEP_MANEUVER_MODIFIER_RIGHT, R.drawable.lane_right_only);
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_SLIGHT_RIGHT
      + STEP_MANEUVER_MODIFIER_RIGHT, R.drawable.lane_right_only);

    // Right / Straight valid - maneuver modifier straight
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_RIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_SHARP_RIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);
    turnLaneMap.put(TURN_LANE_INDICATION_STRAIGHT + TURN_LANE_INDICATION_SLIGHT_RIGHT
      + STEP_MANEUVER_MODIFIER_STRAIGHT, R.drawable.lane_straight_only_right);
  }

  int getTurnLaneResource(String turnLaneKey) {
    if (turnLaneMap.get(turnLaneKey) != null) {
      return turnLaneMap.get(turnLaneKey);
    }
    return 0;
  }
}
