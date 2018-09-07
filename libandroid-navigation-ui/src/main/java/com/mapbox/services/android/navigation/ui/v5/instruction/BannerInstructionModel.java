package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

public class BannerInstructionModel extends InstructionModel {

  public BannerInstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress,
                                BannerInstructions instructions) {
    super(distanceFormatter, progress);
    primaryBannerText = instructions.primary();
    secondaryBannerText = instructions.secondary();
  }

  @Override
  String getManeuverType() {
    return primaryBannerText.type();
  }

  @Override
  String getManeuverModifier() {
    return primaryBannerText.modifier();
  }
}
