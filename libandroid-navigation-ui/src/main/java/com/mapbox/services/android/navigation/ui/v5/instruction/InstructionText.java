package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class InstructionText {

  private String primaryText;
  private String secondaryText;
  private double stepDistance;
  private double distanceAlongStep;

  public InstructionText(LegStep step, @Nullable Double distanceAlongStep) {
    primaryText = "";
    secondaryText = "";
    stepDistance = step.distance();
    if (distanceAlongStep != null) {
      this.distanceAlongStep = distanceAlongStep;
    }
    buildInstructionText(step);
  }

  public String getPrimaryText() {
    return primaryText;
  }

  public String getSecondaryText() {
    return secondaryText;
  }

  public double getStepDistance() {
    return stepDistance;
  }

  private void buildInstructionText(LegStep step) {
    if (hasBannerInstructions(step)) {
      if (isArrivalManeuverType(step) && shouldUseArrivalInstruction(step)) {
        // Arrival instructions
        BannerInstructions instructions = step.bannerInstructions().get(1);
        createInstructionText(instructions);
      } else {
        // Normal instructions
        BannerInstructions instructions = step.bannerInstructions().get(0);
        createInstructionText(instructions);
      }
    }
  }

  private boolean hasBannerInstructions(LegStep step) {
    return step.bannerInstructions() != null && !step.bannerInstructions().isEmpty();
  }

  private boolean isArrivalManeuverType(LegStep step) {
    return step.maneuver() != null
      && !TextUtils.isEmpty(step.maneuver().type())
      && step.maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE);
  }

  private boolean shouldUseArrivalInstruction(LegStep step) {
    return step.bannerInstructions().size() > 1
      && distanceAlongStep >= step.bannerInstructions().get(1).distanceAlongGeometry();
  }

  private void createInstructionText(BannerInstructions instructions) {
    if (instructions.primary() != null
      && !TextUtils.isEmpty(instructions.primary().text())) {
      primaryText = instructions.primary().text();
    }
    if (instructions.secondary() != null
      && !TextUtils.isEmpty(instructions.secondary().text())) {
      secondaryText = instructions.secondary().text();
    }
  }
}
