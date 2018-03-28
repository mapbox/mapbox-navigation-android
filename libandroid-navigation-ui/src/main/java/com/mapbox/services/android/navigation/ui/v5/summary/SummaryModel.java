package com.mapbox.services.android.navigation.ui.v5.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.util.Calendar;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatTime;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatTimeRemaining;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(Context context, RouteProgress progress, Locale locale,
                      @NavigationUnitType.UnitType int unitType, @NavigationTimeFormat.Type int timeFormatType) {
    distanceRemaining = new DistanceUtils(context, locale, unitType)
      .formatDistance(progress.distanceRemaining()).toString();
    timeRemaining = formatTimeRemaining(progress.durationRemaining());
    Calendar time = Calendar.getInstance();
    boolean isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
    arrivalTime = formatTime(time, progress.durationRemaining(), timeFormatType, isTwentyFourHourFormat);
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
