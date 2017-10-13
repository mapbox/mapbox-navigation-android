package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MINIMUM_BACKUP_DISTANCE_FOR_OFF_ROUTE;

public class OffRouteDetector extends OffRoute {

  /**
   * Detects if the user is off route or not.
   *
   * @return true if the users off-route, else false.
   * @since 0.2.0
   */
  @Override
  public boolean isUserOffRoute(Location location, RouteProgress routeProgress,
                                MapboxNavigationOptions options,
                                RingBuffer<Integer> recentDistancesFromManeuverInMeters) {
    Point futurePoint = getFuturePosition(location, options);
    double radius = Math.max(options.maximumDistanceOffRoute(),
      location.getAccuracy() + options.userLocationSnapDistance());

    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    boolean isOffRoute = userTrueDistanceFromStep(futurePoint, currentStep) > radius;

    // Check to see if the user is moving away from the maneuver. Here, we store an array of
    // distances. If the current distance is greater than the last distance, add it to the array. If
    // the array grows larger than x, reroute the user.
    if (movingAwayFromManeuver(routeProgress, recentDistancesFromManeuverInMeters,
      futurePoint)) {
      return true;
    }

    // If the user is moving away from the maneuver location and they are close to the next step we
    // can safely say they have completed the maneuver. This is intended to be a fallback case when
    // we do find that the users course matches the exit bearing.
    boolean isCloseToUpcomingStep;

    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      isCloseToUpcomingStep = userTrueDistanceFromStep(futurePoint, upComingStep) < radius;
      if (isOffRoute && isCloseToUpcomingStep) {
        // TODO increment step index
        return false;
      }
    }
    return isOffRoute;
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

  /**
   * Gets the distance from the users predicted {@link Point} to the
   * closest point on the given {@link LegStep}.
   *
   * @param futurePoint {@link Point} where the user is predicted to be
   * @param step           {@link LegStep} to calculate the closest point on the step to our predicted location
   * @return double in distance meters
   * @since 0.2.0
   */
  private static double userTrueDistanceFromStep(Point futurePoint, LegStep step) {
    LineString lineString = LineString.fromPolyline(step.geometry(), Constants.PRECISION_6);
    Feature feature = TurfMisc.pointOnLine(futurePoint, lineString.coordinates());

    Point snappedPoint = (Point) feature.geometry();

    return TurfMeasurement.distance(
      futurePoint,
      snappedPoint,
      TurfConstants.UNIT_METERS);
  }
}
