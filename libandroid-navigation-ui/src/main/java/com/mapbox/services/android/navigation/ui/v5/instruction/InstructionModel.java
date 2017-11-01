package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;

import com.mapbox.directions.v5.models.IntersectionLanes;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.text.DecimalFormat;
import java.util.List;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private InstructionText instruction;
  private String upcomingStepManeuverModifier;
  private String upcomingStepManeuverType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private List<IntersectionLanes> turnLanes;
  private RouteProgress progress;

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    this.progress = progress;
    buildInstructionModel(progress, decimalFormat);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getPrimaryText() {
    if (instruction != null) {
      return instruction.getPrimaryText();
    } else {
      return "";
    }
  }

  String getSecondaryText() {
    if (instruction != null) {
      return instruction.getSecondaryText();
    } else {
      return "";
    }
  }

  String getThenStepText() {
    if (instruction != null) {
      return instruction.getThenStepText();
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

  public String getThenStepManeuverModifier() {
    return thenStepManeuverModifier;
  }

  public String getThenStepManeuverType() {
    return thenStepManeuverType;
  }

  List<IntersectionLanes> getTurnLanes() {
    return turnLanes;
  }

  RouteProgress getProgress() {
    return progress;
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    formatStepDistance(progress, decimalFormat);
    LegStep upComingStep = progress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      extractStepResources(progress, upComingStep);
    }
  }

  private void extractStepResources(RouteProgress progress, LegStep upcomingStep) {
    if (hasManeuver(upcomingStep)) {
      upcomingStepManeuverModifier = upcomingStep.maneuver().modifier();
      upcomingStepManeuverType = upcomingStep.maneuver().type();
    }
    if (hasIntersections(upcomingStep)) {
      intersectionTurnLanes(upcomingStep);
    }

    LegStep thenStep = extractThenStep(progress, upcomingStep);
    instruction = new InstructionText(upcomingStep, thenStep);
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
    return currentLegSteps.get(thenStepIndex);
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
