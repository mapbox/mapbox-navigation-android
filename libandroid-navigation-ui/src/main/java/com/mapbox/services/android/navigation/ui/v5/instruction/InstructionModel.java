package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;

import java.text.DecimalFormat;
import java.util.List;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private SpannableStringBuilder primaryTextInstruction;
  private SpannableStringBuilder secondaryTextInstruction;
  private InstructionText thenInstructionText;
  private String maneuverViewModifier;
  private String maneuverViewType;
  private String thenStepManeuverModifier;
  private String thenStepManeuverType;
  private List<IntersectionLanes> turnLanes;
  private RouteProgress progress;
  private int unitType;
  private boolean shouldShowThenStep;

  public InstructionModel(RouteProgress progress, BannerInstructionMilestone milestone,
                           DecimalFormat decimalFormat, @NavigationUnitType.UnitType int unitType) {
    this.progress = progress;
    this.unitType = unitType;
    buildInstructionModel(progress, milestone, decimalFormat, unitType);
  }

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat,
                          @NavigationUnitType.UnitType int unitType) {
    this.progress = progress;
    this.unitType = unitType;
    buildInstructionModel(progress, decimalFormat, unitType);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  SpannableStringBuilder getPrimaryTextInstruction() {
    return primaryTextInstruction;
  }

  SpannableStringBuilder getSecondaryTextInstruction() {
    return secondaryTextInstruction;
  }

  String getThenStepText() {
    if (thenInstructionText != null) {
      return thenInstructionText.getPrimaryText();
    } else {
      return "";
    }
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

  RouteProgress getProgress() {
    return progress;
  }

  int getUnitType() {
    return unitType;
  }

  private void buildInstructionModel(RouteProgress progress, BannerInstructionMilestone milestone,
                                     DecimalFormat decimalFormat, int unitType) {
    formatStepDistance(progress, decimalFormat, unitType);
    extractStepResources(progress);
    extractMilestoneInstructions(milestone);
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat, int unitType) {
    formatStepDistance(progress, decimalFormat, unitType);
    extractStepResources(progress);
    extractStepInstructions(progress);
  }

  private void extractMilestoneInstructions(BannerInstructionMilestone milestone) {
    primaryTextInstruction = milestone.getPrimaryInstruction();
    secondaryTextInstruction = milestone.getSecondaryInstruction();
  }

  private void extractStepInstructions(RouteProgress progress) {
    primaryTextInstruction = retrievePrimaryInstructionText(progress);
    secondaryTextInstruction = retrieveSecondaryInstructionText(progress);
  }

  @NonNull
  private SpannableStringBuilder retrievePrimaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    if (bannerInstructions != null
      && !bannerInstructions.isEmpty()
      && bannerInstructions.get(0).primary() != null) {
      return SpanUtils.buildInstructionSpanItems(bannerInstructions.get(0).primary());
    } else {
      return new SpannableStringBuilder("");
    }
  }

  @NonNull
  private SpannableStringBuilder retrieveSecondaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    if (bannerInstructions != null
      && !bannerInstructions.isEmpty()
      && bannerInstructions.get(0).secondary() != null) {
      return SpanUtils.buildInstructionSpanItems(bannerInstructions.get(0).secondary());
    } else {
      return new SpannableStringBuilder("");
    }
  }

  private void extractStepResources(RouteProgress progress) {
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LegStep upcomingStep = progress.currentLegProgress().upComingStep();
    LegStep thenStep = progress.currentLegProgress().followOnStep();

    // Type / Modifier / Text
    if (upcomingStep != null && hasManeuver(upcomingStep)) {
      maneuverViewType = upcomingStep.maneuver().type();
      maneuverViewModifier = upcomingStep.maneuver().modifier();
    } else if (hasManeuver(currentStep)) {
      maneuverViewType = currentStep.maneuver().type();
      maneuverViewModifier = currentStep.maneuver().modifier();
    }

    // Then step (step after upcoming)
    if (thenStep != null && hasManeuver(thenStep)) {
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
