package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

import com.mapbox.services.Experimental;
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

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

/**
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 */
@Experimental
class LocationUpdatedThread extends HandlerThread {

  private static final String TAG = "LocationUpdatedThread";
  private static final int MESSAGE_LOCATION_UPDATED = 0;

  // Handlers
  private Handler requestHandler;
  private Handler responseHandler;

  // Navigation Variables
  private CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners;
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private CopyOnWriteArrayList<OffRouteListener> offRouteListeners;
  private boolean previousUserOffRoute;
  private double userDistanceToManeuverLocation;
  private boolean userOffRoute;
  private boolean snapToRoute;
  private DirectionsRoute directionsRoute;
  private Location location;

  LocationUpdatedThread(Handler responseHandler) {
    super(TAG);
    this.responseHandler = responseHandler;
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

  @Override
  protected void onLooperPrepared() {
    requestHandler = new RequestHandler(this);
  }

  void updateLocation(DirectionsRoute directionsRoute, RouteProgress previousRouteProgress, Location location) {
    Timber.d("updateLocation called");
    this.location = location;
    this.directionsRoute = directionsRoute;

    requestHandler.obtainMessage(MESSAGE_LOCATION_UPDATED, previousRouteProgress).sendToTarget();
  }

  private void handleRequest(final DirectionsRoute directionsRoute, final RouteProgress previousRouteProgress,
                             final Location location) {
    if (location == null || directionsRoute == null) {
      return;
    }

    Timber.d("Previous route alert level: %d", previousRouteProgress.getAlertUserLevel());

    // With a new location update, we create a new RouteProgress object.
    final RouteProgress routeProgress = monitorStepProgress(previousRouteProgress, location);

    userOffRoute = userIsOnRoute(location, routeProgress.getCurrentLeg());

    if (snapToRoute && !userOffRoute) {
      // Pass in the snapped location with all the other location data remaining intact for their use.
      location.setLatitude(routeProgress.usersCurrentSnappedPosition().getLatitude());
      location.setLongitude(routeProgress.usersCurrentSnappedPosition().getLongitude());

      location.setBearing(snapUserBearing(routeProgress));
    }

    this.location = location;

    // Post back to the UI Thread.
    responseHandler.post(new Runnable() {
      public void run() {

        // Only report user off route once.
        if (userOffRoute && (userOffRoute != previousUserOffRoute)) {
          for (OffRouteListener offRouteListener : offRouteListeners) {
            offRouteListener.userOffRoute(location);
          }
          previousUserOffRoute = userOffRoute;
        }

        if (previousRouteProgress.getAlertUserLevel() != routeProgress.getAlertUserLevel()) {

          for (AlertLevelChangeListener alertLevelChangeListener : alertLevelChangeListeners) {
            alertLevelChangeListener.onAlertLevelChange(routeProgress.getAlertUserLevel(), routeProgress);
          }
        }

        for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
          progressChangeListener.onProgressChange(LocationUpdatedThread.this.location, routeProgress);
        }
      }
    });
  }

