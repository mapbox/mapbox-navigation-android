package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.HashMap;

import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT_ONLY;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT_ONLY;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_UTURN;

class TurnLaneDrawableMap extends HashMap<String, Integer> {

  TurnLaneDrawableMap() {
    put(DRAW_LANE_STRAIGHT, R.drawable.ic_lane_straight);
    put(DRAW_LANE_UTURN, R.drawable.ic_lane_uturn);
    put(DRAW_LANE_RIGHT, R.drawable.ic_lane_right);
    put(DRAW_LANE_SLIGHT_RIGHT, R.drawable.ic_lane_slight_right);
    put(DRAW_LANE_RIGHT_ONLY, R.drawable.ic_lane_right_only);
    put(DRAW_LANE_STRAIGHT_ONLY, R.drawable.ic_lane_straight_only);
  }
}
