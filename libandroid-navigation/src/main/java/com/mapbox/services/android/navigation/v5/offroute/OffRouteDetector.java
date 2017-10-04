package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

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
    Position futurePosition = getFuturePosition(location, options);
    double radius = Math.max(options.maximumDistanceOffRoute(),
      location.getAccuracy() + options.userLocationSnapDistance());

    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    boolean isOffRoute = userTrueDistanceFromStep(futurePosition, currentStep) > radius;

    // Check to see if the user is moving away from the maneuver. Here, we store an array of
    // distances. If the current distance is greater than the last distance, add it to the array. If
    // the array grows larger than x, reroute the user.
    double userDistanceToManeuver = TurfMeasurement.distance(
      routeProgress.currentLegProgress().currentStep().getManeuver().asPosition(),
      futurePosition, TurfConstants.UNIT_METERS
    );

    if (recentDistancesFromManeuverInMeters.size() >= 3) {
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

    // If the user is moving away from the maneuver location and they are close to the next step we
    // can safely say they have completed the maneuver. This is intended to be a fallback case when
    // we do find that the users course matches the exit bearing.
    boolean isCloseToUpcomingStep;

    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      isCloseToUpcomingStep = userTrueDistanceFromStep(futurePosition, upComingStep) < radius;
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
   * @return a {@link Position}
   * @since 0.2.0
   */
  private static Position getFuturePosition(Location location, MapboxNavigationOptions options) {
    // Find future location of user
    Position locationToPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    double metersInFrontOfUser = location.getSpeed() * options.deadReckoningTimeInterval();
    return TurfMeasurement.destination(
      locationToPosition, metersInFrontOfUser, location.getBearing(), TurfConstants.UNIT_METERS
    );
  }

  /**
   * Gets the distance from the users predicted {@link Position} to the
   * closest point on the given {@link LegStep}.
   *
   * @param futurePosition {@link Position} where the user is predicted to be
   * @param step           {@link LegStep} to calculate the closest point on the step to our predicted location
   * @return double in distance meters
   * @since 0.2.0
   */
  private double userTrueDistanceFromStep(Position futurePosition, LegStep step) {
    LineString lineString = LineString.fromPolyline(step.getGeometry(), Constants.PRECISION_6);
    Feature feature = TurfMisc.pointOnLine(Point.fromCoordinates(futurePosition), lineString.getCoordinates());

    Point snappedPoint = (Point) feature.getGeometry();

    return TurfMeasurement.distance(
      Point.fromCoordinates(futurePosition),
      snappedPoint,
      TurfConstants.UNIT_METERS);
  }
}
