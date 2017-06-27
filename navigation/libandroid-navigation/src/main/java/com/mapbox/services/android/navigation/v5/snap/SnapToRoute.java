package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.RouteProgress;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

import static com.mapbox.services.Constants.PRECISION_6;

public class SnapToRoute extends Snap {

  private RouteProgress routeProgress;
  private MapboxNavigationOptions options;

  public SnapToRoute(RouteProgress routeProgress, MapboxNavigationOptions options) {
    this.routeProgress = routeProgress;
    this.options = options;
  }

  @Override
  public Location getSnappedLocation(Location location) {
    if (validLocationToSnap(location)) {
      location = snapLocationLatLng(location, routeProgress.getCurrentLegProgress().getCurrentStep().getGeometry());
    } else {
      location.setLatitude(routeProgress.userSnappedPosition().getLatitude());
      location.setLongitude(routeProgress.userSnappedPosition().getLongitude());
    }
    location.setBearing(snapLocationBearing(location, routeProgress, options));
    return location;
  }

  /**
   * Logic used to snap the users location coordinates to the closest position along the current step.
   *
   * @param location     the raw location
   * @param stepGeometry the navigation session's current step
   * @return the altered user location
   * @since 0.4.0
   */
  private static Location snapLocationLatLng(Location location, String stepGeometry) {
    Point locationToPoint = Point.fromCoordinates(
      new double[] {location.getLongitude(), location.getLatitude()}
    );

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(stepGeometry, PRECISION_6);

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
    Position position = ((Point) feature.getGeometry()).getCoordinates();
    location.setLongitude(position.getLongitude());
    location.setLatitude(position.getLatitude());
    return location;
  }

  private static float snapLocationBearing(Location location, RouteProgress routeProgress,
                                           MapboxNavigationOptions options) {
    LineString lineString = LineString.fromPolyline(routeProgress.getRoute().getGeometry(), PRECISION_6);
    double userDistanceBuffer = location.getSpeed() * options.getDeadReckoningTimeInterval();

    Point currentPoint = TurfMeasurement.along(lineString, routeProgress.getDistanceTraveled(),
      TurfConstants.UNIT_METERS);
    Point futurePoint = TurfMeasurement.along(lineString, routeProgress.getDistanceTraveled() + userDistanceBuffer,
      TurfConstants.UNIT_METERS);

    double azimuth = TurfMeasurement.bearing(currentPoint, futurePoint);

    // Get bearing and convert azimuth to degrees
    return (float) MathUtils.wrap(azimuth, 0, 360);
  }
}
