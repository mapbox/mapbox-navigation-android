package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;

public class BannerInstructionModel extends InstructionModel {

  private BannerInstructionMilestone milestone;

  public BannerInstructionModel(BannerInstructionMilestone milestone, RouteProgress progress,
                                DecimalFormat decimalFormat, int unitType) {
    super(progress, decimalFormat, unitType);
    this.milestone = milestone;
  }

  @Override
  BannerText getPrimaryBannerText() {
    return milestone.getBannerInstructions().primary();
  }

  @Override
  BannerText getSecondaryBannerText() {
    return milestone.getBannerInstructions().secondary();
  }
}
