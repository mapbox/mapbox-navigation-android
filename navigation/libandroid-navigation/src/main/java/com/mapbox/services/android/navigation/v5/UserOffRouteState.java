package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

class UserOffRouteState {

  private MapboxNavigationOptions options;
  private RouteProgress routeProgress;
  private Location location;

  UserOffRouteState(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    this.location = location;
    this.routeProgress = routeProgress;
    this.options = options;
  }

  boolean isUserOffRoute() {
    Position futurePosition = getFuturePosition();
    double distanceToNextStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    double radius = Math.min(options.getMaximumDistanceOffRoute(), location.getAccuracy() + distanceToNextStep);

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

  private Position getFuturePosition() {
    // Find future location of user
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    double metersInFrontOfUser = location.getSpeed() * options.getDeadReckoningTimeInterval();
    return TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );
  }

  private double userTrueDistanceFromRoute(Position futurePosition) {
    return TurfMeasurement.distance(
      Point.fromCoordinates(futurePosition),
      Point.fromCoordinates(routeProgress.getUsersCurrentSnappedPosition()),
      TurfConstants.UNIT_DEFAULT
    );
  }
}
