package com.mapbox.services.android.navigation.ui.v5.summary;

import android.text.SpannableStringBuilder;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;

import static com.mapbox.services.android.navigation.v5.utils.DistanceUtils.distanceFormatterBold;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatArrivalTime;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatTimeRemaining;

public class SummaryModel {

  private RouteProgress progress;
  private SpannableStringBuilder distanceRemaining;
  private String timeRemaining;
  private String arrivalTime;
  private float stepFractionTraveled;

  public SummaryModel(RouteProgress progress, DecimalFormat decimalFormat) {
    this.progress = progress;
    distanceRemaining = distanceFormatterBold(progress.distanceRemaining(), decimalFormat);
    timeRemaining = formatTimeRemaining(progress.durationRemaining());
    arrivalTime = formatArrivalTime(progress.durationRemaining());
    stepFractionTraveled = progress.currentLegProgress().currentStepProgress().fractionTraveled();
  }

  RouteProgress getProgress() {
    return progress;
  }

  SpannableStringBuilder getDistanceRemaining() {
    return distanceRemaining;
  }

  String getTimeRemaining() {
    return timeRemaining;
  }

  String getArrivalTime() {
    return arrivalTime;
  }

  float getStepFractionTraveled() {
    return stepFractionTraveled;
  }
}
