package com.mapbox.services.android.navigation.v5.navigation;

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

final class ManeuverMap {

  private static final String NO_INSTANCE = "No Instance.";

  private ManeuverMap() {
    throw new AssertionError(NO_INSTANCE);
  }

  private static final Map<String, Integer> MANEUVER_MAP = new HashMap<String,
    Integer>() {
    {
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN,
        R.drawable.ic_maneuver_turn_180);
      put(STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN,
        R.drawable.ic_maneuver_turn_180);
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_180_left_driving_side);
      put(STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_180_left_driving_side);

      put(STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_turn_0);

      put(STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_arrive_left);
      put(STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_arrive_right);
      put(STEP_MANEUVER_TYPE_ARRIVE,
        R.drawable.ic_maneuver_arrive);

      put(STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_depart_left);
      put(STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_depart_right);
      put(STEP_MANEUVER_TYPE_DEPART, R.drawable.ic_maneuver_depart);

      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        R.drawable.ic_maneuver_turn_75);
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_turn_45);
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_turn_30);

      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
        R.drawable.ic_maneuver_turn_75_left);
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_45_left);
      put(STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_turn_30_left);

      put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_merge_left);
      put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_merge_left);
      put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_merge_right);
      put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_merge_right);
      put(STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_turn_0);

      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
        R.drawable.ic_maneuver_turn_75_left);
      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_45_left);
      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_turn_30_left);

      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        R.drawable.ic_maneuver_turn_75);
      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_turn_45);
      put(STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_turn_30);

      put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_off_ramp_left);
      put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_off_ramp_slight_left);

      put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_off_ramp_right);
      put(STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_off_ramp_slight_right);

      put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_fork_left);
      put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_fork_slight_left);
      put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_fork_right);
      put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_fork_slight_right);
      put(STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_fork_straight);
      put(STEP_MANEUVER_TYPE_FORK, R.drawable.ic_maneuver_fork);

      put(STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_end_of_road_left);
      put(STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_end_of_road_right);

      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_left);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_left);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_left);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_roundabout_right);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        R.drawable.ic_maneuver_roundabout_sharp_right);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_roundabout_slight_right);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_roundabout_straight);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT, R.drawable.ic_maneuver_roundabout);

      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_straight_left_driving_side);

      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_left);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_left);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_left);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_roundabout_right);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        R.drawable.ic_maneuver_roundabout_sharp_right);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_roundabout_slight_right);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_roundabout_straight);
      put(STEP_MANEUVER_TYPE_ROTARY, R.drawable.ic_maneuver_roundabout);

      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side);
      put(STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_roundabout_straight_left_driving_side);

      put(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_45_left);
      put(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_turn_45);

      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT,
        R.drawable.ic_maneuver_turn_45_left);
      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT,
        R.drawable.ic_maneuver_turn_75_left);
      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        R.drawable.ic_maneuver_turn_30_left);

      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT,
        R.drawable.ic_maneuver_turn_45);
      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        R.drawable.ic_maneuver_turn_75);
      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        R.drawable.ic_maneuver_turn_30);
      put(STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_turn_0);

      put(STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT,
        R.drawable.ic_maneuver_turn_0);
    }
  };

  static int getManeuverResource(String maneuver) {
    Integer maneuverResource = MANEUVER_MAP.get(maneuver);
    if (maneuverResource == null) {
      return R.drawable.ic_maneuver_turn_0;
    }
    return maneuverResource;
  }
}
