package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import java.util.List;

public class OffRouteDetector extends OffRoute {

  /**
   * Detects if the user is off route or not.
   *
   * @return true if the users off-route, else false.
   * @since 0.2.0
   */
  @Override
  public boolean isUserOffRoute(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    Position futurePosition = getFuturePosition(location, options);
    double radius = Math.max(options.maximumDistanceOffRoute(),
      location.getAccuracy() + options.userLocationSnapDistance());

    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    boolean isOffRoute = userTrueDistanceFromStep(futurePosition, currentStep) > radius;

    RouteLegProgress currentLegProgress = routeProgress.currentLegProgress();
    List<RouteLeg> routeLegs = routeProgress.directionsRoute().getLegs();

    // If the user is moving away from the maneuver location and they are close to the next step we can safely say they
    // have completed the maneuver. This is intended to be a fallback case when we do find that the users course matches
    // the exit bearing.
    boolean isCloseToUpcomingStep;

    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();

    if (upComingStep != null && isOffRoute) {

      if (upComingStep.getManeuver().getType().equals("arrive")) {
        int legIndex = routeProgress.legIndex();
        int upComingStepIndex = currentLegProgress.stepIndex() + 1;

        // If we're already on the last step
        if (upComingStepIndex >= routeLegs.get(legIndex).getSteps().size() - 1) {
          upComingStepIndex = 0;

          // If we're already on the last leg
          if (legIndex >= routeLegs.size() - 1) {
            // then if means we're arrived, no follow up step
            upComingStep = null;
          } else {
            legIndex++;
            upComingStep = routeLegs.get(legIndex).getSteps().get(upComingStepIndex);
          }
        }
      }

      double distance = userTrueDistanceFromStep(futurePosition, upComingStep);
      isCloseToUpcomingStep = distance < radius;
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
    String geometry = step.getGeometry();
    LineString lineString = LineString.fromPolyline(geometry, Constants.PRECISION_6);

    if (lineString.getCoordinates().size() == 1) {
      return TurfMeasurement.distance(
        Point.fromCoordinates(futurePosition),
        Point.fromCoordinates(lineString.getCoordinates().get(0)),
        TurfConstants.UNIT_METERS);
    } else {
      Feature feature = TurfMisc.pointOnLine(Point.fromCoordinates(futurePosition), lineString.getCoordinates());
      Point snappedPoint = (Point) feature.getGeometry();
      return TurfMeasurement.distance(
        Point.fromCoordinates(futurePosition),
        snappedPoint,
        TurfConstants.UNIT_METERS);
    }
  }
}
