package com.mapbox.navigation.ui.instruction.turnlane;

import com.mapbox.navigation.ui.R;

import java.util.HashMap;

class TurnLaneDrawableMap extends HashMap<String, Integer> {

  TurnLaneDrawableMap() {
    put(TurnLaneViewData.DRAW_LANE_STRAIGHT, R.drawable.ic_lane_straight);
    put(TurnLaneViewData.DRAW_LANE_UTURN, R.drawable.ic_lane_uturn);
    put(TurnLaneViewData.DRAW_LANE_RIGHT, R.drawable.ic_lane_right);
    put(TurnLaneViewData.DRAW_LANE_SLIGHT_RIGHT, R.drawable.ic_lane_slight_right);
    put(TurnLaneViewData.DRAW_LANE_RIGHT_ONLY, R.drawable.ic_lane_right_only);
    put(TurnLaneViewData.DRAW_LANE_STRAIGHT_ONLY, R.drawable.ic_lane_straight_only);
  }
}
