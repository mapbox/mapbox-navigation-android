package com.mapbox.services.android.navigation.v5.utils.time;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

  private static final String ARRIVAL_TIME_STRING_FORMAT = "%tl:%tM %tp%n";
  private static final int STRING_BUILDER_CAPACITY = 64;
  private static final String DAYS = " days ";
  private static final String HOUR = " hr ";
  private static final String MINUTE = " min ";
  private static final String ONE_MINUTE = "1 min";

  public static String formatArrivalTime(double routeDuration) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, (int) routeDuration);

    return String.format(Locale.getDefault(), ARRIVAL_TIME_STRING_FORMAT,
      calendar, calendar, calendar);
  }

  public static String formatTimeRemaining(double routeDuration) {
    long seconds = (long) routeDuration;

    if (seconds < 0) {
      throw new IllegalArgumentException("Duration must be greater than zero.");
    }

    long days = TimeUnit.SECONDS.toDays(seconds);
    seconds -= TimeUnit.DAYS.toSeconds(days);
    long hours = TimeUnit.SECONDS.toHours(seconds);
    seconds -= TimeUnit.HOURS.toSeconds(hours);
    long minutes = TimeUnit.SECONDS.toMinutes(seconds);
    seconds -= TimeUnit.MINUTES.toSeconds(minutes);

    if (seconds >= 30) {
      minutes = minutes + 1;
    }

    StringBuilder sb = new StringBuilder(STRING_BUILDER_CAPACITY);
    if (days != 0) {
      sb.append(days);
      sb.append(DAYS);
    }
    if (hours != 0) {
      sb.append(hours);
      sb.append(HOUR);
    }
    if (minutes != 0) {
      sb.append(minutes);
      sb.append(MINUTE);
    }
    if (days == 0 && hours == 0 && minutes == 0) {
      sb.append(ONE_MINUTE);
    }

    return (sb.toString());
  }

}
