package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class InstructionModel {

  private static final int ONE_INSTRUCTION_INDEX = 1;

  private BannerText primaryBannerText;
  private BannerText secondaryBannerText;
  private BannerText thenBannerText;
  private Float roundaboutAngle = null;
  private InstructionStepResources stepResources;
  private RouteProgress progress;
  private Locale locale;
  private @NavigationUnitType.UnitType int unitType;

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
    LegStep currentStep = legProgress.currentStep();
    LegStep upComingStep = legProgress.upComingStep();
    double stepDistanceTraveled = legProgress.currentStepProgress().distanceRemaining();

    primaryBannerText = findBannerText(currentStep, stepDistanceTraveled, true);
    secondaryBannerText = findBannerText(currentStep, stepDistanceTraveled, false);
    thenBannerText = findBannerText(upComingStep, stepDistanceTraveled, true);

    if (primaryBannerText != null && primaryBannerText.degrees() != null) {
      roundaboutAngle = primaryBannerText.degrees().floatValue();
    }
  }

  @Nullable
  private static BannerText findBannerText(LegStep step, double stepDistanceRemaining, boolean findPrimary) {
    if (step != null && hasInstructions(step.bannerInstructions())) {
      List<BannerInstructions> instructions = step.bannerInstructions();
      Timber.d("Step instructions: %s", instructions.toString());
      Timber.d("Step distance remaining: %s", stepDistanceRemaining);
      for (BannerInstructions instruction : instructions) {
        if (instruction.distanceAlongGeometry() < stepDistanceRemaining) {
          int instructionIndex = instructions.indexOf(instruction);
          int currentInstructionIndex = instructionIndex - ONE_INSTRUCTION_INDEX;
          if (currentInstructionIndex < 0) {
            currentInstructionIndex = 0;
          }
          Timber.d("Using index %s", currentInstructionIndex);
          Timber.d("***************");
          BannerInstructions currentInstructions = instructions.get(currentInstructionIndex);
          return retrievePrimaryOrSecondaryBannerText(findPrimary, currentInstructions);
        }
      }
    }
    return null;
  }

  private static boolean hasInstructions(List<BannerInstructions> bannerInstructions) {
    return bannerInstructions != null && !bannerInstructions.isEmpty();
  }

  private static BannerText retrievePrimaryOrSecondaryBannerText(boolean findPrimary, BannerInstructions instruction) {
    return findPrimary ? instruction.primary() : instruction.secondary();
  }
}
