package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

/**
 * Class is solely for calculating a new alert level using the latest location provided.
 *
 * @since 0.2.0
 */
class AlertLevelState {

  private RouteProgress previousRouteProgress;
  private MapboxNavigationOptions options;
  private Location location;
  private int stepIndex;
  private int legIndex;

  /**
   * Constructs a new AlertLevelState instance.
   *
   * @param location              the user location used to determine the alert level
   * @param previousRouteProgress the previous route state
   * @param stepIndex             the current directions step index
   * @param legIndex              the current directions leg index
   * @param options               a {@link MapboxNavigationOptions} object containing custom parameters
   * @since 0.2.0
   */
  AlertLevelState(Location location, RouteProgress previousRouteProgress, int stepIndex, int legIndex,
                  MapboxNavigationOptions options) {
    this.location = location;
    this.previousRouteProgress = previousRouteProgress;
    this.stepIndex = stepIndex;
    this.legIndex = legIndex;
    this.options = options;
  }

  /**
   * Returns updated alert level as an integer, this might be the same as the previous alert level or it might be new
   * depending on the users new location.
   *
   * @return an integer representing one of the alert levels found inside {@link NavigationConstants}
   * @since 0.2.0
   */
  int getNewAlertLevel() {
    double userSnapToStepDistanceFromManeuver = calculateSnappedDistanceToNextStep(location, previousRouteProgress);
    double durationRemainingOnStep = userSnapToStepDistanceFromManeuver / location.getSpeed();
    double stepDistance = previousRouteProgress.getCurrentLegProgress().getCurrentStep().getDistance();

    int alertLevel = isUserDeparting(previousRouteProgress.getAlertUserLevel(), userSnapToStepDistanceFromManeuver,
      options.getManeuverZoneRadius());

    if (userSnapToStepDistanceFromManeuver <= options.getManeuverZoneRadius()) {
      alertLevel = isUserArriving(alertLevel, previousRouteProgress);
      // Did the user reach the next steps maneuver?
      if (bearingMatchesManeuverFinalHeading(
        location, previousRouteProgress, options.getMaxTurnCompletionOffset())) {
        increaseIndex(previousRouteProgress);
        alertLevel = nextStepAlert(durationRemainingOnStep);
      }
      // If the users not in the maneuver zone, the alert level could potentially be medium or high.
    } else if (durationRemainingOnStep <= options.getHighAlertInterval()
      && stepDistance > getMinimumHighAlertDistance()) {
      alertLevel = NavigationConstants.HIGH_ALERT_LEVEL;
    } else if (durationRemainingOnStep <= options.getMediumAlertInterval()
      && stepDistance > getMinimumMediumAlertDistance()) {
      alertLevel = NavigationConstants.MEDIUM_ALERT_LEVEL;
    }
    return alertLevel;
  }

  /**
   * Returns the step index that might have been incremented while calculating the alert level.
   *
   * @return the current step index
   * @since 0.2.0
   */
  int getStepIndex() {
    return stepIndex;
  }

  /**
   * Returns the leg index that might have been incremented while calculating the alert level.
   *
   * @return the current leg index
   * @since 0.2.0
   */
  int getLegIndex() {
    return legIndex;
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
  private static int isUserDeparting(int alertLevel, double distance, double maneuverZoneRadius) {
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
  private static int isUserArriving(int alertLevel, RouteProgress routeProgress) {
    if (routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getType()
      .equals(com.mapbox.services.android.Constants.STEP_MANEUVER_TYPE_ARRIVE)) {
      return NavigationConstants.ARRIVE_ALERT_LEVEL;
    }
    return alertLevel;
  }

  /**
   * Determines what alert level should be provided when the user has just completed the previous maneuver and they
   * begin traversing along a new route step.
   *
   * @param secondsToEndOfNewStep used in determining if a {@link NavigationConstants#MEDIUM_ALERT_LEVEL} or
   *                              {@link NavigationConstants#LOW_ALERT_LEVEL} will occur.
   * @return the alert level
   * @since 0.2.0
   */
  private int nextStepAlert(double secondsToEndOfNewStep) {
    return secondsToEndOfNewStep <= NavigationConstants.MEDIUM_ALERT_INTERVAL ? NavigationConstants.MEDIUM_ALERT_LEVEL
      : NavigationConstants.LOW_ALERT_LEVEL;
  }

  /**
   * Provides the distance from the users snapped location to the next maneuver location.
   *
   * @param location      the users location
   * @param routeProgress used to get the steps geometry
   * @return distance in meters (by default)
   * @since 0.2.0
   */
  private double calculateSnappedDistanceToNextStep(Location location, RouteProgress routeProgress) {
    Position truePosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    String stepGeometry = routeProgress.getRoute().getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry();

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(stepGeometry, Constants.PRECISION_6);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(truePosition),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );

    double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

    // Prevents the distance to next step from increasing causing a previous alert level to occur again.
    if (distance > routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining()) {
      return routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    }
    return distance;
  }

  /**
   * Checks whether the user's bearing matches the next step's maneuver provided bearingAfter variable. This is one of
   * the criteria's required for the user location to be recognized as being on the next step or potentially arriving.
   *
   * @param userLocation  the location of the user
   * @param routeProgress used for getting route information
   * @return boolean true if the user location matches (using a tolerance) the final heading
   * @since 0.2.0
   */
  private static boolean bearingMatchesManeuverFinalHeading(Location userLocation, RouteProgress routeProgress,
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

  /**
   * When the user proceeds to a new step (or leg) the index needs to be increased. We determine the amount of legs
   * and steps and increase accordingly till the last step's reached.
   *
   * @param routeProgress used for getting the route index sizes
   * @since 0.2.0
   */
  private void increaseIndex(RouteProgress routeProgress) {
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (stepIndex >= routeProgress.getRoute().getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 1
      && legIndex < routeProgress.getRoute().getLegs().size()) {
      legIndex += 1;
      stepIndex = 0;
    } else {
      stepIndex += 1;
    }
  }

  private double getMinimumHighAlertDistance() {
    switch (options.getDirectionsProfile()) {
      case DirectionsCriteria.PROFILE_CYCLING:
        return options.getMinimumHighAlertDistanceCycling();
      case DirectionsCriteria.PROFILE_WALKING:
        return options.getMinimumHighAlertDistanceWalking();
      default:
        return options.getMinimumHighAlertDistanceDriving();
    }
  }

  private double getMinimumMediumAlertDistance() {
    switch (options.getDirectionsProfile()) {
      case DirectionsCriteria.PROFILE_CYCLING:
        return options.getMinimumMediumAlertDistanceCycling();
      case DirectionsCriteria.PROFILE_WALKING:
        return options.getMinimumMediumAlertDistanceWalking();
      default:
        return options.getMinimumMediumAlertDistanceDriving();
    }
  }


}
