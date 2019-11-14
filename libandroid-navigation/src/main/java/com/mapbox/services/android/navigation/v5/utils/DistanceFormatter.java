package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
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

public class DistanceFormatter {

  private static final int LARGE_UNIT_THRESHOLD = 10;
  private static final int SMALL_UNIT_THRESHOLD = 401;
  @NavigationConstants.RoundingIncrement
  private final int roundingIncrement;
  private final Map<String, String> unitStrings = new HashMap<>();
  private final NumberFormat numberFormat;
  private final String largeUnit;
  private final String smallUnit;
  private final LocaleUtils localeUtils;
  private final String language;
  private final String unitType;

  /**
   * Creates an instance of DistanceFormatter, which can format distances in meters
   * based on a language format and unit type.
   * <p>
   * This constructor will infer device language and unit type using the device locale.
   *
   * @param context  from which to get localized strings from
   * @param language for which language
   * @param unitType to use, or NONE_SPECIFIED to use default for locale country
   * @param roundingIncrement increment by which to round small distances
   */
  public DistanceFormatter(Context context, @NonNull String language,
                           @NonNull @DirectionsCriteria.VoiceUnitCriteria String unitType,
                           @NavigationConstants.RoundingIncrement int roundingIncrement) {
    this.roundingIncrement = roundingIncrement;
    localeUtils = new LocaleUtils();

    unitStrings.put(UNIT_KILOMETERS, context.getString(R.string.kilometers));
    unitStrings.put(UNIT_METERS, context.getString(R.string.meters));
    unitStrings.put(UNIT_MILES, context.getString(R.string.miles));
    unitStrings.put(UNIT_FEET, context.getString(R.string.feet));

    Locale locale;
    if (language == null) {
      locale = localeUtils.inferDeviceLocale(context);
    } else {
      locale = new Locale(language);
    }
    this.language = locale.getLanguage();
    numberFormat = NumberFormat.getNumberInstance(locale);

    if (!DirectionsCriteria.IMPERIAL.equals(unitType) && !DirectionsCriteria.METRIC.equals(unitType)) {
      unitType = localeUtils.getUnitTypeForDeviceLocale(context);
    }
    this.unitType = unitType;

    largeUnit = DirectionsCriteria.IMPERIAL.equals(unitType) ? UNIT_MILES : UNIT_KILOMETERS;
    smallUnit = DirectionsCriteria.IMPERIAL.equals(unitType) ? UNIT_FEET : UNIT_METERS;
  }

  /**
   * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
   *
   * @param distance in meters
   * @return SpannableString representation which has a bolded number and units which have a
   * relative size of .65 times the size of the number
   */
  public SpannableString formatDistance(double distance) {
    double distanceSmallUnit = TurfConversion.convertLength(distance, TurfConstants.UNIT_METERS, smallUnit);
    double distanceLargeUnit = TurfConversion.convertLength(distance, TurfConstants.UNIT_METERS, largeUnit);

    // If the distance is greater than 10 miles/kilometers, then round to nearest mile/kilometer
    if (distanceLargeUnit > LARGE_UNIT_THRESHOLD) {
      return getDistanceString(roundToDecimalPlace(distanceLargeUnit, 0), largeUnit);
      // If the distance is less than 401 feet/meters, round by fifty feet/meters
    } else if (distanceSmallUnit < SMALL_UNIT_THRESHOLD) {
      return getDistanceString(roundToClosestIncrement(distanceSmallUnit), smallUnit);
      // If the distance is between 401 feet/meters and 10 miles/kilometers, then round to one decimal place
    } else {
      return getDistanceString(roundToDecimalPlace(distanceLargeUnit, 1), largeUnit);
    }
  }

  /**
   * Method that can be used to check if an instance of {@link DistanceFormatter}
   * needs to be updated based on the passed language / unitType.
   *
   * @param language to check against the current formatter language
   * @param unitType to check against the current formatter unitType
   * @return true if new formatter is needed, false otherwise
   */
  public boolean shouldUpdate(@NonNull String language, @NonNull String unitType, int roundingIncrement) {
    return !this.language.equals(language) || !this.unitType.equals(unitType)
      || !(this.roundingIncrement == roundingIncrement);
  }

  /**
   * Returns number rounded to closest specified rounding increment, unless the number is less than
   * the rounding increment, then the rounding increment is returned
   *
   * @param distance to round to closest specified rounding increment
   * @return number rounded to closest rounding increment, or rounding increment if distance is less
   */
  private String roundToClosestIncrement(double distance) {
    int roundedNumber = ((int) Math.round(distance)) / roundingIncrement * roundingIncrement;

    return String.valueOf(roundedNumber < roundingIncrement ? roundingIncrement : roundedNumber);
  }

  /**
   * Rounds given number to the given decimal place
   *
   * @param distance     to round
   * @param decimalPlace number of decimal places to round
   * @return distance rounded to given decimal places
   */
  private String roundToDecimalPlace(double distance, int decimalPlace) {
    numberFormat.setMaximumFractionDigits(decimalPlace);

    return numberFormat.format(distance);
  }

  /**
   * Takes in a distance and units and returns a formatted SpannableString where the number is bold
   * and the unit is shrunked to .65 times the size
   *
   * @param distance formatted with appropriate decimal places
   * @param unit     string from TurfConstants. This will be converted to the abbreviated form.
   * @return String with bolded distance and shrunken units
   */
  private SpannableString getDistanceString(String distance, String unit) {
    SpannableString spannableString = new SpannableString(String.format("%s %s", distance, unitStrings.get(unit)));

    spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, distance.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannableString.setSpan(new RelativeSizeSpan(0.65f), distance.length() + 1,
      spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spannableString;
  }

  public static int calculateAbsoluteDistance(Location currentLocation, MetricsRouteProgress metricProgress) {
    Point currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    Point finalPoint = metricProgress.getDirectionsRouteDestination();

    return (int) TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS);
  }
}
