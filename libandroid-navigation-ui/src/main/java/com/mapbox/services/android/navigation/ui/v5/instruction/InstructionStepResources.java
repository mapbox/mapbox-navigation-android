package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

import java.util.List;

class InstructionStepResources {

  private static final double VALID_UPCOMING_DURATION = 25d * 1.2d;
  private static final double VALID_CURRENT_DURATION = 70d;
  private static final int FIRST_INSTRUCTION = 0;

  private SpannableString stepDistanceRemaining;
  private String maneuverViewModifier;
  private String maneuverViewType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private Float thenStepRoundaboutDegrees;
  private List<IntersectionLanes> turnLanes;
  private boolean shouldShowThenStep;

  InstructionStepResources(DistanceFormatter distanceFormatter, RouteProgress progress) {
    double distanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
    stepDistanceRemaining = distanceFormatter.formatDistance(distanceRemaining);
    extractStepResources(progress.currentLegProgress());
  }

  InstructionStepResources(DistanceFormatter distanceFormatter, RouteLeg leg, int stepIndex) {
    LegStep currentStep = PreviewInstructionUtils.currentStep(leg, stepIndex);
    LegStep upcomingStep = PreviewInstructionUtils.upcomingStep(leg, stepIndex);
    LegStep followOnStep = PreviewInstructionUtils.followOnStep(leg, stepIndex);

    stepDistanceRemaining = distanceFormatter.formatDistance(currentStep.distance());
    extractStepResources(currentStep, upcomingStep, followOnStep, currentStep.duration());
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

  @Nullable
  Float getThenStepRoundaboutDegrees() {
    return thenStepRoundaboutDegrees;
  }

  boolean shouldShowThenStep() {
    return shouldShowThenStep;
  }

  List<IntersectionLanes> getTurnLanes() {
    return turnLanes;
  }

  private void extractStepResources(RouteLegProgress routeLegProgress) {
    LegStep currentStep = routeLegProgress.currentStep();
    LegStep upcomingStep = routeLegProgress.upComingStep();
    LegStep followOnStep = routeLegProgress.followOnStep();
    double durationRemaining = routeLegProgress.currentStepProgress().durationRemaining();
    extractStepResources(currentStep, upcomingStep, followOnStep, durationRemaining);
  }

  public void extractStepResources(LegStep currentStep, LegStep upcomingStep, LegStep followOnStep,
                                   double durationRemaining) {
    // Type / Modifier / Text
    if (upcomingStep != null) {
      maneuverViewType = upcomingStep.maneuver().type();
      maneuverViewModifier = upcomingStep.maneuver().modifier();

      // Then step (step after upcoming)
      if (followOnStep != null) {
        thenStep(upcomingStep, followOnStep, durationRemaining);
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
    List<BannerInstructions> bannerInstructions = followOnStep.bannerInstructions();
    if (bannerInstructions == null || bannerInstructions.isEmpty()) {
      return;
    }
    BannerText primaryText = bannerInstructions.get(FIRST_INSTRUCTION).primary();
    thenStepManeuverType = primaryText.type();
    thenStepManeuverModifier = primaryText.modifier();
    if (primaryText.degrees() != null) {
      thenStepRoundaboutDegrees = primaryText.degrees().floatValue();
    }
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
