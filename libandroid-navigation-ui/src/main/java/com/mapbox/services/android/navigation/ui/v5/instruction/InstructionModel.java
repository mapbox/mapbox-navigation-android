package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LegStep upComingStep = legProgress.upComingStep();
    int stepDistanceRemaining = (int) legProgress.currentStepProgress().distanceRemaining();

    primaryBannerText = findBannerText(currentStep, stepDistanceRemaining, true);
    secondaryBannerText = findBannerText(currentStep, stepDistanceRemaining, false);
    thenBannerText = findBannerText(upComingStep, stepDistanceRemaining, true);

    if (primaryBannerText != null && primaryBannerText.degrees() != null) {
      roundaboutAngle = primaryBannerText.degrees().floatValue();
    }
  }

  @Nullable
  private static BannerText findBannerText(LegStep step, final double stepDistanceRemaining, boolean findPrimary) {
    if (step != null && hasInstructions(step.bannerInstructions())) {
      List<BannerInstructions> instructions = new ArrayList<>(step.bannerInstructions());
      for (int i = 0; i < instructions.size(); i++) {
        double distanceAlongGeometry = instructions.get(i).distanceAlongGeometry();
        if (distanceAlongGeometry < stepDistanceRemaining) {
          instructions.remove(i);
        }
      }
      int instructionIndex = instructions.size() - 1;
      BannerInstructions currentInstructions = instructions.get(instructionIndex);
      return retrievePrimaryOrSecondaryBannerText(findPrimary, currentInstructions);
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
