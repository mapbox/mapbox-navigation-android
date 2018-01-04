package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;
import java.util.List;

public class InstructionModel {

  private BannerText primaryBannerText;
  private BannerText secondaryBannerText;
  private BannerText thenBannerText;
  private InstructionStepResources stepResources;
  private RouteProgress progress;
  private int unitType;

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

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat, int unitType) {
    stepResources = new InstructionStepResources(progress, decimalFormat, unitType);
    extractStepInstructions(progress);
  }

  private void extractStepInstructions(RouteProgress progress) {
    primaryBannerText = retrievePrimaryInstructionText(progress);
    secondaryBannerText = retrieveSecondaryInstructionText(progress);
    thenBannerText = retrieveThenInstructionText(progress);
  }

  private BannerText retrievePrimaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    if (hasInstructions(bannerInstructions)) {
      return bannerInstructions.get(0).primary();
    } else {
      return null;
    }
  }

  private BannerText retrieveSecondaryInstructionText(RouteProgress progress) {
    List<BannerInstructions> bannerInstructions = progress.currentLegProgress().currentStep().bannerInstructions();
    if (hasInstructions(bannerInstructions)) {
      return bannerInstructions.get(0).secondary();
    } else {
      return null;
    }
  }

  private BannerText retrieveThenInstructionText(RouteProgress progress) {
    if (progress.currentLegProgress().followOnStep() != null) {
      List<BannerInstructions> bannerInstructions = progress.currentLegProgress().followOnStep().bannerInstructions();
      if (hasInstructions(bannerInstructions)) {
        return bannerInstructions.get(0).primary();
      } else {
        return null;
      }
    }
    return null;
  }

  private boolean hasInstructions(List<BannerInstructions> bannerInstructions) {
    return bannerInstructions != null && !bannerInstructions.isEmpty();
  }
}
