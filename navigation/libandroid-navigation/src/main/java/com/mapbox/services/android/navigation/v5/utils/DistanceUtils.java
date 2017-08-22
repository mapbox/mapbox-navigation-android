package com.mapbox.services.android.navigation.v5.utils;

import android.text.Spannable;
import android.text.SpannableStringBuilder;

import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfHelpers;

import java.text.DecimalFormat;
import java.util.Locale;

public class DistanceUtils {

  private static final String DECIMAL_FORMAT = "#.#";
  private static final String MILE_FORMAT = "%s mi";
  private static final String FEET_FORMAT = "%s ft";

  public static SpannableStringBuilder distanceFormatterBold(double distance) {
    SpannableStringBuilder formattedString;
    if (longDistance(distance)) {
      formattedString = roundToNearestMile(distance);
    } else if (mediumDistance(distance)) {
      formattedString = roundOneDecimalPlace(distance);
    } else {
      formattedString = roundByFiftyFeet(distance);
    }
    return formattedString;
  }

  private static SpannableStringBuilder roundByFiftyFeet(double distance) {
    distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET);
    int roundedNumber = ((int) Math.round(distance)) / 50 * 50;

    SpannableStringBuilder formattedString
      = new SpannableStringBuilder(String.format(Locale.US, FEET_FORMAT, roundedNumber));
    formattedString.setSpan(
      new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
      0, String.valueOf(roundedNumber).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return formattedString;
  }

  private static SpannableStringBuilder roundOneDecimalPlace(double distance) {
    distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES);
    DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);
    double roundedNumber = (distance / 100 * 100);
    SpannableStringBuilder formattedString = new SpannableStringBuilder(String.format(Locale.US, MILE_FORMAT,
      df.format(roundedNumber)));
    formattedString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
      0, df.format(roundedNumber).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return formattedString;
  }

  private static boolean mediumDistance(double distance) {
    return TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES) < 10
      && TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET) > 401;
  }

  private static SpannableStringBuilder roundToNearestMile(double distance) {
    distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES);
    SpannableStringBuilder formattedString
      = new SpannableStringBuilder(String.format(Locale.US, MILE_FORMAT, (int) Math.round(distance)));
    formattedString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
      0, String.valueOf((int) Math.round(distance)).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return formattedString;
  }

  private static boolean longDistance(double distance) {
    return TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES) > 10;
  }
}
