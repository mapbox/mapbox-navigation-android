package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

import static com.mapbox.services.Constants.PRECISION_6;

class NavigationHelper {

  // Always get the closest position on the route to the actual
  // raw location so that can accurately calculate values.
  static Position userSnappedToRoutePosition(Location location, int legIndex, int stepIndex,
                                             DirectionsRoute route) {
    Point locationToPoint = Point.fromCoordinates(
      new double[] {location.getLongitude(), location.getLatitude()}
    );

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(route.getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry(),
      PRECISION_6);

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
    return ((Point) feature.getGeometry()).getCoordinates();
  }

  static double getStepDistanceRemaining(Position snappedPosition, int legIndex,
                                         int stepIndex, DirectionsRoute directionsRoute) {
    List<LegStep> steps = directionsRoute.getLegs().get(legIndex).getSteps();
    Position nextManeuverPosition = getNextManeuverPosition(stepIndex, steps);
    // If the users snapped position equals the next maneuver position, the distance remaining is zero.
    if (snappedPosition.equals(nextManeuverPosition)) {
      return 0;
    }
    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(snappedPosition),
      Point.fromCoordinates(nextManeuverPosition),
      LineString.fromPolyline(steps.get(stepIndex).getGeometry(), PRECISION_6)
    );
    return TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
  }

  static double getLegDistanceRemaining(double stepDistanceRemaining, int legIndex,
                                        int stepIndex, DirectionsRoute directionsRoute) {
    List<LegStep> steps = directionsRoute.getLegs().get(legIndex).getSteps();
    if ((steps.size() < stepIndex + 1)) {
      return stepDistanceRemaining;
    }
    for (int i = stepIndex + 1; i < steps.size(); i++) {
      stepDistanceRemaining += steps.get(i).getDistance();
    }
    return stepDistanceRemaining;
  }

  static double getRouteDistanceRemaining(double legDistanceRemaining, int legIndex,
                                          DirectionsRoute directionsRoute) {
    if (directionsRoute.getLegs().size() < 2) {
      return legDistanceRemaining;
    }

    for (int i = legIndex + 1; i < directionsRoute.getLegs().size(); i++) {
      legDistanceRemaining += directionsRoute.getLegs().get(i).getDistance();
    }
    return legDistanceRemaining;
  }

  static Position getNextManeuverPosition(int stepIndex, List<LegStep> steps) {
    // If there is an upcoming step, use it's maneuver as the position.
    if (steps.size() > (stepIndex + 1)) {
      return steps.get(stepIndex + 1).getManeuver().asPosition();
    }
    // TODO does the last step in directions response always a single point representing the maneuver?
    // Decode the geometry
    List<Position> coords
      = PolylineUtils.decode(steps.get(stepIndex).getGeometry(), Constants.PRECISION_6);
    return coords.size() > 1 ? coords.get(coords.size() - 1) : coords.get(coords.size());
  }

  static int[] increaseIndex(RouteProgress routeProgress, int legIndex, int stepIndex) {
    int[] indexes = new int[2];
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (stepIndex >= routeProgress.directionsRoute().getLegs().get(routeProgress.legIndex()).getSteps().size() - 2
      && legIndex < routeProgress.directionsRoute().getLegs().size() - 1) {
      indexes[0] += 1;
      indexes[1] = 0;
    } else {
      indexes[1] += 1;
    }
    return indexes;
  }

}
