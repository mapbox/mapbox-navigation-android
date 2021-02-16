package com.mapbox.navigation.ui.internal.instruction.turnlane;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.ManeuverModifier;

public class TurnLaneViewData {

  public static final String DRAW_LANE_SLIGHT_RIGHT = "draw_lane_slight_right";
  public static final String DRAW_LANE_RIGHT = "draw_lane_right";
  public static final String DRAW_LANE_STRAIGHT = "draw_lane_straight";
  public static final String DRAW_LANE_UTURN = "draw_lane_uturn";
  public static final String DRAW_LANE_RIGHT_ONLY = "draw_lane_right_only";
  public static final String DRAW_LANE_STRAIGHT_ONLY = "draw_lane_straight_only";
  public static final String DRAW_LANE_STRAIGHT_RIGHT_NONE = "draw_lane_straight_right_none";

  private boolean shouldFlip;
  private String drawMethod;

  public TurnLaneViewData(@NonNull String laneIndications, @NonNull String primaryManeuver) {
    buildDrawData(laneIndications, primaryManeuver);
  }

  public boolean shouldBeFlipped() {
    return shouldFlip;
  }

  public String getDrawMethod() {
    return drawMethod;
  }

  private void buildDrawData(@NonNull String laneIndications, @NonNull String primaryManeuver) {

    // U-turn
    if (laneIndications.contentEquals(ManeuverModifier.UTURN)) {
      drawMethod = DRAW_LANE_UTURN;
      shouldFlip = true;
      return;
    }

    // Straight
    if (laneIndications.contentEquals(ManeuverModifier.STRAIGHT)) {
      drawMethod = DRAW_LANE_STRAIGHT;
      return;
    }

    // Right or left
    if (laneIndications.contentEquals(ManeuverModifier.RIGHT)) {
      drawMethod = DRAW_LANE_RIGHT;
      return;
    } else if (laneIndications.contentEquals(ManeuverModifier.LEFT)) {
      drawMethod = DRAW_LANE_RIGHT;
      shouldFlip = true;
      return;
    }

    // Slight right or slight left
    if (laneIndications.contentEquals(ManeuverModifier.SLIGHT_RIGHT)) {
      drawMethod = DRAW_LANE_SLIGHT_RIGHT;
      return;
    } else if (laneIndications.contentEquals(ManeuverModifier.SLIGHT_LEFT)) {
      drawMethod = DRAW_LANE_SLIGHT_RIGHT;
      shouldFlip = true;
      return;
    }

    // Straight and left
    if (laneIndications.contains(ManeuverModifier.STRAIGHT)
      && (laneIndications.contains(ManeuverModifier.LEFT)
            || laneIndications.contains(ManeuverModifier.SLIGHT_LEFT)
            || laneIndications.contains(ManeuverModifier.SHARP_LEFT)
        )
    ) {
      if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.STRAIGHT)) {
        drawMethod = DRAW_LANE_STRAIGHT_ONLY;
      } else if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.LEFT)
          || primaryManeuver.contains(ManeuverModifier.SLIGHT_LEFT)
          || primaryManeuver.contains(ManeuverModifier.SHARP_LEFT)
      ) {
        drawMethod = DRAW_LANE_RIGHT_ONLY;
      } else if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.RIGHT)
          || primaryManeuver.contains(ManeuverModifier.SLIGHT_RIGHT)
          || primaryManeuver.contains(ManeuverModifier.SHARP_RIGHT)
      ) {
        drawMethod = DRAW_LANE_STRAIGHT_RIGHT_NONE;
      }
      shouldFlip = true;
      return;
    }

    // Straight and right
    if (laneIndications.contains(ManeuverModifier.STRAIGHT)
      && (laneIndications.contains(ManeuverModifier.RIGHT)
            || laneIndications.contains(ManeuverModifier.SLIGHT_RIGHT)
            || laneIndications.contains(ManeuverModifier.SHARP_RIGHT)
        )
    ) {
      if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.STRAIGHT)) {
        drawMethod = DRAW_LANE_STRAIGHT_ONLY;
      } else if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.RIGHT)
          || primaryManeuver.contains(ManeuverModifier.SLIGHT_RIGHT)
          || primaryManeuver.contains(ManeuverModifier.SHARP_RIGHT)
      ) {
        drawMethod = DRAW_LANE_RIGHT_ONLY;
      } else if (primaryManeuver.equalsIgnoreCase(ManeuverModifier.LEFT)
          || primaryManeuver.contains(ManeuverModifier.SLIGHT_LEFT)
          || primaryManeuver.contains(ManeuverModifier.SHARP_LEFT)
      ) {
        drawMethod = DRAW_LANE_STRAIGHT_RIGHT_NONE;
      }
    }
  }
}
