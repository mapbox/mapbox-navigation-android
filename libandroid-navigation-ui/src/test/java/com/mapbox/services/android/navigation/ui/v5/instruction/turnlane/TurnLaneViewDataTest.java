package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import org.junit.Test;

import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TurnLaneViewDataTest {

  @Test
  public void shouldBeFlipped_trueForUturn() {
    String laneIndications = "uturn";
    String maneuverModifier = "modifier";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    boolean shouldFlip = data.shouldBeFlipped();

    assertTrue(shouldFlip);
  }

  @Test
  public void getDrawMethod_correctForRightLane() {
    String laneIndications = "right";
    String maneuverModifier = "modifier";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_RIGHT, drawMethod);
  }

  @Test
  public void getDrawMethod_correctForStraightRightLane_modifierRight() {
    String laneIndications = "straightright";
    String maneuverModifier = "right";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_RIGHT_ONLY, drawMethod);
  }
}