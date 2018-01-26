package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.ToleranceUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE;
import static com.mapbox.services.android.navigation.v5.utils.MeasurementUtils.userTrueDistanceFromStep;

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
                                RingBuffer<Integer> recentDistancesFromManeuverInMeters) {

    Timber.d("NAV-DEBUG xxxxx xxxxx xxxxx Detecting Off Route xxxxx xxxxx xxxxx");

    if (!validOffRoute(location, options)) {
      Timber.d("NAV-DEBUG xx invalid off-route");
      return false;
    }

    Point currentUserPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    double dynamicTolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(currentUserPoint, routeProgress);
    double accuracyTolerance = location.getSpeed() * options.deadReckoningTimeInterval();
    double offRouteRadius = Math.max(dynamicTolerance, accuracyTolerance);
    Timber.d("NAV-DEBUG xx Radius: %s", offRouteRadius);

    // Get interpolated point based on our current speed
    Point futurePoint = getFuturePosition(location, options);

    // Get distance from the current step to future point
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double distanceFromStep = userTrueDistanceFromStep(futurePoint, currentStep);

    // Off route if this distance is greater than our offRouteRadius
    boolean isOffRoute = distanceFromStep > offRouteRadius;

    Timber.d("NAV-DEBUG xx Distance from step: %s", distanceFromStep);

    // If not offRoute at this point, don't continue with remaining logic
    if (!isOffRoute) {
      Timber.d("NAV-DEBUG xx Off route: false -- returning false...");
      return false;
    } else {
      Timber.d("NAV-DEBUG xx Off route: true -- logic continues...");
    }

//    // Check to see if the user is moving away from the maneuver. Here, we store an array of
//    // distances. If the current distance is greater than the last distance, add it to the array. If
//    // the array grows larger than x, reroute the user.
//    if (movingAwayFromManeuver(routeProgress, recentDistancesFromManeuverInMeters, futurePoint)) {
//      updateLastReroutePoint(location);
//      return true;
//    }


    // If the user is moving away from the maneuver location and they are close to the next step we
    // can safely say they have completed the maneuver. This is intended to be a fallback case when
    // we do find that the users course matches the exit bearing.
    boolean isCloseToUpcomingStep;

    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      double distanceFromUpcomingStep = userTrueDistanceFromStep(currentUserPoint, upComingStep);
      Timber.d("NAV-DEBUG xx Distance from upComingStep: %s", distanceFromStep);
      double maneuverZoneRadius = options.maneuverZoneRadius();
      Timber.d("NAV-DEBUG xx Maneuver zone radius: %s", maneuverZoneRadius);
      isCloseToUpcomingStep = distanceFromUpcomingStep < maneuverZoneRadius;
      if (isCloseToUpcomingStep) {
        // TODO increment step index
        // TODO this needs to happen or the movement will just stop
        Timber.d("NAV-DEBUG xx isCloseToUpcomingStep: %s", true);
        return false;
      }
    }

    // All checks have run, return true
    Timber.d("NAV-DEBUG xxxxx xxxxx xxxxx Off route: TRUE xxxxx xxxxx xxxxx xxxxx");
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

  /**
   * uses dead reckoning to find the users future location.
   *
   * @return a {@link Point}
   * @since 0.2.0
   */
  private static Point getFuturePosition(Location location, MapboxNavigationOptions options) {
    // Find future location of user
    Point locationToPosition = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    double metersInFrontOfUser = location.getSpeed() * options.deadReckoningTimeInterval();
    return TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );
  }

  private static boolean movingAwayFromManeuver(RouteProgress routeProgress,
                                                RingBuffer<Integer> recentDistancesFromManeuverInMeters,
                                                Point futurePosition) {

    if (routeProgress.currentLegProgress().upComingStep() == null) {
      return false;
    }

    double userDistanceToManeuver = TurfMeasurement.distance(
      routeProgress.currentLegProgress().upComingStep().maneuver().location(),
      futurePosition, TurfConstants.UNIT_METERS
    );

    if (!recentDistancesFromManeuverInMeters.isEmpty()
      && recentDistancesFromManeuverInMeters.peekLast()
      - recentDistancesFromManeuverInMeters.peekFirst() < MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE
      && recentDistancesFromManeuverInMeters.size() >= 3) {
      // User's moving away from maneuver position, thus offRoute.
      return true;
    }
    if (recentDistancesFromManeuverInMeters.isEmpty()) {
      recentDistancesFromManeuverInMeters.push((int) userDistanceToManeuver);
    } else if (userDistanceToManeuver > recentDistancesFromManeuverInMeters.peek()) {
      recentDistancesFromManeuverInMeters.push((int) userDistanceToManeuver);
    } else {
      // If we get a descending distance, reset the counter
      recentDistancesFromManeuverInMeters.clear();
    }
    return false;
  }

  private void updateLastReroutePoint(Location location) {
    lastReroutePoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
  }
}
