package com.mapbox.services.android.navigation.v5.utils.time;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.utils.span.SpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;
import com.mapbox.services.android.navigation.v5.utils.span.TextSpanItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

  private static final String TIME_STRING_FORMAT = " %s ";

  public static String formatTime(Calendar time, double routeDuration, @NavigationTimeFormat.Type int type,
                                  boolean isDeviceTwentyFourHourFormat) {
    time.add(Calendar.SECOND, (int) routeDuration);
    TimeFormattingChain chain = new TimeFormattingChain();
    return chain.setup(isDeviceTwentyFourHourFormat).obtainTimeFormatted(type, time);
  }

  public static SpannableStringBuilder formatTimeRemaining(Context context, double routeDuration) {
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
    Resources resources = context.getResources();
    formatDays(resources, days, textSpanItems);
    formatHours(context, hours, textSpanItems);
    formatMinutes(context, minutes, textSpanItems);
    formatNoData(context, days, hours, minutes, textSpanItems);
    return SpanUtils.combineSpans(textSpanItems);
  }

  private static void formatDays(Resources resources, long days, List<SpanItem> textSpanItems) {
    if (days != 0) {
      String dayQuantityString = resources.getQuantityString(R.plurals.numberOfDays, (int) days);
      String dayString = String.format(TIME_STRING_FORMAT, dayQuantityString);
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(days)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), dayString));
    }
  }

  private static void formatHours(Context context, long hours, List<SpanItem> textSpanItems) {
    if (hours != 0) {
      String hourString = String.format(TIME_STRING_FORMAT, context.getString(R.string.hr));
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(hours)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), hourString));
    }
  }

  private static void formatMinutes(Context context, long minutes, List<SpanItem> textSpanItems) {
    if (minutes != 0) {
      String minuteString = String.format(TIME_STRING_FORMAT, context.getString(R.string.min));
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(minutes)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), minuteString));
    }
  }

  private static void formatNoData(Context context, long days, long hours, long minutes,
                                   List<SpanItem> textSpanItems) {
    if (days == 0 && hours == 0 && minutes == 0) {
      String minuteString = String.format(TIME_STRING_FORMAT, context.getString(R.string.min));
      textSpanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), String.valueOf(1)));
      textSpanItems.add(new TextSpanItem(new RelativeSizeSpan(1f), minuteString));
    }
  }
}
