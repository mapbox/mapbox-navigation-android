package com.mapbox.navigation.ui.internal.instruction;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteStepProgress;

public class InstructionModel {

  @Nullable
  private RouteProgress progress;
  private SpannableString stepDistanceRemaining;
  @Nullable
  private String drivingSide;

  public InstructionModel(@NonNull DistanceFormatter distanceFormatter, @Nullable RouteProgress progress) {
    if (progress != null) {
      this.progress = progress;
      RouteLegProgress legProgress = progress.getCurrentLegProgress();
      if (legProgress != null) {
        RouteStepProgress stepProgress = legProgress.getCurrentStepProgress();
        if (stepProgress != null) {
          double distanceRemaining = stepProgress.getDistanceRemaining();
          stepDistanceRemaining = distanceFormatter.formatDistance(distanceRemaining);
          LegStep step = stepProgress.getStep();
          if (step != null) {
            this.drivingSide = step.drivingSide();
          }
        }
      }
    }
  }

  @Nullable
  public RouteProgress retrieveProgress() {
    return progress;
  }

  public SpannableString retrieveStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  @Nullable
  public String retrieveDrivingSide() {
    return drivingSide;
  }
}
