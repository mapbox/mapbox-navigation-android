package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class BannerInstructionModel extends InstructionModel {

  public BannerInstructionModel(Context context, BannerInstructionMilestone milestone, RouteProgress progress,
                                String language, @DirectionsCriteria.VoiceUnitCriteria String unitType) {
    super(context, progress, language, unitType);
    primaryBannerText = milestone.getBannerInstructions().primary();
    secondaryBannerText = milestone.getBannerInstructions().secondary();
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
