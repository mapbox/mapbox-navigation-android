package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import java.util.List;

import static com.mapbox.services.Constants.PRECISION_6;

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
                                     @Nullable List<Position> coords) {
    Location snappedLocation = new Location(String.format("%s-snapped", location.getProvider()));
    snappedLocation = snapLocationLatLng(location, snappedLocation, coords);
    snappedLocation.setBearing(snapLocationBearing(routeProgress));
    return snappedLocation;
  }

  /**
   * Logic used to snap the users location coordinates to the closest position along the current
   * step.
   *
   * @param location        the raw location
   * @param snappedLocation new Location object representing the snapped position
   * @param coords          the list of step geometry coordinates
   * @return the altered user location
   * @since 0.4.0
   */
  private static Location snapLocationLatLng(Location location, Location snappedLocation,
                                             List<Position> coords) {
    Point locationToPoint = Point.fromCoordinates(
      new double[] {location.getLongitude(), location.getLatitude()}
    );

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    if (coords.size() > 1) {
      Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
      Position position = ((Point) feature.getGeometry()).getCoordinates();
      snappedLocation.setLongitude(position.getLongitude());
      snappedLocation.setLatitude(position.getLatitude());
    }
    return snappedLocation;
  }

  private static float snapLocationBearing(RouteProgress routeProgress) {
    LineString lineString = LineString.fromPolyline(
      routeProgress.currentLegProgress().currentStep().getGeometry(), PRECISION_6);

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