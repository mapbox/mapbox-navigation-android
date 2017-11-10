package com.mapbox.services.android.navigation.ui.v5.summary;

import android.text.SpannableStringBuilder;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;

import static com.mapbox.services.android.navigation.v5.utils.DistanceUtils.distanceFormatterBold;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatArrivalTime;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatTimeRemaining;

public class SummaryModel {

  private SpannableStringBuilder distanceRemaining;
  private SpannableStringBuilder timeRemaining;
  private String arrivalTime;

  public SummaryModel(RouteProgress progress, DecimalFormat decimalFormat) {
    distanceRemaining = distanceFormatterBold(progress.distanceRemaining(), decimalFormat, false);
    timeRemaining = formatTimeRemaining(progress.durationRemaining());
    arrivalTime = formatArrivalTime(progress.durationRemaining());
  }

  SpannableStringBuilder getDistanceRemaining() {
    return distanceRemaining;
  }

  SpannableStringBuilder getTimeRemaining() {
    return timeRemaining;
  }

  String getArrivalTime() {
    return arrivalTime;
  }
}
