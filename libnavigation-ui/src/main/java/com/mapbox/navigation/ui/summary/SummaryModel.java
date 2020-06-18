package com.mapbox.navigation.ui.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.internal.extensions.LocaleEx;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.trip.notification.internal.TimeFormatter;
import timber.log.Timber;

import java.util.Calendar;
import java.util.Locale;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(final Context context, final DistanceFormatter distanceFormatter, final RouteProgress progress,
                      final @TimeFormat.Type int timeFormatType) {
    final Locale locale = LocaleEx.getLocaleDirectionsRoute(progress.getRoute(), context);
    distanceRemaining = distanceFormatter.formatDistance(progress.getDistanceRemaining()).toString();
    // TODO Method invocation 'getDurationRemaining' may produce 'NullPointerException'
    final double legDurationRemaining = progress.getCurrentLegProgress().getDurationRemaining();
    timeRemaining = TimeFormatter.formatTimeRemaining(context, legDurationRemaining, locale);
    final Calendar time = Calendar.getInstance();

    boolean isTwentyFourHourFormat;
    try {
      isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
    } catch (NullPointerException npe) {
      Timber.e(npe, "isTwentyFourHourFormat set to true when NPE occurs");
      isTwentyFourHourFormat = true;
    }

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
