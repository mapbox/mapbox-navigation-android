package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.text.SpannableString;

import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.util.List;
import java.util.Locale;

class InstructionStepResources {

  private static final double VALID_UPCOMING_DURATION = 25d * 1.2d;
  private static final double VALID_CURRENT_DURATION = 70d;

  private SpannableString stepDistanceRemaining;
  private String maneuverViewModifier;
  private String maneuverViewType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private List<IntersectionLanes> turnLanes;
  private boolean shouldShowThenStep;

  InstructionStepResources(Context context, RouteProgress progress, Locale locale,
                           @NavigationUnitType.UnitType int unitType) {
    formatStepDistance(context, progress, locale, unitType);
    extractStepResources(progress);
  }

  SpannableString getStepDistanceRemaining() {
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
    LegStep followOnStep = progress.currentLegProgress().followOnStep();

    // Type / Modifier / Text
    if (upcomingStep != null) {
      maneuverViewType = upcomingStep.maneuver().type();
      maneuverViewModifier = upcomingStep.maneuver().modifier();

      // Then step (step after upcoming)
      if (followOnStep != null) {
        thenStep(upcomingStep, followOnStep, progress.currentLegProgress().currentStepProgress().durationRemaining());
      }

      // Turn lane data
      if (hasIntersections(upcomingStep)) {
        intersectionTurnLanes(upcomingStep);
      }
    } else {
      maneuverViewType = currentStep.maneuver().type();
      maneuverViewModifier = currentStep.maneuver().modifier();
    }
  }

  private void formatStepDistance(Context context, RouteProgress progress,
                                  Locale locale, @NavigationUnitType.UnitType int unitType) {
    stepDistanceRemaining = new DistanceUtils(context, locale, unitType)
      .formatDistance(progress.currentLegProgress().currentStepProgress().distanceRemaining());
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

  private void thenStep(LegStep upcomingStep, LegStep followOnStep, double currentDurationRemaining) {
    thenStepManeuverType = followOnStep.maneuver().type();
    thenStepManeuverModifier = followOnStep.maneuver().modifier();
    shouldShowThenStep = isValidStepDuration(upcomingStep, currentDurationRemaining);
  }

  private boolean isValidStepDuration(LegStep upcomingStep, double currentDurationRemaining) {
    return upcomingStep.duration() <= VALID_UPCOMING_DURATION
      && currentDurationRemaining <= VALID_CURRENT_DURATION;
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
