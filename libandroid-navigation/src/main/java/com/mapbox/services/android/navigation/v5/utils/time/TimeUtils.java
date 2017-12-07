package com.mapbox.services.android.navigation.v5.utils.time;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.mapbox.services.android.navigation.v5.utils.span.SpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.TextSpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

  private static final String ARRIVAL_TIME_STRING_FORMAT = "%tl:%tM %tp%n";
  private static final String DAY = " day ";
  private static final String DAYS = " days ";
  private static final String HOUR = " hr ";
  private static final String MINUTE = " min ";

  public static String formatArrivalTime(double routeDuration) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, (int) routeDuration);

    return String.format(Locale.getDefault(), ARRIVAL_TIME_STRING_FORMAT,
      calendar, calendar, calendar);
  }

  public static SpannableStringBuilder formatTimeRemaining(double routeDuration) {
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

    List<SpanItem> textSpanItems = new ArrayList<>();
    if (days != 0) {
      String dayFormat = days > 1 ? DAYS : DAY;
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(days)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), dayFormat));
    }
    if (hours != 0) {
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(hours)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), HOUR));
    }
    if (minutes != 0) {
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(minutes)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), MINUTE));
    }
    if (days == 0 && hours == 0 && minutes == 0) {
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(1)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), MINUTE));
    }

    return SpanUtils.combineSpans(textSpanItems);
  }

  public static long dateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
  }

}
