package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

public class SnapLocation {

  private MapboxNavigationOptions options;
  private LegStep currentStep;

  private Location location;

  SnapLocation(Location location, LegStep currentStep, MapboxNavigationOptions options) {
    this.location = location;
    this.currentStep = currentStep;
    this.options = options;
  }

  Location getSnappedLocation() {
    // Pass in the snapped location with all the other location data remaining intact for their use.
    location.setLatitude(getUsersCurrentSnappedPosition().getLatitude());
    location.setLongitude(getUsersCurrentSnappedPosition().getLongitude());
    return location;
  }

  /**
   * Provides the users location snapped to the current route they are navigating on.
   *
   * @return {@link Position} object with coordinates snapping the user to the route.
   * @since 0.1.0
   */
  Position getUsersCurrentSnappedPosition() {
    Point locationToPoint = Point.fromCoordinates(new double[] {location.getLongitude(), location.getLatitude()});
    String stepGeometry = currentStep.getGeometry();

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(stepGeometry, Constants.PRECISION_6);

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
    return ((Point) feature.getGeometry()).getCoordinates();
  }

  float snapUserBearing(RouteProgress routeProgress) {
    LineString lineString = LineString.fromPolyline(routeProgress.getRoute().getGeometry(),
      com.mapbox.services.Constants.PRECISION_6);

    Position newCoordinate;
    newCoordinate = getUsersCurrentSnappedPosition();

    double userDistanceBuffer = location.getSpeed() * options.getDeadReckoningTimeInterval();

    if (routeProgress.getDistanceTraveled() + userDistanceBuffer
      > routeProgress.getRoute().getDistance()) {
      // If the user is near the end of the route, take the remaining distance and divide by two
      userDistanceBuffer = routeProgress.getDistanceRemaining() / 2;
    }

    Point pointOneClosest = TurfMeasurement.along(lineString, routeProgress.getDistanceTraveled()
      + userDistanceBuffer, TurfConstants.UNIT_METERS);
    Point pointTwoClosest = TurfMeasurement.along(lineString, routeProgress.getDistanceTraveled()
      + (userDistanceBuffer * 2), TurfConstants.UNIT_METERS);

    // Get direction of these points
    double pointOneBearing = TurfMeasurement.bearing(Point.fromCoordinates(newCoordinate), pointOneClosest);
    double pointTwoBearing = TurfMeasurement.bearing(Point.fromCoordinates(newCoordinate), pointTwoClosest);

    double wrappedPointOne = MathUtils.wrap(pointOneBearing, -180, 180);
    double wrappedPointTwo = MathUtils.wrap(pointTwoBearing, -180, 180);
    double wrappedCurrentBearing = MathUtils.wrap(location.getBearing(), -180, 180);

    double relativeAnglepointOne = MathUtils.wrap(wrappedPointOne - wrappedCurrentBearing, -180, 180);
    double relativeAnglepointTwo = MathUtils.wrap(wrappedPointTwo - wrappedCurrentBearing, -180, 180);

    double averageRelativeAngle = (relativeAnglepointOne + relativeAnglepointTwo) / 2;

    double absoluteBearing = MathUtils.wrap(wrappedCurrentBearing + averageRelativeAngle, 0, 360);

    if (MathUtils.differenceBetweenAngles(absoluteBearing, location.getBearing())
      > options.getMaxManipulatedCourseAngle()) {
      return location.getBearing();
    }

    return averageRelativeAngle <= options.getMaxTurnCompletionOffset() ? (float)
      absoluteBearing : location.getBearing();
  }
}
