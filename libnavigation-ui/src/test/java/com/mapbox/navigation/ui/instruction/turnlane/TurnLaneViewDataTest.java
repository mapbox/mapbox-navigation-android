package com.mapbox.navigation.ui.instruction.turnlane;

import com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData;

import org.junit.Test;

import static com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneViewData.*;
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

  @Test
  public void getDrawMethod_correctForStraightRightLane_modifierStraight() {
    String laneIndications = "straightright";
    String maneuverModifier = "straight";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_STRAIGHT_ONLY, drawMethod);
  }

  @Test
  public void getDrawMethod_correctForStraightRightLane_modifierLeft() {
    String laneIndications = "straightright";
    String maneuverModifier = "left";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_STRAIGHT_RIGHT_NONE, drawMethod);
  }

  @Test
  public void getDrawMethod_correctForStraightLeftLane_modifierLeft() {
    String laneIndications = "straightleft";
    String maneuverModifier = "left";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();
    boolean shouldFlip = data.shouldBeFlipped();

    assertEquals(DRAW_LANE_RIGHT_ONLY, drawMethod);
    assertTrue(shouldFlip);
  }

  @Test
  public void getDrawMethod_correctForStraightLeftLane_modifierStraight() {
    String laneIndications = "straightleft";
    String maneuverModifier = "straight";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_STRAIGHT_ONLY, drawMethod);
  }

  @Test
  public void getDrawMethod_correctForStraightLeftLane_modifierRight() {
    String laneIndications = "straightleft";
    String maneuverModifier = "right";
    TurnLaneViewData data = new TurnLaneViewData(laneIndications, maneuverModifier);

    String drawMethod = data.getDrawMethod();

    assertEquals(DRAW_LANE_STRAIGHT_RIGHT_NONE, drawMethod);
  }
}