  private RouteProgress monitorStepProgress(@NonNull RouteProgress routeProgress, Location location) {
    int currentStepIndex = routeProgress.getCurrentLegProgress().getStepIndex();
    int currentLegIndex = routeProgress.getLegIndex();

    // Force an announcement when the user begins a route
    int alertLevel = routeProgress.getAlertUserLevel() == Constants.NONE_ALERT_LEVEL
      ? Constants.DEPART_ALERT_LEVEL : routeProgress.getAlertUserLevel();

    Position truePosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    double userSnapToStepDistanceFromManeuver = RouteUtils.getDistanceToNextStep(
      truePosition,
      routeProgress.getCurrentLeg(),
      routeProgress.getCurrentLegProgress().getStepIndex()
    );

    double secondsToEndOfStep = userSnapToStepDistanceFromManeuver / location.getSpeed();
    boolean courseMatchesManeuverFinalHeading = false;

    // TODO set to eventually adjust for different direction profiles.
    int minimumDistanceForHighAlert = Constants.MINIMUM_DISTANCE_FOR_HIGH_ALERT;
    int minimumDistanceForMediumAlert = Constants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT;

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    if (routeProgress.getCurrentLegProgress().getUpComingStep() != null) {
      double finalHeading = routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getBearingAfter();
      double finalHeadingNormalized = MathUtils.wrap(finalHeading, 0, 360);
      double userHeadingNormalized = MathUtils.wrap(location.getBearing(), 0, 360);
      courseMatchesManeuverFinalHeading = MathUtils.differenceBetweenAngles(
        finalHeadingNormalized, userHeadingNormalized
      ) <= Constants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION;
    }

    // When departing, userSnapToStepDistanceFromManeuver is most often less than RouteControllerManeuverZoneRadius
    // since the user will most often be at the beginning of the route, in the maneuver zone
    if (alertLevel == Constants.DEPART_ALERT_LEVEL && userSnapToStepDistanceFromManeuver
      <= Constants.MANEUVER_ZONE_RADIUS) {
      // If the user is close to the maneuver location, don't give a departure instruction, instead, give a high alert.
      if (secondsToEndOfStep <= Constants.HIGH_ALERT_INTERVAL) {
        alertLevel = Constants.HIGH_ALERT_LEVEL;
      }

    } else if (userSnapToStepDistanceFromManeuver <= Constants.MANEUVER_ZONE_RADIUS) {

      // Use the currentStep if there is not a next step, this occurs when arriving.
      if (routeProgress.getCurrentLegProgress().getUpComingStep() != null) {

        double userAbsoluteDistance = TurfMeasurement.distance(
          truePosition, routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().asPosition(),
          TurfConstants.UNIT_METERS
        );

        // userAbsoluteDistanceToManeuverLocation is set to nil by default
        // If it's set to nil, we know the user has never entered the maneuver radius
        if (userDistanceToManeuverLocation == 0.0) {
          userDistanceToManeuverLocation = Constants.MANEUVER_ZONE_RADIUS;
        }

        double lastKnownUserAbsoluteDistance = userDistanceToManeuverLocation;

        // The objective here is to make sure the user is moving away from the maneuver location
        // This helps on maneuvers where the difference between the exit and enter heading are similar
        if (userAbsoluteDistance <= lastKnownUserAbsoluteDistance) {
          userDistanceToManeuverLocation = userAbsoluteDistance;
        }

        if (routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getType().equals("arrive")) {
          alertLevel = Constants.ARRIVE_ALERT_LEVEL;
        } else if (courseMatchesManeuverFinalHeading) {

          // Check if we are in the last step in the current routeLeg and iterate it if needed.
          if (currentStepIndex >= directionsRoute.getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 1
            && currentLegIndex < directionsRoute.getLegs().size()) {
            currentLegIndex += 1;
            currentStepIndex = 0;
          } else {
            currentStepIndex += 1;
          }
          userSnapToStepDistanceFromManeuver = RouteUtils.getDistanceToNextStep(
            Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
            routeProgress.getCurrentLeg(),
            routeProgress.getCurrentLegProgress().getStepIndex()
          );
          secondsToEndOfStep = userSnapToStepDistanceFromManeuver / location.getSpeed();
          alertLevel = secondsToEndOfStep <= Constants.MEDIUM_ALERT_INTERVAL
            ? Constants.MEDIUM_ALERT_LEVEL : Constants.LOW_ALERT_LEVEL;
        }
      } else if (secondsToEndOfStep <= Constants.HIGH_ALERT_INTERVAL
        && routeProgress.getCurrentLegProgress().getCurrentStep().getDistance() > minimumDistanceForHighAlert) {
        alertLevel = Constants.HIGH_ALERT_LEVEL;
        // Don't alert if the route segment is shorter than X however, if it's the beginning of the route There needs to
        // be an alert
      } else if (secondsToEndOfStep <= Constants.MEDIUM_ALERT_INTERVAL
        && routeProgress.getCurrentLegProgress().getCurrentStep().getDistance() > minimumDistanceForMediumAlert) {
        alertLevel = Constants.MEDIUM_ALERT_LEVEL;
      }
    }

    Position snappedPosition = RouteUtils.getSnapToRoute(
      Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
      directionsRoute.getLegs().get(routeProgress.getLegIndex()),
      routeProgress.getCurrentLegProgress().getStepIndex()
    );

    Timber.d("New alertLevel: %d", alertLevel);

    // Create an updated RouteProgress object to return
    return new RouteProgress(directionsRoute, snappedPosition, currentLegIndex, currentStepIndex, alertLevel);
  }

  /**
   * Determine if the user is off route or not using the value set in
   * {@link Constants#MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE}. We first calculate the users next predicted location one
   * second from the current time.
   *
   * @param location The users current location.
   * @param routeLeg The route leg the user is currently on.
   * @return true if the user is found to be off route, otherwise false.
   * @since 2.1.0
   */
  private boolean userIsOnRoute(Location location, RouteLeg routeLeg) {
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    // Find future location of user
    double metersInFrontOfUser = location.getSpeed() * Constants.DEAD_RECKONING_TIME_INTERVAL;

    Position locationInFrontOfUser = TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );

    return RouteUtils.isOffRoute(locationInFrontOfUser, routeLeg, Constants.MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE);
  }

  private float snapUserBearing(RouteProgress routeProgress) {

    LineString lineString = LineString.fromPolyline(routeProgress.getRoute().getGeometry(),
      com.mapbox.services.Constants.PRECISION_6);

    Position newCoordinate;
    if (snapToRoute) {
      newCoordinate = routeProgress.usersCurrentSnappedPosition();
    } else {
      newCoordinate = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    }

    double userDistanceBuffer = location.getSpeed() * Constants.DEAD_RECKONING_TIME_INTERVAL;

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

    if (MathUtils.differenceBetweenAngles(absoluteBearing, location.getBearing()) > Constants.MAX_MANIPULATED_COURSE_ANGLE) {
      return location.getBearing();
    }

    return averageRelativeAngle <= Constants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION ? (float) absoluteBearing
      : location.getBearing();
  }

  /**
   * Prevents having a reference to our NavigationService class. Allows for garbage collected and reduces chance of
   * memory leak.
   */
  private static class RequestHandler extends Handler {
    //Using a weak reference means you won't prevent garbage collection
    private final WeakReference<LocationUpdatedThread> locationUpdatedHandler;

    RequestHandler(LocationUpdatedThread locationUpdatedThread) {
      locationUpdatedHandler = new WeakReference<>(locationUpdatedThread);
    }

    @Override
    public void handleMessage(Message msg) {
      LocationUpdatedThread locationUpdatedThread = this.locationUpdatedHandler.get();
      if (locationUpdatedThread != null) {
        if (msg.what == MESSAGE_LOCATION_UPDATED) {
          RouteProgress previousRouteProgress = (RouteProgress) msg.obj;
          Timber.d("Received request to calculate new location information");
          locationUpdatedThread.handleRequest(locationUpdatedThread.directionsRoute, previousRouteProgress,
            locationUpdatedThread.location);
        }
      }
    }
  }
}
