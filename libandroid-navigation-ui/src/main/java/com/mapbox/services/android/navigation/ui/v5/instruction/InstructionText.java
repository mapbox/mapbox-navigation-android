package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class InstructionText {

  private String primaryText;
  private String secondaryText;
  private double stepDistance;
  private double distanceAlongStep;

  public InstructionText(LegStep step, double distanceAlongStep) {
    primaryText = "";
    secondaryText = "";
    stepDistance = step.distance();
    if (distanceAlongStep > 0d) {
      this.distanceAlongStep = distanceAlongStep;
    }
    buildInstructionText(step);
  }

  public String getPrimaryText() {
    return StringAbbreviator.abbreviate(primaryText);
  }

  public String getSecondaryText() {
    return StringAbbreviator.abbreviate(secondaryText);
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
