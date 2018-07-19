package com.mapbox.services.android.navigation.ui.v5.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

import java.util.Calendar;

import static com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTimeRemaining;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(Context context, RouteProgress progress, String language,
                      @DirectionsCriteria.VoiceUnitCriteria String unitType,
                      @NavigationTimeFormat.Type int timeFormatType) {
    distanceRemaining = new DistanceFormatter(context, language, unitType)
      .formatDistance(progress.distanceRemaining()).toString();
    timeRemaining = formatTimeRemaining(context, progress.durationRemaining());
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
