package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

public class SnapLocation {

  private MapboxNavigationOptions options;
  private RouteProgress routeProgress;
  private Location location;

  SnapLocation(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    this.location = location;
    this.routeProgress = routeProgress;
  }

  Location getSnappedLocation() {
    // Pass in the snapped location with all the other location data remaining intact for their use.
    location.setLatitude(routeProgress.getUsersCurrentSnappedPosition().getLatitude());
    location.setLongitude(routeProgress.getUsersCurrentSnappedPosition().getLongitude());

    location.setBearing(snapUserBearing());

    return location;
  }

  // TODO split this method
  private float snapUserBearing() {
    LineString lineString = LineString.fromPolyline(routeProgress.getRoute().getGeometry(),
      com.mapbox.services.Constants.PRECISION_6);

    Position newCoordinate;
    newCoordinate = routeProgress.getUsersCurrentSnappedPosition();

    double userDistanceBuffer = location.getSpeed() * options.getDeadReckoningTimeInterval();

    if (routeProgress.getDistanceTraveled() + userDistanceBuffer
      > RouteUtils.getDistanceToEndOfRoute(
      routeProgress.getRoute().getLegs().get(0).getSteps().get(0).getManeuver().asPosition(),
      routeProgress.getRoute(),
      TurfConstants.UNIT_METERS)) {
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
