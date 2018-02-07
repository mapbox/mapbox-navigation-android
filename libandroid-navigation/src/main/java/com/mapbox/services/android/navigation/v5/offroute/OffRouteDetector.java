package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE;
import static com.mapbox.services.android.navigation.v5.utils.MeasurementUtils.userTrueDistanceFromStep;
import static com.mapbox.services.android.navigation.v5.utils.ToleranceUtils.dynamicRerouteDistanceTolerance;

public class OffRouteDetector extends OffRoute {

  private Point lastReroutePoint;

  /**
   * Detects if the user is off route or not.
   *
   * @return true if the users off-route, else false.
   * @since 0.2.0
   */
  @Override
  public boolean isUserOffRoute(Location location, RouteProgress routeProgress, MapboxNavigationOptions options,
                                RingBuffer<Integer> distancesAwayFromManeuver, OffRouteCallback callback) {

    if (!validOffRoute(location, options)) {
      return false;
    }

    Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Get distance from the current step to future point
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double distanceFromCurrentStep = userTrueDistanceFromStep(currentPoint, currentStep);

    // Create off-route radius from the max of our dynamic or accuracy based tolerances
    double dynamicTolerance = dynamicRerouteDistanceTolerance(currentPoint, routeProgress);
    double accuracyTolerance = location.getAccuracy() * options.deadReckoningTimeInterval();
    double offRouteRadius = Math.max(dynamicTolerance, accuracyTolerance);

    // Off route if this distance is greater than our offRouteRadius
    boolean isOffRoute = distanceFromCurrentStep > offRouteRadius;

    // If not offRoute at this point, do not continue with remaining logic
    if (!isOffRoute) {
      // Even though the current point is not considered off-route, check to see if the user is
      // moving away from the maneuver.
      if (movingAwayFromManeuver(routeProgress, distancesAwayFromManeuver, currentPoint)) {
        updateLastReroutePoint(location);
        return true;
      }
      return false;
    }

    // If the user is considered off-route at this point, but they are close to the upcoming step,
    // do not send an off-route event and increment the step index to the upcoming step
    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    if (closeToUpcomingStep(options, callback, currentPoint, upComingStep)) {
      return false;
    }

    // All checks have run, return true
    updateLastReroutePoint(location);
    return true;
  }

  /**
   * Method to check if the user has passed either the set (in {@link MapboxNavigationOptions})
   * minimum amount of seconds or minimum amount of meters since the last reroute.
   * <p>
   * If the user is above both thresholds, then the off-route can proceed.  Otherwise, ignore.
   *
   * @param location current location from engine
   * @param options  for second (default 3) / distance (default 50m) minimums
   * @return true if valid, false if not
   */
  private boolean validOffRoute(Location location, MapboxNavigationOptions options) {
    // Check if minimum amount of distance has been passed since last reroute
    Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    double distanceFromLastReroute = 0d;
    if (lastReroutePoint != null) {
      distanceFromLastReroute = TurfMeasurement.distance(lastReroutePoint,
        currentPoint, TurfConstants.UNIT_METERS);
    } else {
      // If null, this is our first update - set the last reroute point to the given location
      updateLastReroutePoint(location);
    }
    return distanceFromLastReroute > options.minimumDistanceBeforeRerouting();
  }

  private static boolean closeToUpcomingStep(MapboxNavigationOptions options, OffRouteCallback callback,
                                      Point currentPoint, LegStep upComingStep) {
    boolean isCloseToUpcomingStep;
    if (upComingStep != null) {
      double distanceFromUpcomingStep = userTrueDistanceFromStep(currentPoint, upComingStep);
      double maneuverZoneRadius = options.maneuverZoneRadius();
      isCloseToUpcomingStep = distanceFromUpcomingStep < maneuverZoneRadius;
      if (isCloseToUpcomingStep) {
        // Callback to the NavigationEngine to increase the step index
        callback.onShouldIncreaseIndex();
        return true;
      }
    }
    return false;
  }

  private static boolean movingAwayFromManeuver(RouteProgress routeProgress,
                                                RingBuffer<Integer> distancesAwayFromManeuver,
                                                Point currentPoint) {

    if (routeProgress.currentLegProgress().upComingStep() == null) {
      return false;
    }

    double userDistanceToManeuver = TurfMeasurement.distance(
      routeProgress.currentLegProgress().upComingStep().maneuver().location(),
      currentPoint, TurfConstants.UNIT_METERS
    );

    if (!distancesAwayFromManeuver.isEmpty()
      && distancesAwayFromManeuver.peekLast()
      - distancesAwayFromManeuver.peekFirst() < MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE
      && distancesAwayFromManeuver.size() >= 3) {
      // User's moving away from maneuver position, thus offRoute.
      return true;
    }
    if (distancesAwayFromManeuver.isEmpty()) {
      distancesAwayFromManeuver.push((int) userDistanceToManeuver);
    } else if (userDistanceToManeuver > distancesAwayFromManeuver.peek()) {
      distancesAwayFromManeuver.push((int) userDistanceToManeuver);
    } else {
      // If we get a descending distance, reset the counter
      distancesAwayFromManeuver.clear();
    }
    return false;
  }

  private void updateLastReroutePoint(Location location) {
    lastReroutePoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
  }
}
