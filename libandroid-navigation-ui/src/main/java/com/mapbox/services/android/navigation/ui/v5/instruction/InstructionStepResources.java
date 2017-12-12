package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;

import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.text.DecimalFormat;
import java.util.List;

class InstructionStepResources {

  private SpannableStringBuilder stepDistanceRemaining;
  private String maneuverViewModifier;
  private String maneuverViewType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private boolean shouldShowThenStep;
  private List<IntersectionLanes> turnLanes;

  InstructionStepResources(RouteProgress progress, DecimalFormat decimalFormat, int unitType) {
    formatStepDistance(progress, decimalFormat, unitType);
    extractStepResources(progress);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getManeuverViewModifier() {
    return maneuverViewModifier;
  }

  String getManeuverViewType() {
    return maneuverViewType;
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

  private void extractStepResources(RouteProgress progress) {
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LegStep upcomingStep = progress.currentLegProgress().upComingStep();
    LegStep thenStep = progress.currentLegProgress().followOnStep();

    // Type / Modifier / Text
    if (upcomingStep != null) {
      maneuverViewType = upcomingStep.maneuver().type();
      maneuverViewModifier = upcomingStep.maneuver().modifier();
    } else {
      maneuverViewType = currentStep.maneuver().type();
      maneuverViewModifier = currentStep.maneuver().modifier();
    }

    // Then step (step after upcoming)
    if (thenStep != null) {
      thenStep(upcomingStep, thenStep);
    }

    // Turn lane data
    if (upcomingStep != null && hasIntersections(upcomingStep)) {
      intersectionTurnLanes(upcomingStep);
    }
  }

  private void formatStepDistance(RouteProgress progress, DecimalFormat decimalFormat, int unitType) {
    stepDistanceRemaining = DistanceUtils.distanceFormatter(progress.currentLegProgress()
      .currentStepProgress().distanceRemaining(), decimalFormat, true, unitType);
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

  private void thenStep(LegStep upcomingStep, LegStep thenStep) {
    thenStepManeuverType = thenStep.maneuver().type();
    thenStepManeuverModifier = thenStep.maneuver().modifier();
    // Should show then step if the upcoming step is less than 25 seconds
    shouldShowThenStep = upcomingStep.duration() <= (25d * 1.2d);
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
