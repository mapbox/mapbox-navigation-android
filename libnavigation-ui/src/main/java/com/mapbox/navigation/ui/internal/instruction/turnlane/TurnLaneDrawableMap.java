package com.mapbox.navigation.ui.internal.instruction.turnlane;

import com.mapbox.navigation.ui.R;

import java.util.HashMap;

import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT_ONLY;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_SLIGHT_RIGHT;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT_ONLY;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT_RIGHT_NONE;
import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.DRAW_LANE_UTURN;

public class TurnLaneDrawableMap extends HashMap<String, Integer> {

  public TurnLaneDrawableMap() {
    put(DRAW_LANE_STRAIGHT, R.drawable.mapbox_ic_lane_straight);
    put(DRAW_LANE_UTURN, R.drawable.mapbox_ic_lane_uturn);
    put(DRAW_LANE_RIGHT, R.drawable.mapbox_ic_lane_right);
    put(DRAW_LANE_SLIGHT_RIGHT, R.drawable.mapbox_ic_lane_slight_right);
    put(DRAW_LANE_RIGHT_ONLY, R.drawable.mapbox_ic_lane_right_only);
    put(DRAW_LANE_STRAIGHT_ONLY, R.drawable.mapbox_ic_lane_straight_only);
    put(DRAW_LANE_STRAIGHT_RIGHT_NONE, R.drawable.mapbox_ic_lane_straight_right_none);
  }
}
