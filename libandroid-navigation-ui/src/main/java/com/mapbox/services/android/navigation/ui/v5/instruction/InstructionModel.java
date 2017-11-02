package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.mapbox.directions.v5.models.IntersectionLanes;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.text.DecimalFormat;
import java.util.List;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private InstructionText upcomingInstructionText;
  private InstructionText thenInstructionText;
  private String upcomingStepManeuverModifier;
  private String upcomingStepManeuverType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private List<IntersectionLanes> turnLanes;
  private RouteProgress progress;
  private boolean shouldShowThenStep;

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    this.progress = progress;
    buildInstructionModel(progress, decimalFormat);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getPrimaryText() {
    if (upcomingInstructionText != null) {
      return upcomingInstructionText.getPrimaryText();
    } else {
      return "";
    }
  }

  String getSecondaryText() {
    if (upcomingInstructionText != null) {
      return upcomingInstructionText.getSecondaryText();
    } else {
      return "";
    }
  }

  String getThenStepText() {
    if (thenInstructionText != null) {
      return thenInstructionText.getPrimaryText();
    } else {
      return "";
    }
  }

  String getUpcomingStepManeuverModifier() {
    return upcomingStepManeuverModifier;
  }

  String getUpcomingStepManeuverType() {
    return upcomingStepManeuverType;
  }

  String getThenStepManeuverModifier() {
    return thenStepManeuverModifier;
  }

  String getThenStepManeuverType() {
    return thenStepManeuverType;
  }

  boolean shouldShowThenStep() {
    return shouldShowThenStep;
  }

  List<IntersectionLanes> getTurnLanes() {
    return turnLanes;
  }

  RouteProgress getProgress() {
    return progress;
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    formatStepDistance(progress, decimalFormat);
    LegStep upcomingStep = progress.currentLegProgress().upComingStep();
    if (upcomingStep != null) {
      extractStepResources(progress, upcomingStep);
    }
  }

  private void extractStepResources(RouteProgress progress, LegStep upcomingStep) {
    // Upcoming step
    if (hasManeuver(upcomingStep)) {
      upcomingStepManeuverType = upcomingStep.maneuver().type();
      upcomingStepManeuverModifier = upcomingStep.maneuver().modifier();
    }

    // Then step (step after upcoming)
    LegStep thenStep = extractThenStep(progress, upcomingStep);
    if (thenStep != null && hasManeuver(thenStep)) {
      thenStep(upcomingStep, thenStep);
    }

    // Turn lane data
    if (hasIntersections(upcomingStep)) {
      intersectionTurnLanes(upcomingStep);
    }

    // Upcoming instruction text data
    upcomingInstructionText = new InstructionText(upcomingStep);
  }

  private void formatStepDistance(RouteProgress progress, DecimalFormat decimalFormat) {
    stepDistanceRemaining = DistanceUtils.distanceFormatterBold(progress.currentLegProgress()
      .currentStepProgress().distanceRemaining(), decimalFormat);
  }

  private boolean hasManeuver(LegStep step) {
    return step.maneuver() != null;
  }

  private void intersectionTurnLanes(LegStep step) {
    StepIntersection intersection = step.intersections().get(0);
    List<IntersectionLanes> lanes = intersection.lanes();
    if (checkForNoneIndications(lanes)) {
      turnLanes = null;
      return;
    }
    turnLanes = lanes;
  }

  private LegStep extractThenStep(RouteProgress progress, LegStep upcomingStep) {
    List<LegStep> currentLegSteps = progress.currentLeg().steps();
    int thenStepIndex = currentLegSteps.indexOf(upcomingStep) + 1;
    if (thenStepIndex < currentLegSteps.size()) {
      return currentLegSteps.get(thenStepIndex);
    } else {
      return null;
    }
  }

  private void thenStep(LegStep upcomingStep, LegStep thenStep) {
    thenStepManeuverType = thenStep.maneuver().type();
    thenStepManeuverModifier = thenStep.maneuver().modifier();
    thenInstructionText = new InstructionText(thenStep);
    // Should show then step if the upcoming step is less than 25 seconds
    shouldShowThenStep = upcomingStep.duration() <= (25d * 1.2d)
      && !TextUtils.isEmpty(thenInstructionText.getPrimaryText());
  }

  private boolean checkForNoneIndications(List<IntersectionLanes> lanes) {
    if (lanes == null) {
      return true;
    }
    for (IntersectionLanes lane : lanes) {
      for (String indication : lane.indications()) {
        if (indication.contains("none")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasIntersections(LegStep step) {
    return step.intersections() != null
      && step.intersections().get(0) != null;
  }
}
