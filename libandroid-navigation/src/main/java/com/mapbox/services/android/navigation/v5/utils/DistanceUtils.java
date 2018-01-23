package com.mapbox.services.android.navigation.v5.utils;

import android.graphics.Typeface;
import android.location.Location;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.utils.span.SpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.TextSpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfConversion;
import com.mapbox.turf.TurfMeasurement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DistanceUtils {

  private static final int LARGE_UNIT_THRESHOLD = 10;
  private static final int SMALL_UNIT_THRESHOLD = 401;
  private static final String MILE = " mi";
  private static final String FEET = " ft";
  private static final String KILOMETER = " km";
  private static final String METER = " m";

  public static SpannableStringBuilder distanceFormatter(double distance,
                                                         DecimalFormat decimalFormat,
                                                         boolean spansEnabled,
                                                         int unitType) {

    boolean isImperialUnitType = unitType == NavigationUnitType.TYPE_IMPERIAL;

    String largeUnitFormat = isImperialUnitType ? MILE : KILOMETER;
    String smallUnitFormat = isImperialUnitType ? FEET : METER;
    String largeFinalUnit = isImperialUnitType ? TurfConstants.UNIT_MILES : TurfConstants.UNIT_KILOMETERS;
    String smallFinalUnit = isImperialUnitType ? TurfConstants.UNIT_FEET : TurfConstants.UNIT_METERS;

    SpannableStringBuilder formattedString;
    if (longDistance(distance, largeFinalUnit)) {
      formattedString = roundToNearestMile(distance, spansEnabled, largeFinalUnit, largeUnitFormat);
    } else if (mediumDistance(distance, largeFinalUnit, smallFinalUnit)) {
      formattedString = roundOneDecimalPlace(distance, decimalFormat, spansEnabled,
        largeFinalUnit, largeUnitFormat);
    } else {
      formattedString = roundByFiftyFeet(distance, spansEnabled, smallUnitFormat, smallFinalUnit);
    }
    return formattedString;
  }

  private static SpannableStringBuilder roundByFiftyFeet(double distance, boolean spansEnabled,
                                                         String smallUnitFormat, String smallFinalUnit) {
    distance = TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, smallFinalUnit);

    // Distance value
    int roundedNumber = ((int) Math.round(distance)) / 50 * 50;
    roundedNumber = roundedNumber < 50 ? 50 : roundedNumber;

    if (spansEnabled) {
      return generateSpannedText(String.valueOf(roundedNumber), smallUnitFormat);
    } else {
      return new SpannableStringBuilder(String.valueOf(roundedNumber) + smallUnitFormat);
    }
  }

  private static SpannableStringBuilder roundOneDecimalPlace(double distance, DecimalFormat decimalFormat,
                                                             boolean spansEnabled, String largeFinalUnit,
                                                             String largeUnitFormat) {
    distance = TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, largeFinalUnit);

    // Distance value
    double roundedNumber = (distance / 100 * 100);
    String roundedDecimal = decimalFormat.format(roundedNumber);

    if (spansEnabled) {
      return generateSpannedText(roundedDecimal, largeUnitFormat);
    } else {
      return new SpannableStringBuilder(roundedDecimal + largeUnitFormat);
    }
  }

  private static SpannableStringBuilder roundToNearestMile(double distance, boolean spansEnabled,
                                                           String largeFinalUnit, String largeUnitFormat) {
    distance = TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, largeFinalUnit);

    // Distance value
    int roundedNumber = (int) Math.round(distance);

    if (spansEnabled) {
      return generateSpannedText(String.valueOf(roundedNumber), largeUnitFormat);
    } else {
      return new SpannableStringBuilder(String.valueOf(roundedNumber) + largeUnitFormat);
    }
  }

  private static boolean mediumDistance(double distance, String largeFinalUnit, String smallFinalUnit) {
    return TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, largeFinalUnit) < LARGE_UNIT_THRESHOLD
      && TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, smallFinalUnit) > SMALL_UNIT_THRESHOLD;
  }

  private static boolean longDistance(double distance, String largeFinalUnit) {
    return TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, largeFinalUnit) > LARGE_UNIT_THRESHOLD;
  }

  public static int calculateAbsoluteDistance(Location currentLocation, MetricsRouteProgress metricProgress) {

    Point currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    Point finalPoint = metricProgress.getDirectionsRouteDestination();

    return (int) TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS);
  }

  private static SpannableStringBuilder generateSpannedText(String distance, String unit) {
    List<SpanItem> spans = new ArrayList<>();
    spans.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), distance));
    spans.add(new TextSpanItem(new RelativeSizeSpan(0.65f), unit));
    return SpanUtils.combineSpans(spans);
  }
}
