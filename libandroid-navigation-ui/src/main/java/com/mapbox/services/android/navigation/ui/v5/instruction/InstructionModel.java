package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.utils.RouteUtils.findCurrentBannerText;

public class InstructionModel {

  private BannerText primaryBannerText;
  private BannerText secondaryBannerText;
  private BannerText thenBannerText;
  private Float roundaboutAngle = null;
  private InstructionStepResources stepResources;
  private RouteProgress progress;
  private Locale locale;
  @NavigationUnitType.UnitType
  private int unitType;

  public InstructionModel(Context context, RouteProgress progress,
                          Locale locale, @NavigationUnitType.UnitType int unitType) {
    this.progress = progress;
    this.locale = locale;
    this.unitType = unitType;
    buildInstructionModel(context, progress);
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

  RouteProgress getProgress() {
    return progress;
  }

  private void buildInstructionModel(Context context, RouteProgress progress) {
    stepResources = new InstructionStepResources(context, progress, locale, unitType);
    extractStepInstructions(progress);
  }

  private void extractStepInstructions(RouteProgress progress) {
    RouteLegProgress legProgress = progress.currentLegProgress();
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LegStep upComingStep = legProgress.upComingStep();
    int stepDistanceRemaining = (int) legProgress.currentStepProgress().distanceRemaining();

    primaryBannerText = findCurrentBannerText(currentStep, stepDistanceRemaining, true);
    secondaryBannerText = findCurrentBannerText(currentStep, stepDistanceRemaining, false);

    if (upComingStep != null) {
      thenBannerText = findCurrentBannerText(upComingStep, upComingStep.distance(), true);
    }

    if (primaryBannerText != null && primaryBannerText.degrees() != null) {
      roundaboutAngle = primaryBannerText.degrees().floatValue();
    }
  }
}
