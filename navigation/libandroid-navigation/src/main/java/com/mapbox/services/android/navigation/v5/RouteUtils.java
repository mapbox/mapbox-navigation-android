package com.mapbox.services.android.navigation.v5;

import com.mapbox.services.Constants;
import com.mapbox.services.Experimental;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfException;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A few utilities to work with RouteLeg objects.
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 0.1.0
 */
@Experimental
public class RouteUtils {

  // Default threshold for the user to be considered to be off-route (100 meters)
  public static final double DEFAULT_OFF_ROUTE_THRESHOLD_KM = 0.1;

  private double offRouteThresholdKm;

  /**
   * RouteUtils constructor using default threshold of 100 meters.
   *
   * @since 0.1.0
   * @deprecated All methods in RouteUtils are now static.
   */
  @Deprecated
  public RouteUtils() {
    this.offRouteThresholdKm = DEFAULT_OFF_ROUTE_THRESHOLD_KM;
  }

  /**
   * RouteUtils constructor allowing you to pass in a threshold value.
   *
   * @param offRouteThresholdKm Double value using unit kilometers. This value determines the
   *                            distance till you are notified.
   * @since 0.1.0
   * @deprecated All methods in RouteUtils are now static.
   */
  @Deprecated
  public RouteUtils(double offRouteThresholdKm) {
    this.offRouteThresholdKm = offRouteThresholdKm;
  }

  /**
   * @return the RouteUtils threshold as a double value; defaults 100 meters.
   * @since 0.1.0
   * @deprecated All methods in RouteUtils are now static.
   */
  @Deprecated
  public double getOffRouteThresholdKm() {
    return offRouteThresholdKm;
  }

  /**
   * Compute the distance between the position and the step line (the closest point), and checks
   * if it's within the off-route threshold.
   *
   * @param position  you want to verify is on or near the routeLeg step. If using for navigation,
   *                  this would typically be the users current location.
   * @param routeLeg  a directions routeLeg.
   * @param stepIndex integer index for step in routeLeg.
   * @return true if the position is outside the OffRoute threshold.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   * @deprecated use the static equivalent of this method.
   */
  @Deprecated
  public boolean isInStep(Position position, RouteLeg routeLeg, int stepIndex) throws ServicesException, TurfException {
    double distance = getDistanceToStep(position, routeLeg, stepIndex);
    return (distance <= offRouteThresholdKm);
  }

  /**
   * Compute the distance between the position and the step line (the closest point), and checks
   * if it's within the off-route threshold.
   *
   * @param position  you want to verify is on or near the routeLeg step. If using for navigation,
   *                  this would typically be the users current location.
   * @param routeLeg  a directions routeLeg.
   * @param stepIndex integer index for step in routeLeg.
   * @return true if the position is outside the OffRoute threshold.
   * @since 0.1.0
   */
  public static boolean isInStep(Position position, RouteLeg routeLeg, int stepIndex, double threshold) {
    double distance = getDistanceToStep(position, routeLeg, stepIndex);
    return (distance <= threshold);
  }

