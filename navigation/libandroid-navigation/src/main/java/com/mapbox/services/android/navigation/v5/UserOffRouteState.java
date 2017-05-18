package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

class UserOffRouteState {

  private MapboxNavigationOptions options;
  private RouteProgress routeProgress;
  private Location location;

  /**
   * Constructor taking in all the needed parameters for detecting if the users off-route.
   *
   * @param location      users current location
   * @param routeProgress a {@link RouteProgress} object
   * @param options       the {@link MapboxNavigationOptions} options set
   * @since 0.2.0
   */
  UserOffRouteState(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    this.location = location;
    this.routeProgress = routeProgress;
    this.options = options;
  }

  /**
   * Detects if the user is off route or not.
   *
   * @return true if the users off-route, else false.
   * @since 0.2.0
   */
  boolean isUserOffRoute() {
    Position futurePosition = getFuturePosition();
    double radius = Math.min(options.getMaximumDistanceOffRoute(),
      location.getAccuracy() + options.getUserLocationSnapDistance());

    boolean isOffRoute = userTrueDistanceFromRoute(futurePosition) > radius;

    // If the user is moving away from the maneuver location and they are close to the next step we can safely say they
    // have completed the maneuver. This is intended to be a fallback case when we do find that the users course matches
    // the exit bearing.
    boolean isCloseToUpcomingStep;

    if (routeProgress.getCurrentLegProgress().getUpComingStep() != null) {
      isCloseToUpcomingStep = userTrueDistanceFromRoute(futurePosition) < radius;
      if (isOffRoute && isCloseToUpcomingStep) {
        return false;
      }
    }
    return isOffRoute;
  }

  /**
   * uses dead reckoning to find the users future location.
   *
   * @return a {@link Position}
   * @since 0.2.0
   */
  private Position getFuturePosition() {
    // Find future location of user
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    double metersInFrontOfUser = location.getSpeed() * options.getDeadReckoningTimeInterval();
    return TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );
  }

  /**
   * Gets the distance from the users true location to their snapped location along the route.
   *
   * @param futurePosition a {@link Position} value
   * @return double distance in meters
   * @since 0.2.0
   */
  private double userTrueDistanceFromRoute(Position futurePosition) {
    LineString lineString = LineString.fromPolyline(
      routeProgress.getCurrentLegProgress().getCurrentStep().getGeometry(), Constants.PRECISION_6);
    Feature feature = TurfMisc.pointOnLine(Point.fromCoordinates(futurePosition), lineString.getCoordinates());

    Point snappedPoint = (Point) feature.getGeometry();

    return TurfMeasurement.distance(
      Point.fromCoordinates(futurePosition),
      snappedPoint,
      TurfConstants.UNIT_METERS
    );
  }
}
