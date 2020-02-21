package com.mapbox.navigation.ui.instruction;

import android.text.SpannableString;

import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;

public class InstructionModel {

  private RouteProgress progress;
  private SpannableString stepDistanceRemaining;
  private String drivingSide;

  public InstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress) {
    this.progress = progress;
    double distanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
    stepDistanceRemaining = distanceFormatter.formatDistance(distanceRemaining);
    this.drivingSide = progress.currentLegProgress().currentStepProgress().step().drivingSide();
  }

  RouteProgress retrieveProgress() {
    return progress;
  }

  SpannableString retrieveStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String retrieveDrivingSide() {
    return drivingSide;
  }
}
