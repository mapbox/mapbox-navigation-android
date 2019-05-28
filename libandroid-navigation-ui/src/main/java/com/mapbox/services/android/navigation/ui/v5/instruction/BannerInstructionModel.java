package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

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

  BannerText retrievePrimaryBannerText() {
    return primaryBannerText;
  }

  BannerText retrieveSecondaryBannerText() {
    return secondaryBannerText;
  }

  BannerText retrieveSubBannerText() {
    return subBannerText;
  }

  String retrievePrimaryManeuverType() {
    return primaryBannerText.type();
  }

  String retrievePrimaryManeuverModifier() {
    return primaryBannerText.modifier();
  }

  @Nullable
  Double retrievePrimaryRoundaboutAngle() {
    return primaryBannerText.degrees();
  }
}
