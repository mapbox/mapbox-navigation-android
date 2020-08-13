package com.mapbox.navigation.ui.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.internal.extensions.LocaleEx;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.trip.notification.internal.TimeFormatter;
import timber.log.Timber;

import java.util.Calendar;
import java.util.Locale;

/**
 * Model that transforms the current route progress to formatted display data.
 * @see SummaryModel#create(Context, DistanceFormatter, RouteProgress, int)
 */
public class SummaryModel {

  /**
   * Creates a new summary display model.
   */
  @NonNull
  public static SummaryModel create(final Context context, final DistanceFormatter distanceFormatter,
                                    final RouteProgress progress,
                                    final @TimeFormat.Type int timeFormatType) {
    final Locale locale = LocaleEx.getLocaleDirectionsRoute(progress.getRoute(), context);
    String distanceRemaining = distanceFormatter.formatDistance(progress.getDistanceRemaining()).toString();
    // TODO Method invocation 'getDurationRemaining' may produce 'NullPointerException'
    final double legDurationRemaining = progress.getCurrentLegProgress().getDurationRemaining();
    SpannableStringBuilder timeRemaining = TimeFormatter.formatTimeRemaining(context, legDurationRemaining, locale);
    final Calendar time = Calendar.getInstance();

    boolean isTwentyFourHourFormat;
    try {
      isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
    } catch (NullPointerException npe) {
      Timber.e(npe, "isTwentyFourHourFormat set to true when NPE occurs");
      isTwentyFourHourFormat = true;
    }

    String arrivalTime = TimeFormatter.formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat);
    return new SummaryModel(distanceRemaining, timeRemaining, arrivalTime);
  }

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  private SummaryModel(@NonNull String distanceRemaining,
                       @NonNull SpannableStringBuilder timeRemaining,
                       @NonNull String arrivalTime) {
    this.distanceRemaining = distanceRemaining;
    this.timeRemaining = timeRemaining;
    this.arrivalTime = arrivalTime;
  }

  @NonNull
  String getDistanceRemaining() {
    return distanceRemaining;
  }

  @NonNull
  SpannableStringBuilder getTimeRemaining() {
    return timeRemaining;
  }

  @NonNull
  String getArrivalTime() {
    return arrivalTime;
  }
}
