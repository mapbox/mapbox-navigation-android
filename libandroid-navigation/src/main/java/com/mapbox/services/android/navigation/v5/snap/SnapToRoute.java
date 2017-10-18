package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.List;

import static com.mapbox.services.constants.Constants.PRECISION_6;

/**
 * This attempts to snap the user to the closest position along the route. Prior to snapping the
 * user, their location's checked to ensure that the user didn't veer off-route. If your application
 * uses the Mapbox Map SDK, querying the map and snapping the user to the road grid might be a
 * better solution.
 *
 * @since 0.4.0
 */
public class SnapToRoute extends Snap {

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress,
                                     @Nullable List<Point> coords) {
    location = snapLocationLatLng(location, coords);
    location.setBearing(snapLocationBearing(routeProgress));
    return location;
  }

  /**
   * Logic used to snap the users location coordinates to the closest position along the current
   * step.
   *
   * @param location        the raw location
   * @param coords          the list of step geometry coordinates
   * @return the altered user location
   * @since 0.4.0
   */
  private static Location snapLocationLatLng(Location location,
                                             List<Point> coords) {
    Point locationToPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    if (coords.size() > 1) {
      Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
      Point point = ((Point) feature.geometry());
      location.setLongitude(point.longitude());
      location.setLatitude(point.latitude());
    }
    return location;
  }

  private static float snapLocationBearing(RouteProgress routeProgress) {
    LineString lineString = LineString.fromPolyline(
      routeProgress.currentLegProgress().currentStep().geometry(), PRECISION_6);

    Point currentPoint = TurfMeasurement.along(
      lineString, routeProgress.currentLegProgress().currentStepProgress().distanceTraveled(),
      TurfConstants.UNIT_METERS);
    // Measure 1 meter ahead of the users current location
    Point futurePoint = TurfMeasurement.along(
      lineString,
      routeProgress.currentLegProgress().currentStepProgress().distanceTraveled() + 1,
      TurfConstants.UNIT_METERS);

    double azimuth = TurfMeasurement.bearing(currentPoint, futurePoint);

    // Get bearing and convert azimuth to degrees
    return (float) MathUtils.wrap(azimuth, 0, 360);
  }
}