  /**
   * Computes the distance between the position and the closest point in routeLeg step.
   *
   * @param position  you want to measure distance to from route. If using for navigation, this
   *                  would typically be the users current location.
   * @param routeLeg  a directions routeLeg.
   * @param stepIndex integer index for step in routeLeg.
   * @return double value giving distance in kilometers.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static double getDistanceToStep(Position position, RouteLeg routeLeg, int stepIndex) throws ServicesException,
    TurfException {
    Position closestPoint = getSnapToRoute(position, routeLeg, stepIndex);
    return TurfMeasurement.distance(
      Point.fromCoordinates(position),
      Point.fromCoordinates(closestPoint),
      TurfConstants.UNIT_DEFAULT
    );
  }

  /**
   * Measures the distance from a position to the end of the route step. The position provided is snapped to the route
   * before distance is calculated.
   *
   * @param position  you want to measure distance to from route. If using for navigation, this would typically be the
   *                  users current location.
   * @param routeLeg  a directions route.
   * @param stepIndex integer index for step in route.
   * @return double value giving distance in kilometers.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static double getDistanceToNextStep(Position position, RouteLeg routeLeg, int stepIndex)
    throws ServicesException,
    TurfException {
    return getDistanceToNextStep(position, routeLeg, stepIndex, TurfConstants.UNIT_DEFAULT);
  }

  /**
   * Measures the distance from a position to the end of the route step. The position provided is snapped to the route
   * before distance is calculated.
   *
   * @param position  you want to measure distance to from route. If using for navigation, this would typically be the
   *                  users current location.
   * @param routeLeg  a directions {@link RouteLeg}.
   * @param stepIndex integer index for step in route.
   * @return double value giving distance in kilometers.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static double getDistanceToNextStep(Position position, RouteLeg routeLeg, int stepIndex, String units)
    throws ServicesException, TurfException {
    return getDistanceToNextStep(position, routeLeg, stepIndex, units, Constants.PRECISION_6);
  }

  /**
   * Measures the distance from a position to the end of the route step. The position provided is snapped to the route
   * before distance is calculated.
   *
   * @param position          you want to measure distance to from route. If using for navigation, this would typically
   *                          be the users current location.
   * @param routeLeg          a directions route.
   * @param stepIndex         integer index for step in route.
   * @param geometryPrecision either {@link Constants#PRECISION_5} or {@link Constants#PRECISION_6}
   * @return double value giving distance in kilometers.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static double getDistanceToNextStep(Position position, RouteLeg routeLeg, int stepIndex, String units,
                                             int geometryPrecision)
    throws ServicesException, TurfException {
    LegStep step = validateStep(routeLeg, stepIndex);

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(step.getGeometry(), geometryPrecision);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(position),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );
    return TurfMeasurement.lineDistance(slicedLine, units);
  }

  /**
   * Measures the distance from a position to the end of the given {@link RouteLeg}. The position provided is snapped
   * to the route before distance is calculated.
   *
   * @param position you want to measure distance to from route. If using for navigation, this would typically be the
   *                 users current location.
   * @param routeLeg a directions {@link RouteLeg}.
   * @return double value giving distance in specified units
   */
  public static double getDistanceToNextLeg(Position position, RouteLeg routeLeg, String units, int geometryPrecision) {
    // Decode the geometry
    List<Position> coords = new ArrayList<>();
    for (int i = 0; i < routeLeg.getSteps().size(); i++) {
      coords.addAll(PolylineUtils.decode(routeLeg.getSteps().get(i).getGeometry(), geometryPrecision));
    }
    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(position),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );
    return TurfMeasurement.lineDistance(slicedLine, units);
  }

  /**
   * Measures from the provided position (typically the user location), to the end of a {@link DirectionsRoute}. The
   * {@link Position} provided will be automatically snapped to the closest point to the route.
   *
   * @param position you want to measure distance to from route. If using for navigation, this would typically be the
   *                 users current location.
   * @param route    a {@link DirectionsRoute}.
   * @param units    pass in the measurement units.
   * @return double value giving distance in kilometers.
   * @since 0.1.0
   */
  public static double getDistanceToEndOfRoute(Position position, DirectionsRoute route, String units) {
    return getDistanceToEndOfRoute(position, route, Constants.PRECISION_6, units);
  }

  /**
   * Measures from the provided position (typically the user location), to the end of a {@link DirectionsRoute}. The
   * {@link Position} provided will be automatically snapped to the closest point to the route.
   *
   * @param position          you want to measure distance to from route. If using for navigation, this would typically
   *                          be the users current location.
   * @param route             a {@link DirectionsRoute}.
   * @param units             pass in the measurement units.
   * @param geometryPrecision either {@link Constants#PRECISION_5} or {@link Constants#PRECISION_6}
   * @return double value giving distance your specified unit of measurement.
   * @since 0.1.0
   */
  public static double getDistanceToEndOfRoute(Position position, DirectionsRoute route, int geometryPrecision,
                                               String units) {
    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(route.getGeometry(), geometryPrecision);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(position),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );
    return TurfMeasurement.lineDistance(slicedLine, units);
  }

  /**
   * Snaps given position to a {@link RouteLeg} step.
   *
   * @param position  that you want to snap to routeLeg. If using for navigation, this would
   *                  typically be the users current location.
   * @param routeLeg  that you want to snap position to.
   * @param stepIndex integer index for step in routeLeg.
   * @return your position snapped to the route.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static Position getSnapToRoute(Position position, RouteLeg routeLeg, int stepIndex)
    throws ServicesException, TurfException {
    return getSnapToRoute(position, routeLeg, stepIndex, Constants.PRECISION_6);
  }

  /**
   * Snaps given position to a {@link RouteLeg} step.
   *
   * @param position          that you want to snap to routeLeg. If using for navigation, this would
   *                          typically be the users current location.
   * @param routeLeg          that you want to snap position to.
   * @param stepIndex         integer index for step in routeLeg.
   * @param geometryPrecision either {@link Constants#PRECISION_5} or {@link Constants#PRECISION_6}
   * @return your position snapped to the route.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static Position getSnapToRoute(Position position, RouteLeg routeLeg, int stepIndex, int geometryPrecision)
    throws ServicesException, TurfException {
    LegStep step = validateStep(routeLeg, stepIndex);

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(step.getGeometry(), geometryPrecision);

    // No need to do the math if the step has one coordinate only
    if (coords.size() == 1) {
      return coords.get(0);
    }

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature point = TurfMisc.pointOnLine(Point.fromCoordinates(position), coords);
    return ((Point) point.getGeometry()).getCoordinates();
  }

  /**
   * Method to check whether the position given is outside the routeLeg using the threshold.
   *
   * @param position you want to verify is on or near the routeLeg. If using for navigation, this
   *                 would typically be the users current location.
   * @param routeLeg a directions routeLeg.
   * @return true if the position is beyond the threshold limit from the routeLeg.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   * @deprecated Use the new static {@code isOffRoute()} method.
   */
  @Deprecated
  public boolean isOffRoute(Position position, RouteLeg routeLeg) throws ServicesException, TurfException {
    for (int stepIndex = 0; stepIndex < routeLeg.getSteps().size(); stepIndex++) {
      if (isInStep(position, routeLeg, stepIndex)) {
        // We aren't off-route if we're close to at least one routeLeg step
        return false;
      }
    }
    return true;
  }

  /**
   * Method to check whether the position given is outside the routeLeg using the threshold.
   *
   * @param position  you want to verify is on or near the routeLeg. If using for navigation, this
   *                  would typically be the users current location.
   * @param routeLeg  a directions routeLeg.
   * @param threshold double value used to determine if position is outside the range.
   * @return true if the position is beyond the threshold limit from the routeLeg.
   * @since 0.1.0
   */
  public static boolean isOffRoute(Position position, RouteLeg routeLeg, double threshold) {
    for (int stepIndex = 0; stepIndex < routeLeg.getSteps().size(); stepIndex++) {
      if (isInStep(position, routeLeg, stepIndex, threshold)) {
        // We aren't off-route if we're close to at least one routeLeg step
        return false;
      }
    }
    return true;
  }

  /**
   * Get the closest routeLeg step to the given position. Ties will go to the furthest step in the
   * routeLeg.
   *
   * @param position that you want to get closest routeLeg step to.
   * @param routeLeg a directions routeLeg.
   * @return integer step index in routeLeg.
   * @throws ServicesException if error occurs Mapbox API related.
   * @throws TurfException     signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static int getClosestStep(Position position, RouteLeg routeLeg) throws TurfException, ServicesException {
    double minDistance = Double.MAX_VALUE;
    int closestIndex = 0;

    double distance;
    for (int stepIndex = 0; stepIndex < routeLeg.getSteps().size(); stepIndex++) {
      distance = getDistanceToStep(position, routeLeg, stepIndex);
      if (distance <= minDistance) {
        minDistance = distance;
        closestIndex = stepIndex;
      }
    }

    return closestIndex;
  }

  /**
   * Get the remaining route geometry from the position provided to the end of the directions route. This is useful
   * when the user location is traversing along the route and you don't want the past route geometry to show on the map.
   *
   * @param position the new starting postion of the route geometry. If using for navigation, this would typically be
   *                 the users current location.
   * @param route    a Directions route.
   * @return the {@link LineString} representing the new route geometry.
   * @throws TurfException signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static LineString getGeometryRemainingOnRoute(Position position, DirectionsRoute route) throws TurfException {
    return getGeometryRemainingOnRoute(position, route, Constants.PRECISION_6);
  }

  /**
   * Get the remaining route geometry from the position provided to the end of the directions route. This is useful
   * when the user location is traversing along the route and you don't want the past route geometry to show on the map.
   *
   * @param position          the new starting postion of the route geometry. If using for navigation, this would
   *                          typically be the users current location.
   * @param route             a Directions route.
   * @param geometryPrecision either {@link Constants#PRECISION_5} or {@link Constants#PRECISION_6}
   * @return the {@link LineString} representing the new route geometry.
   * @throws TurfException signals that a Turf exception of some sort has occurred.
   * @since 0.1.0
   */
  public static LineString getGeometryRemainingOnRoute(Position position, DirectionsRoute route, int geometryPrecision)
    throws TurfException {
    int lastLegIndex = route.getLegs().size() - 1;
    int lastStepIndex = route.getLegs().get(lastLegIndex).getSteps().size() - 1;

    String polyline = route.getLegs().get(lastLegIndex).getSteps().get(lastStepIndex).getGeometry();
    LineString lastStepInLastLegLineString = LineString.fromPolyline(polyline, geometryPrecision);
    int lastStepLastIndex = lastStepInLastLegLineString.getCoordinates().size() - 1;
    Position lastStepInLastLegPostion = lastStepInLastLegLineString.getCoordinates().get(lastStepLastIndex);

    return TurfMisc.lineSlice(
      Point.fromCoordinates(position),
      Point.fromCoordinates(lastStepInLastLegPostion),
      LineString.fromPolyline(route.getGeometry(), geometryPrecision)
    );
  }

  private static LegStep validateStep(RouteLeg route, int stepIndex) throws ServicesException {
    if (route == null) {
      throw new ServicesException("The provided route is empty.");
    } else if (route.getSteps() == null || route.getSteps().size() == 0) {
      throw new ServicesException("The provided route has an empty set of steps.");
    } else if (stepIndex >= route.getSteps().size()) {
      throw new ServicesException(String.format(
        Locale.US, "The provided route doesn't have so many steps (%d).", stepIndex));
    }

    return route.getSteps().get(stepIndex);
  }
}
