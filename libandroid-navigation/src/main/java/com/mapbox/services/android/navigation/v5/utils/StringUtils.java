package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.utils.TextUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfHelpers;

import java.text.DecimalFormat;
import java.util.Locale;

public class StringUtils {

  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s miles";
  private static final String FEET_STRING_FORMAT = "%s feet";

  public static String convertFirstCharLowercase(String instruction) {
    if (TextUtils.isEmpty(instruction)) {
      return instruction;
    } else {
      return instruction.substring(0, 1).toLowerCase() + instruction.substring(1);
    }
  }

  /**
   * If over 1099 feet, use miles format.  If less, use feet in intervals of 100
   *
   * @param distance given distance extracted from {@link RouteProgress}
   * @return {@link String} in either feet (int) or miles (decimal) format
   * @since 0.4.0
   */
  public static String distanceFormatter(double distance) {
    String formattedString;
    if (TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET) > 1099) {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES);
      DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);
      double roundedNumber = (distance / 100 * 100);
      formattedString = String.format(Locale.US, MILES_STRING_FORMAT, df.format(roundedNumber));
    } else {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET);
      int roundedNumber = ((int) Math.round(distance)) / 100 * 100;
      formattedString = String.format(Locale.US, FEET_STRING_FORMAT, roundedNumber);
    }
    return formattedString;
  }
}