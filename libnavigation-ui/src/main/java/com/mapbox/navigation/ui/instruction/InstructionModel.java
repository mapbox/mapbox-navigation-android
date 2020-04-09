package com.mapbox.navigation.ui.instruction;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteStepProgress;

public class InstructionModel {

  private RouteProgress progress;
  private SpannableString stepDistanceRemaining;
  private String drivingSide;

  public InstructionModel(@NonNull DistanceFormatter distanceFormatter, @Nullable RouteProgress progress) {
    if (progress != null) {
      this.progress = progress;
      RouteLegProgress legProgress = progress.currentLegProgress();
      if (legProgress != null) {
        RouteStepProgress stepProgress = legProgress.currentStepProgress();
        if (stepProgress != null) {
          double distanceRemaining = stepProgress.distanceRemaining();
          stepDistanceRemaining = distanceFormatter.formatDistance(distanceRemaining);
          LegStep step = stepProgress.step();
          if (step != null) {
            this.drivingSide = step.drivingSide();
          }
        }
      }
    }
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
