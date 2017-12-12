package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class InstructionModel {

  private BannerText primaryBannerText;
  private BannerText secondaryBannerText;
  private BannerText thenBannerText;
  private InstructionStepResources stepResources;
  private RouteProgress progress;
  private int unitType;

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

  BannerText getPrimaryBannerText() {
    return primaryBannerText;
  }

  BannerText getSecondaryBannerText() {
    return secondaryBannerText;
  }

  BannerText getThenBannerText() {
    return thenBannerText;
  }

  InstructionStepResources getStepResources() {
    return stepResources;
  }

  RouteProgress getProgress() {
    return progress;
  }

  int getUnitType() {
    return unitType;
  }

  private void buildInstructionModel(RouteProgress progress, BannerInstructionMilestone milestone,
                                     DecimalFormat decimalFormat, int unitType) {
    stepResources = new InstructionStepResources(progress, decimalFormat, unitType);
    extractMilestoneInstructions(milestone);
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat, int unitType) {
    stepResources = new InstructionStepResources(progress, decimalFormat, unitType);
    extractStepInstructions(progress);
  }

  private void extractMilestoneInstructions(BannerInstructionMilestone milestone) {
    primaryBannerText = milestone.getPrimaryInstruction();
    secondaryBannerText = milestone.getSecondaryInstruction();
    thenBannerText = retrieveThenInstructionText(progress);
  }

  private void extractStepInstructions(RouteProgress progress) {
    primaryBannerText = retrievePrimaryInstructionText(progress);
    secondaryBannerText = retrieveSecondaryInstructionText(progress);
    thenBannerText = retrieveThenInstructionText(progress);
  }

  private BannerText retrievePrimaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    double stepDistanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
    return InstructionLoader.findInstructionBannerText(stepDistanceRemaining,
      bannerInstructions, InstructionLoader.BANNER_TEXT_TYPE_PRIMARY);
  }

  private BannerText retrieveSecondaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    double stepDistanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
    return InstructionLoader.findInstructionBannerText(stepDistanceRemaining,
      bannerInstructions, InstructionLoader.BANNER_TEXT_TYPE_SECONDARY);
  }

  private BannerText retrieveThenInstructionText(RouteProgress progress) {
    LegStep followOnStep = progress.currentLegProgress().followOnStep();
    List<BannerInstructions> bannerInstructions = new ArrayList<>();
    if (followOnStep != null) {
      bannerInstructions = followOnStep.bannerInstructions();
    }
    return InstructionLoader.findInstructionBannerText(0,
      bannerInstructions, InstructionLoader.BANNER_TEXT_TYPE_PRIMARY);
  }
}
