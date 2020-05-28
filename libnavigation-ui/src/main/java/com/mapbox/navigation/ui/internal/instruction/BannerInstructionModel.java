package com.mapbox.navigation.ui.internal.instruction;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;

public class BannerInstructionModel extends InstructionModel {

  private final BannerText primaryBannerText;
  private final BannerText secondaryBannerText;
  private final BannerText subBannerText;

  public BannerInstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress,
      BannerInstructions instructions) {
    super(distanceFormatter, progress);
    primaryBannerText = instructions.primary();
    secondaryBannerText = instructions.secondary();
    subBannerText = instructions.sub();
  }

  public BannerText retrievePrimaryBannerText() {
    return primaryBannerText;
  }

  public BannerText retrieveSecondaryBannerText() {
    return secondaryBannerText;
  }

  public BannerText retrieveSubBannerText() {
    return subBannerText;
  }

  public String retrievePrimaryManeuverType() {
    return primaryBannerText.type();
  }

  public String retrievePrimaryManeuverModifier() {
    return primaryBannerText.modifier();
  }

  @Nullable
  public Double retrievePrimaryRoundaboutAngle() {
    return primaryBannerText.degrees();
  }
}
