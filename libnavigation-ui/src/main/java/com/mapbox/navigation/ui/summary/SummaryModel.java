package com.mapbox.navigation.ui.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import com.mapbox.services.android.navigation.v5.navigation.TimeFormatType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter;

import java.util.Calendar;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(Context context, DistanceFormatter distanceFormatter, RouteProgress progress,
                      @TimeFormatType int timeFormatType) {
    distanceRemaining = distanceFormatter.formatDistance(progress.distanceRemaining()).toString();
    double legDurationRemaining = progress.currentLegProgress().durationRemaining();
    timeRemaining = TimeFormatter.formatTimeRemaining(context, legDurationRemaining);
    Calendar time = Calendar.getInstance();
    boolean isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
    arrivalTime = TimeFormatter.formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat);
  }

  String getDistanceRemaining() {
    return distanceRemaining;
  }

  SpannableStringBuilder getTimeRemaining() {
    return timeRemaining;
  }

  String getArrivalTime() {
    return arrivalTime;
  }
}
