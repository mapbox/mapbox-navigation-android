package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
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
  private DistanceFormatter distanceFormatter;
  private String language;
  @DirectionsCriteria.VoiceUnitCriteria
  private String unitType;

  InstructionStepResources(Context context, RouteProgress progress, String language, String unitType) {
    formatStepDistance(context, progress, language, unitType);
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
                                  String language, @DirectionsCriteria.VoiceUnitCriteria String unitType) {
    if (shouldDistanceUtilsBeInitialized(language, unitType)) {
      distanceFormatter = new DistanceFormatter(context, language, unitType);
      this.language = language;
      this.unitType = unitType;
    }
    stepDistanceRemaining = distanceFormatter.formatDistance(
      progress.currentLegProgress().currentStepProgress().distanceRemaining());
  }

  private boolean shouldDistanceUtilsBeInitialized(String language,
                                                   @DirectionsCriteria.VoiceUnitCriteria String unitType) {
    return distanceFormatter == null ||  !this.language.equals(language) || !this.unitType.equals(unitType);
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
