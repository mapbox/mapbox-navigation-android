package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;
import java.util.Locale;

public class InstructionModel {

  private BannerText primaryBannerText;
  private BannerText secondaryBannerText;
  private BannerText thenBannerText;
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
    if (progress.currentLegProgress().upComingStep() != null) {
      List<BannerInstructions> bannerInstructions = progress.currentLegProgress().upComingStep().bannerInstructions();
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
