package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.Constants;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

class RouteController {

  private MapboxNavigationOptions options;

  private int currentLegIndex;
  private int currentStepIndex;

  RouteController(@NonNull MapboxNavigationOptions options) {
    this.options = options;
  }

  int getCurrentLegIndex() {
    return currentLegIndex;
  }

  int getCurrentStepIndex() {
    return currentStepIndex;
  }

  int computeAlertLevel(Location userLocation, RouteProgress previousRouteProgress,
                        double userSnapToStepDistanceFromManeuver, double durationRemainingOnStep) {
    int alertLevel = isUserDeparting(previousRouteProgress.getAlertUserLevel(),
      userSnapToStepDistanceFromManeuver, options.getManeuverZoneRadius());

    // The user is near the next steps maneuver.
    if (userSnapToStepDistanceFromManeuver <= options.getManeuverZoneRadius()) {
      alertLevel = isUserArriving(alertLevel, previousRouteProgress);
      // Did the user reach the next steps maneuver?
      if (bearingMatchesManeuverFinalHeading(
        userLocation, previousRouteProgress, options.getMaxTurnCompletionOffset())) {
        increaseIndex(previousRouteProgress);
        alertLevel = nextStepAlert(userLocation, previousRouteProgress);
      }
      // If the users not in the maneuver zone, the alert level could potentially be medium or high.
    } else if (durationRemainingOnStep <= NavigationConstants.HIGH_ALERT_INTERVAL
      && previousRouteProgress.getCurrentLegProgress().getCurrentStep().getDistance()
      > NavigationConstants.MINIMUM_DISTANCE_FOR_HIGH_ALERT) {
      alertLevel = NavigationConstants.HIGH_ALERT_LEVEL;
    } else if (durationRemainingOnStep <= NavigationConstants.MEDIUM_ALERT_INTERVAL
      && previousRouteProgress.getCurrentLegProgress().getCurrentStep().getDistance()
      > NavigationConstants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT) {
      alertLevel = NavigationConstants.MEDIUM_ALERT_LEVEL;
    }

    return alertLevel;
  }

  /**
   * Checks whether the user is departing or not and whether the alert level needs to be
   * {@link NavigationConstants#HIGH_ALERT_LEVEL} or {@link NavigationConstants#DEPART_ALERT_LEVEL} depending on if the
   * user location is within the {@link MapboxNavigationOptions#getManeuverZoneRadius()}.
   *
   * @param alertLevel The currently calculated alert level.
   * @param distance   From the users snapped location to the next steps maneuver.
   * @return either the original alert level (if the user's not departing) or an updated alert level value either
   * representing {@link NavigationConstants#HIGH_ALERT_LEVEL} or {@link NavigationConstants#DEPART_ALERT_LEVEL}
   * @since 0.2.0
   */
  static int isUserDeparting(int alertLevel, double distance, double maneuverZoneRadius) {
    if (alertLevel == NavigationConstants.NONE_ALERT_LEVEL) {
      if (distance > maneuverZoneRadius) {
        alertLevel = NavigationConstants.DEPART_ALERT_LEVEL;
      } else {
        alertLevel = NavigationConstants.HIGH_ALERT_LEVEL;
      }
    }
    return alertLevel;
  }

  /**
   * Checks whether the user is arriving to their final destination or not.
   *
   * @param alertLevel    The currently calculated alert level.
   * @param routeProgress The most recent {@link RouteProgress} object that was created.
   * @return either the original alert level (if the user's not arriving) or an updated alert level value.
   * @since 0.2.0
   */
  static int isUserArriving(int alertLevel, RouteProgress routeProgress) {
    if (routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getType()
      .equals(Constants.STEP_MANEUVER_TYPE_ARRIVE)) {
      return NavigationConstants.ARRIVE_ALERT_LEVEL;
    }
    return alertLevel;
  }

  /**
   * Checks whether the user's bearing matches the next step's maneuver provided bearingAfter variable. This is one of
   * the criteria's required for the user location to be recognized as being on the next step or potentially arriving.
   *
   * @param userLocation
   * @param routeProgress
   * @return
   * @since 0.2.0
   */
  static boolean bearingMatchesManeuverFinalHeading(Location userLocation, RouteProgress routeProgress,
                                                    double maxTurnCompletionOffset) {
    if (routeProgress.getCurrentLegProgress().getUpComingStep() == null) {
      return false;
    }

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    double finalHeading = routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getBearingAfter();
    double finalHeadingNormalized = MathUtils.wrap(finalHeading, 0, 360);
    double userHeadingNormalized = MathUtils.wrap(userLocation.getBearing(), 0, 360);
    return MathUtils.differenceBetweenAngles(finalHeadingNormalized, userHeadingNormalized)
      <= maxTurnCompletionOffset;

  }

