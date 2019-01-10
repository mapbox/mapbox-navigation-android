package com.mapbox.services.android.navigation.v5.navigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class NavigationTelemetryUtils {
  private static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US);

  static String obtainCurrentDate() {
    return dateFormat.format(new Date());
  }
}
