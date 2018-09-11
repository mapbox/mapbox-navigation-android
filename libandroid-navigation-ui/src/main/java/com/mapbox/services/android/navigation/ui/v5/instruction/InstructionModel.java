package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

public class InstructionModel {

  BannerText primaryBannerText;
  BannerText secondaryBannerText;
  private BannerText thenBannerText;
  private Float roundaboutAngle = null;
  private InstructionStepResources stepResources;
  private RouteProgress progress;
  private RouteUtils routeUtils;

  public InstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress) {
    this.progress = progress;
    routeUtils = new RouteUtils();
    buildInstructionModel(distanceFormatter, progress);
  }

  BannerText getPrimaryBannerText() {
    return primaryBannerText;
  }

  BannerText getSecondaryBannerText() {
    return secondaryBannerText;
  }

  BannerText getThenBannerText() {
    return thenBannerText;
  }

  @Nullable
  Float getRoundaboutAngle() {
    return roundaboutAngle;
  }

  InstructionStepResources getStepResources() {
    return stepResources;
  }

  String getManeuverType() {
    return stepResources.getManeuverViewType();
  }

  String getManeuverModifier() {
    return stepResources.getManeuverViewModifier();
  }

  RouteProgress getProgress() {
    return progress;
  }

  private void buildInstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress) {
    stepResources = new InstructionStepResources(distanceFormatter, progress);
    extractStepInstructions(progress);
  }

  private void extractStepInstructions(RouteProgress progress) {
    RouteLegProgress legProgress = progress.currentLegProgress();
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LegStep upComingStep = legProgress.upComingStep();
    int stepDistanceRemaining = (int) legProgress.currentStepProgress().distanceRemaining();

    primaryBannerText = routeUtils.findCurrentBannerText(currentStep, stepDistanceRemaining, true);
    secondaryBannerText = routeUtils.findCurrentBannerText(currentStep, stepDistanceRemaining, false);

    if (upComingStep != null) {
      thenBannerText = routeUtils.findCurrentBannerText(upComingStep, upComingStep.distance(), true);
    }

    if (primaryBannerText != null && primaryBannerText.degrees() != null) {
      roundaboutAngle = primaryBannerText.degrees().floatValue();
    }
  }
}