  void increaseIndex(RouteProgress routeProgress) {
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (currentStepIndex >= routeProgress.getRoute().getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 1
      && currentLegIndex < routeProgress.getRoute().getLegs().size()) {
      currentLegIndex += 1;
      currentStepIndex = 0;
    } else {
      currentStepIndex += 1;
    }
  }

   int nextStepAlert(Location userLocation, RouteProgress routeProgress) {
    double secondsToEndOfNewStep = RouteUtils.getDistanceToNextStep(
      Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude()),
      routeProgress.getRoute().getLegs().get(currentLegIndex),
      currentStepIndex
    ) / userLocation.getSpeed();
    return secondsToEndOfNewStep <= NavigationConstants.MEDIUM_ALERT_INTERVAL ? NavigationConstants.MEDIUM_ALERT_LEVEL
      : NavigationConstants.LOW_ALERT_LEVEL;
  }

  double calculateSnappedDistanceToNextStep(Location location, RouteProgress routeProgress) {
    // TODO prevent this distance increasing if on same step
    Position truePosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    return RouteUtils.getDistanceToNextStep(
      truePosition,
      routeProgress.getRoute().getLegs().get(currentLegIndex),
      currentStepIndex,
      TurfConstants.UNIT_METERS
    );
  }

  //      double userAbsoluteDistance = TurfMeasurement.distance(
//        truePosition, routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().asPosition(), TurfConstants.UNIT_METERS
//      );
//
//      // userAbsoluteDistanceToManeuverLocation is set to nil by default
//      // If it's set to nil, we know the user has never entered the maneuver radius
//      if (userDistanceToManeuverLocation == 0.0) {
//        userDistanceToManeuverLocation = NavigationConstants.MANEUVER_ZONE_RADIUS;
//      }
//
//      double lastKnownUserAbsoluteDistance = userDistanceToManeuverLocation;
//
//      // The objective here is to make sure the user is moving away from the maneuver location
//      // This helps on maneuvers where the difference between the exit and enter heading are similar
//      if (userAbsoluteDistance <= lastKnownUserAbsoluteDistance) {
//        userDistanceToManeuverLocation = userAbsoluteDistance;
//      }


  /**
   * Determine if the user is off route or not using the value set in
   * {@link NavigationConstants#MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE}. We first calculate the users next predicted
   * location one second from the current time.
   *
   * @param location The users current location.
   * @param routeLeg The route leg the user is currently on.
   * @return true if the user is found to be off route, otherwise false.
   * @since 0.1.0
   */
  boolean userIsOnRoute(Location location, RouteLeg routeLeg) {
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    // Find future location of user
    double metersInFrontOfUser = location.getSpeed() * options.getDeadReckoningTimeInterval();

    Position locationInFrontOfUser = TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );

    return RouteUtils.isOffRoute(locationInFrontOfUser, routeLeg,
      options.getMaximumDistanceOffRoute());
  }

  float snapUserBearing(Location userLocation, RouteProgress routeProgress, boolean snapToRoute) {

    LineString lineString = LineString.fromPolyline(routeProgress.getRoute().getGeometry(),
      com.mapbox.services.Constants.PRECISION_6);

    Position newCoordinate;
    if (snapToRoute) {
      newCoordinate = routeProgress.usersCurrentSnappedPosition();
    } else {
      newCoordinate = Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude());
    }

    double userDistanceBuffer = userLocation.getSpeed() * options.getDeadReckoningTimeInterval();

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
    double wrappedCurrentBearing = MathUtils.wrap(userLocation.getBearing(), -180, 180);

    double relativeAnglepointOne = MathUtils.wrap(wrappedPointOne - wrappedCurrentBearing, -180, 180);
    double relativeAnglepointTwo = MathUtils.wrap(wrappedPointTwo - wrappedCurrentBearing, -180, 180);

    double averageRelativeAngle = (relativeAnglepointOne + relativeAnglepointTwo) / 2;

    double absoluteBearing = MathUtils.wrap(wrappedCurrentBearing + averageRelativeAngle, 0, 360);

    if (MathUtils.differenceBetweenAngles(absoluteBearing, userLocation.getBearing())
      > options.getMaxManipulatedCourseAngle()) {
      return userLocation.getBearing();
    }

    return averageRelativeAngle <= options.getMaxTurnCompletionOffset() ? (float)
      absoluteBearing : userLocation.getBearing();
  }
}
