package com.mapbox.services.android.navigation.ui.v5.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatArrivalTime;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeUtils.formatTimeRemaining;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(Context context, RouteProgress progress,
                      Locale locale, @NavigationUnitType.UnitType int unitType) {
    distanceRemaining = new DistanceUtils(context, locale, unitType)
      .formatDistance(progress.distanceRemaining()).toString();
    timeRemaining = formatTimeRemaining(progress.durationRemaining());
    arrivalTime = formatArrivalTime(progress.durationRemaining());
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
