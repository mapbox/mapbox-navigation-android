package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class BannerInstructionModel extends InstructionModel {

  public BannerInstructionModel(Context context, BannerInstructions instructions, RouteProgress progress,
                                String language, @DirectionsCriteria.VoiceUnitCriteria String unitType) {
    super(context, progress, language, unitType);
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
