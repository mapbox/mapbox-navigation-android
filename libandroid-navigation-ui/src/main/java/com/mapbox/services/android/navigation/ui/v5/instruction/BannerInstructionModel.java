package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Locale;

public class BannerInstructionModel extends InstructionModel {

  private BannerInstructionMilestone milestone;

  public BannerInstructionModel(Context context, BannerInstructionMilestone milestone,
                                RouteProgress progress, Locale locale, @NavigationUnitType.UnitType int unitType) {
    super(context, progress, locale, unitType);
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
