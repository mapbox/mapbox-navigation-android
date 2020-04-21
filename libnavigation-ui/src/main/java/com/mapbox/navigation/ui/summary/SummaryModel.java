package com.mapbox.navigation.ui.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import com.mapbox.navigation.base.internal.extensions.LocaleEx;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.typedef.TimeFormatType;
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter;

import java.util.Calendar;
import java.util.Locale;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(final Context context, final DistanceFormatter distanceFormatter, final RouteProgress progress,
                      final @TimeFormatType int timeFormatType) {
    final Locale locale = progress.route() == null ? null :
            LocaleEx.getLocaleDirectionsRoute(progress.route(), context);
    distanceRemaining = distanceFormatter.formatDistance(progress.distanceRemaining()).toString();
    final double legDurationRemaining = progress.currentLegProgress().durationRemaining();
    timeRemaining = TimeFormatter.formatTimeRemaining(context, legDurationRemaining, locale);
    final Calendar time = Calendar.getInstance();
    final boolean isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
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
