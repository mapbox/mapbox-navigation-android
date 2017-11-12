package com.mapbox.services.android.navigation.v5.utils.distance;

import android.location.Location;
import android.text.SpannableStringBuilder;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.text.DecimalFormat;

public abstract class DistanceUtils {

  private static DistanceUtils meterDistanceUtils;
  private static DistanceUtils imperialDistanceUtils;

  public static DistanceUtils get(UnitType unitType){
    switch (unitType){
      case UNIT_METRIC:
         if(meterDistanceUtils == null) meterDistanceUtils = new MeterDistanceUtils();
         return meterDistanceUtils;
      case UNIT_IMPERIAL:
        if(imperialDistanceUtils == null) imperialDistanceUtils = new ImperialDistanceUtils();
        return imperialDistanceUtils;
    }
    throw new RuntimeException("Not defined unit type "+unitType.getValue());
  }

  public abstract SpannableStringBuilder distanceFormatterBold(double distance, DecimalFormat decimalFormat, boolean spansEnabled);

  public abstract SpannableStringBuilder roundBySmallDistance(double distance, boolean spansEnabled);

  public abstract SpannableStringBuilder roundOneDecimalPlace(double distance, DecimalFormat decimalFormat, boolean spansEnabled);

  public abstract SpannableStringBuilder roundToNearest(double distance, boolean spansEnabled);

  public abstract boolean mediumDistance(double distance);

  public abstract boolean longDistance(double distance);

  public static int calculateAbsoluteDistance(Location currentLocation, MetricsRouteProgress routeProgress) {

    Point currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    Point finalPoint = routeProgress.getDirectionsRouteDestination();

    int absoluteDistance = (int) TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS);

    return absoluteDistance;
  }


  public abstract SpannableStringBuilder generateSpannedText(String distance, String unit);

}
