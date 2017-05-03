package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public class RouteController {

  // Navigation Variables
  private CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners;
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private CopyOnWriteArrayList<OffRouteListener> offRouteListeners;
  private boolean previousUserOffRoute;
  private boolean userOffRoute;
  private boolean snapToRoute;
  private DirectionsRoute directionsRoute;
  private MapboxNavigationOptions options;

  private boolean departed;

  private int currentLegIndex;
  private int currentStepIndex;

  public RouteController(MapboxNavigationOptions options) {
    this.options = options;
    // By default we snap to route when possible.
    snapToRoute = true;
  }

  void setAlertLevelChangeListener(CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners) {
    this.alertLevelChangeListeners = alertLevelChangeListeners;
  }

  void setProgressChangeListener(CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners) {
    this.progressChangeListeners = progressChangeListeners;
  }

  void setOffRouteListener(CopyOnWriteArrayList<OffRouteListener> offRouteListeners) {
    this.offRouteListeners = offRouteListeners;
  }

  void setSnapToRoute(boolean snapToRoute) {
    this.snapToRoute = snapToRoute;
  }

  public void updateLocation(Location userLocation, DirectionsRoute directionsRoute, RouteProgress previousRouteProgress) {
    this.directionsRoute = directionsRoute;

    if (userLocation == null || directionsRoute == null) {
      return;
    }

    Timber.d("Previous route alert level: %d", previousRouteProgress.getAlertUserLevel());

    // With a new location update, we create a new RouteProgress object.
    RouteProgress routeProgress = monitorStepProgress(userLocation, previousRouteProgress);

    userOffRoute = userIsOnRoute(userLocation, routeProgress.getCurrentLeg());

    if (snapToRoute && !userOffRoute) {
      // Pass in the snapped location with all the other location data remaining intact for their use.
      userLocation.setLatitude(routeProgress.usersCurrentSnappedPosition().getLatitude());
      userLocation.setLongitude(routeProgress.usersCurrentSnappedPosition().getLongitude());

      userLocation.setBearing(snapUserBearing(userLocation, routeProgress));
    }

    // Only report user off route once.
    if (userOffRoute && (userOffRoute != previousUserOffRoute)) {
      for (OffRouteListener offRouteListener : offRouteListeners) {
        offRouteListener.userOffRoute(userLocation);
      }
      previousUserOffRoute = userOffRoute;
    }

    if (previousRouteProgress.getAlertUserLevel() != routeProgress.getAlertUserLevel()) {

      for (AlertLevelChangeListener alertLevelChangeListener : alertLevelChangeListeners) {
        alertLevelChangeListener.onAlertLevelChange(routeProgress.getAlertUserLevel(), routeProgress);
      }
    }

    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(userLocation, routeProgress);
    }
  }









  private boolean userDidDepart(RouteProgress routeProgress) {

    return routeProgress.getAlertUserLevel() == NavigationConstants.NONE_ALERT_LEVEL
      && routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining()
      > options.getManeuverZoneRadius();
  }

  private boolean courseMatchesManeuverFinalHeading(Location userLocation, RouteProgress routeProgress) {
    if (routeProgress.getCurrentLegProgress().getUpComingStep() != null) {
      return false;
    }
    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    double finalHeading = routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getBearingAfter();
    double finalHeadingNormalized = MathUtils.wrap(finalHeading, 0, 360);
    double userHeadingNormalized = MathUtils.wrap(userLocation.getBearing(), 0, 360);
    return MathUtils.differenceBetweenAngles(finalHeadingNormalized, userHeadingNormalized)
      <= options.getMaxTurnCompletionOffset();

  }

  private void increaseIndex(RouteProgress routeProgress) {
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (currentStepIndex >= directionsRoute.getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 1
      && currentLegIndex < directionsRoute.getLegs().size()) {
      currentLegIndex += 1;
      currentStepIndex = 0;
    } else {
      currentStepIndex += 1;
    }
  }

  private boolean isAlertLevelLow(Location userLocation, RouteProgress routeProgress) {
    double secondsToEndOfNewStep = RouteUtils.getDistanceToNextStep(
      Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude()),
      routeProgress.getRoute().getLegs().get(currentLegIndex),
      currentStepIndex
    ) / userLocation.getSpeed();
    return secondsToEndOfNewStep > options.getMediumAlertInterval();
  }

  private double calculateSnappedDistanceToNextStep(Location location, RouteProgress routeProgress) {
    // TODO prevent this distance increasing if on same step
    Position truePosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    return RouteUtils.getDistanceToNextStep(
      truePosition,
      routeProgress.getRoute().getLegs().get(currentLegIndex),
      currentStepIndex
    );
  }





  private RouteProgress monitorStepProgress(Location userLocation, RouteProgress previousRouteProgress) {
    int alertLevel = previousRouteProgress.getAlertUserLevel();

    if (!departed) {
      departed = true;
      if (userDidDepart(previousRouteProgress)) {
        alertLevel = NavigationConstants.DEPART_ALERT_LEVEL;
      } else {
        alertLevel = NavigationConstants.HIGH_ALERT_LEVEL;
      }
    }

    double distanceToNextStep = calculateSnappedDistanceToNextStep(userLocation, previousRouteProgress);
    double secondsToEndOfStep = distanceToNextStep / userLocation.getSpeed();

    // Checks if the user has completed the current step
    if (distanceToNextStep <= options.getManeuverZoneRadius()) {
      if (previousRouteProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getType().equals("arrive")) {
        alertLevel = NavigationConstants.ARRIVE_ALERT_LEVEL;
      } else if (courseMatchesManeuverFinalHeading(userLocation, previousRouteProgress)) {
        increaseIndex(previousRouteProgress);
        alertLevel = isAlertLevelLow(userLocation, previousRouteProgress) ? NavigationConstants.LOW_ALERT_LEVEL : NavigationConstants.MEDIUM_ALERT_LEVEL;
      }
    }

    if (secondsToEndOfStep <= options.getHighAlertInterval() && distanceToNextStep > options.getMinimumHighAlertDistance()) {
      alertLevel = NavigationConstants.HIGH_ALERT_LEVEL;
    } else if (secondsToEndOfStep <= options.getMediumAlertInterval() && distanceToNextStep > options.getMinimumMediumAlertDistance()) {
      alertLevel = NavigationConstants.MEDIUM_ALERT_LEVEL;
    }

    return new RouteProgress(directionsRoute, userLocation, currentLegIndex, currentStepIndex, alertLevel);
  }
























































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
  private boolean userIsOnRoute(Location location, RouteLeg routeLeg) {
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    // Find future location of user
    double metersInFrontOfUser = location.getSpeed() * options.getDeadReckoningTimeInterval();

    Position locationInFrontOfUser = TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );

    return RouteUtils.isOffRoute(locationInFrontOfUser, routeLeg,
      options.getMaximumDistanceOffRoute());
  }

  private float snapUserBearing(Location userLocation, RouteProgress routeProgress) {

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
