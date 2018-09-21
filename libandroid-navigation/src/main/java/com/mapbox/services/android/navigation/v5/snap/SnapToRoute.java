package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.MathUtils;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.List;

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
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    Location snappedLocation = new Location(location);
    Point locationToPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    List<Point> stepCoordinates = routeProgress.currentStepPoints();
    if (stepCoordinates.size() > 1) {
      Feature feature = TurfMisc.nearestPointOnLine(locationToPoint, stepCoordinates);
      Point point = ((Point) feature.geometry());
      snappedLocation.setLongitude(point.longitude());
      snappedLocation.setLatitude(point.latitude());

      float bearing = calculateBearing(location, point, stepCoordinates);
      snappedLocation.setBearing(bearing);
    }
    return snappedLocation;
  }

  private float calculateBearing(Location location, Point snappedLocation, List<Point> stepCoordinates) {
    LineString stepLineString = LineString.fromLngLats(stepCoordinates);
    Point maneuverPoint = stepCoordinates.get(stepCoordinates.size() - 1);
    // get slice line string between current point to maneuver point
    LineString remainingStepLineString = TurfMisc.lineSlice(snappedLocation, maneuverPoint, stepLineString);
    List<Point> coordinates = remainingStepLineString.coordinates();

    // has current point and next point
    if (coordinates.size() > 1) {
      // get second point to have upcoming point
      Point upcomingPoint = coordinates.get(1);
      double azimuth = TurfMeasurement.bearing(snappedLocation, upcomingPoint);
      return (float) MathUtils.wrap(azimuth, 0, 360);
    } else {
      return location.getBearing();
    }
  }
}