package com.mapbox.services.android.navigation.v5.utils;

import android.graphics.Typeface;
import android.location.Location;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfConversion;
import com.mapbox.turf.TurfMeasurement;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.mapbox.turf.TurfConstants.UNIT_FEET;
import static com.mapbox.turf.TurfConstants.UNIT_KILOMETERS;
import static com.mapbox.turf.TurfConstants.UNIT_METERS;
import static com.mapbox.turf.TurfConstants.UNIT_MILES;

public class DistanceUtils {
  private static final Map<String, String> UNIT_STRINGS = new HashMap<>();

  static {
    UNIT_STRINGS.put(UNIT_KILOMETERS, "km");
    UNIT_STRINGS.put(UNIT_METERS, "m");
    UNIT_STRINGS.put(UNIT_MILES, "mi");
    UNIT_STRINGS.put(UNIT_FEET, "ft");
  }

  private static final int LARGE_UNIT_THRESHOLD = 10;
  private static final int SMALL_UNIT_THRESHOLD = 401;

  private static NumberFormat numberFormat;



  /**
   * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
   * @param distance in meters
   * @param locale from which to format numbers
   * @param unitType NavigationUnitType.TYPE_IMPERIAL or NavigationUnitType.TYPE_METRIC
   * @return SpannableString representation which has a bolded number and units which have a
   * relative size of .65 times the size of the number
   */
  public static SpannableString formatDistance(double distance,
                                               Locale locale,
                                               @NavigationUnitType.UnitType int unitType) {
    numberFormat = NumberFormat.getNumberInstance(locale);

    boolean isImperialUnitType = unitType == NavigationUnitType.TYPE_IMPERIAL;
    String largeUnit = isImperialUnitType ? UNIT_MILES : TurfConstants.UNIT_KILOMETERS;
    String smallUnit = isImperialUnitType ? TurfConstants.UNIT_FEET : TurfConstants.UNIT_METERS;

    double distanceSmallUnit = TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, smallUnit);
    double distanceLargeUnit = TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, largeUnit);

    // If the distance is greater than 10 miles/kilometers, then round to nearest mile/kilometer
    if (distanceLargeUnit > LARGE_UNIT_THRESHOLD) {
      return getDistanceString(roundToDecimalPlace(distanceLargeUnit, 0), largeUnit);
      // If the distance is less than 401 feet/meters, round by fifty feet/meters
    } else if (distanceSmallUnit < SMALL_UNIT_THRESHOLD) {
      return getDistanceString(roundToClosestFifty(distanceSmallUnit), smallUnit);
      // If the distance is between 401 feet/meters and 10 miles/kilometers, then round to one decimal place
    } else {
      return getDistanceString(roundToDecimalPlace(distanceLargeUnit, 1), largeUnit);
    }
  }

  /**
   * Returns number rounded to closest fifty, unless the number is less than fifty, then fifty is returned
   * @param distance to round to closest fifty
   * @return number rounded to closest fifty, or fifty if distance is less than fifty
   */
  private static String roundToClosestFifty(double distance) {
    int roundedNumber = ((int) Math.round(distance)) / 50 * 50;

    return String.valueOf(roundedNumber < 50 ? 50 : roundedNumber);
  }

  /**
   * Rounds given number to the given decimal place
   * @param distance to round
   * @param decimalPlace number of decimal places to round
   * @return distance rounded to given decimal places
   */
  private static String roundToDecimalPlace(double distance, int decimalPlace) {
    numberFormat.setMaximumFractionDigits(decimalPlace);

    return numberFormat.format(distance);
  }

  /**
   * Takes in a distance and units and returns a formatted SpannableString where the number is bold
   * and the unit is shrunked to .65 times the size
   * @param distance formatted with appropriate decimal places
   * @param unit string from TurfConstants. This will be converted to the abbreviated form.
   * @return String with bolded distance and shrunken units
   */
  private static SpannableString getDistanceString(String distance, String unit) {
    SpannableString spannableString = new SpannableString(String.format("%s %s", distance, UNIT_STRINGS.get(unit)));

    spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, distance.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannableString.setSpan(new RelativeSizeSpan(0.65f), distance.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spannableString;
  }

  public static int calculateAbsoluteDistance(Location currentLocation, MetricsRouteProgress metricProgress) {
    Point currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    Point finalPoint = metricProgress.getDirectionsRouteDestination();

    return (int) TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS);
  }
}
