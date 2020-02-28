package com.mapbox.navigation.ui.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.typedef.TimeFormatType;
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter;

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
