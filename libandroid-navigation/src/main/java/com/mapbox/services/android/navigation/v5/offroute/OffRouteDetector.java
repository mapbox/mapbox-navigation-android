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
   * Method in charge of running a series of test based on the device current location
   * and the user progress along the route.
   * <p>
   * Test #1:
   * Valid or invalid off-route.  An off-route check can only continue if the device has received
   * at least 1 location update (for comparison) and the user has traveled passed
   * the {@link MapboxNavigationOptions#minimumDistanceBeforeRerouting()} checked against the last re-route location.
   * <p>
   * Test #2:
   * Distance from the step. This test is checked against the max of the dynamic rerouting tolerance or the
   * accuracy based tolerance. If this test passes, this method then also checks if there have been &gt;= 3
   * location updates moving away from the maneuver point. If false, this method will return false early.
   * <p>
   * Test #3:
   * Checks if the user is close the upcoming step.  At this point, the user is considered off-route.
   * But, if the location update is within the {@link MapboxNavigationOptions#maneuverZoneRadius()} of the
   * upcoming step, this method will return false as well as send fire {@link OffRouteCallback#onShouldIncreaseIndex()}
   * to let the <tt>NavigationEngine</tt> know that the
   * step index should be increased on the next location update.
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
    double offRouteRadius = createOffRouteRadius(location, routeProgress, options, currentPoint);

    // Off route if this distance is greater than our offRouteRadius
    boolean isOffRoute = distanceFromCurrentStep > offRouteRadius;

    // If not offRoute at this point, do not continue with remaining logic
    if (!isOffRoute) {
      // Even though the current point is not considered off-route, check to see if the user is
      // moving away from the maneuver.
      return isMovingAwayFromManeuver(location, routeProgress, distancesAwayFromManeuver, currentPoint);
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

  private double createOffRouteRadius(Location location, RouteProgress routeProgress,
                                      MapboxNavigationOptions options, Point currentPoint) {
    double dynamicTolerance = dynamicRerouteDistanceTolerance(currentPoint, routeProgress);
    double accuracyTolerance = location.getAccuracy() * options.deadReckoningTimeInterval();
    return Math.max(dynamicTolerance, accuracyTolerance);
  }

  private boolean isMovingAwayFromManeuver(Location location, RouteProgress routeProgress,
                                           RingBuffer<Integer> distancesAwayFromManeuver, Point currentPoint) {
    if (movingAwayFromManeuver(routeProgress, distancesAwayFromManeuver, currentPoint)) {
      updateLastReroutePoint(location);
      return true;
    }
    return false;
  }

  /**
   * If the upcoming step is not null, detect if the current point
   * is within the maneuver radius.
   * <p>
   * If it is, fire {@link OffRouteCallback#onShouldIncreaseIndex()} to increase the step
   * index in the <tt>NavigationEngine</tt> and return true.
   *
   * @param options      for maneuver zone radius
   * @param callback     to increase step index
   * @param currentPoint for distance from upcoming step
   * @param upComingStep for distance from current point
   * @return true if close to upcoming step, false if not
   */
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

  /**
   * Checks to see if the current point is moving away from the maneuver.
   * <p>
   * If the current point is farther away from the maneuver than the last point in the
   * stack, add it to the stack.
   * <p>
   * If the stack if >= 3 distances, return true to fire an off-route event as it
   * can be considered that the user is no longer going in the right direction.
   *
   * @param routeProgress             for the upcoming step maneuver
   * @param distancesAwayFromManeuver current stack of distances away
   * @param currentPoint              to determine if moving away or not
   * @return true if moving away from maneuver, false if not
   */
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

    boolean hasDistances = !distancesAwayFromManeuver.isEmpty();
    boolean validOffRouteDistanceTraveled = hasDistances && distancesAwayFromManeuver.peekLast()
      - distancesAwayFromManeuver.peekFirst() < MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE;
    boolean exceedsManeuverDistancesThreshold = validOffRouteDistanceTraveled
      && distancesAwayFromManeuver.size() >= 3;

    if (exceedsManeuverDistancesThreshold) {
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
