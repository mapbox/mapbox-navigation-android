package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.R;

import java.util.HashMap;
import java.util.Map;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_CONTINUE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_DEPART;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_MERGE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_NEW_NAME;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_TURN;

class ManeuverMap {

  private Map<String, Integer> maneuverMap;

  ManeuverMap() {
    maneuverMap = new HashMap<>();
    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN,
      R.drawable.ic_maneuver_turn_180);
    maneuverMap.put(STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN,
      R.drawable.ic_maneuver_turn_180);

    maneuverMap.put(STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_turn_0);

    maneuverMap.put(STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_arrive_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_arrive_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ARRIVE,
      R.drawable.ic_maneuver_arrive);

    maneuverMap.put(STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_depart_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_depart_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_DEPART, R.drawable.ic_maneuver_depart);

    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
      R.drawable.ic_maneuver_turn_75);
    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_turn_45);
    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_turn_30);

    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
      R.drawable.ic_maneuver_turn_75_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_turn_45_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_turn_30_left);

    maneuverMap.put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_merge_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_merge_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_merge_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_merge_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_turn_0);

    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
      R.drawable.ic_maneuver_turn_75_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_turn_45_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_turn_30_left);

    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
      R.drawable.ic_maneuver_turn_75);
    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_turn_45);
    maneuverMap.put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_turn_30);

    maneuverMap.put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_off_ramp_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_off_ramp_slight_left);

    maneuverMap.put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_off_ramp_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_off_ramp_slight_right);

    maneuverMap.put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_fork_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_fork_slight_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_fork_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_fork_slight_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_fork_straight);
    maneuverMap.put(STEP_MANEUVER_TYPE_FORK, R.drawable.ic_maneuver_fork);

    maneuverMap.put(STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_end_of_road_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_end_of_road_right);

    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_roundabout_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
      R.drawable.ic_maneuver_roundabout_sharp_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_roundabout_slight_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_roundabout_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
      R.drawable.ic_maneuver_roundabout_sharp_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_roundabout_slight_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_roundabout_straight);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT, R.drawable.ic_maneuver_roundabout);

    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_roundabout_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
      R.drawable.ic_maneuver_roundabout_sharp_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_roundabout_slight_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_roundabout_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
      R.drawable.ic_maneuver_roundabout_sharp_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_roundabout_slight_right);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_roundabout_straight);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROTARY, R.drawable.ic_maneuver_roundabout);

    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_turn_45_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_turn_45);

    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT,
      R.drawable.ic_maneuver_turn_45_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
      R.drawable.ic_maneuver_turn_75_left);
    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
      R.drawable.ic_maneuver_turn_30_left);

    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT,
      R.drawable.ic_maneuver_turn_45);
    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
      R.drawable.ic_maneuver_turn_75);
    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
      R.drawable.ic_maneuver_turn_30);
    maneuverMap.put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_turn_0);

    maneuverMap.put(STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT,
      R.drawable.ic_maneuver_turn_0);
  }

  public int getManeuverResource(String maneuver) {
    if (maneuverMap.get(maneuver) != null) {
      return maneuverMap.get(maneuver);
    } else {
      return R.drawable.ic_maneuver_turn_0;
    }
  }
}
