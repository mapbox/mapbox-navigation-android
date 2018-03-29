package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteCallback;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkBearingForStepCompletion;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.getSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.increaseIndex;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.legDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.stepDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.userSnappedToRoutePosition;

class NavigationRouteProcessor implements OffRouteCallback {

  private static final int FIRST_LEG_INDEX = 0;
  private static final int FIRST_STEP_INDEX = 0;
  private static final int SECOND_STEP_INDEX = 1;

  private RouteProgress routeProgress;
  private List<Point> currentStepPoints;
  private List<Point> upcomingStepPoints;
  private NavigationIndices indices;
  private boolean shouldIncreaseIndex;

  NavigationRouteProcessor() {
    indices = NavigationIndices.create(FIRST_LEG_INDEX, FIRST_STEP_INDEX);
  }

  @Override
  public void onShouldIncreaseIndex() {
    shouldIncreaseIndex = true;
  }

  /**
   * Will take a given location update and create a new {@link RouteProgress}
   * based on our calculations of the distances remaining.
   * <p>
   * Also in charge of detecting if a step / leg has finished and incrementing the
   * indices if needed ({@link NavigationRouteProcessor#advanceIndices(MapboxNavigation)} handles
   * the decoding of the next step point list).
   *
   * @param mapboxNavigation for the current route / options
   * @param location         for step / leg / route distance remaining
   * @return new route progress along the route
   */
  RouteProgress buildNewRouteProgress(MapboxNavigation mapboxNavigation, Location location) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();
    double completionOffset = options.maxTurnCompletionOffset();
    double maneuverZoneRadius = options.maneuverZoneRadius();

    checkNewRoute(mapboxNavigation);

    double stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
    boolean withinManeuverRadius = stepDistanceRemaining < maneuverZoneRadius;
    boolean bearingMatchesManeuver = checkBearingForStepCompletion(
      location, routeProgress, stepDistanceRemaining, completionOffset
    );
    boolean forceIncreaseIndices = stepDistanceRemaining == 0 && !bearingMatchesManeuver;

    if ((bearingMatchesManeuver && withinManeuverRadius) || forceIncreaseIndices) {
      advanceIndices(mapboxNavigation);
      stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
    }

    int legIndex = indices.legIndex();
    int stepIndex = indices.stepIndex();
    double legDistanceRemaining = legDistanceRemaining(stepDistanceRemaining, legIndex, stepIndex, directionsRoute);
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, legIndex, directionsRoute);

    RouteProgress.Builder progressBuilder = RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .stepIndex(stepIndex)
      .legIndex(legIndex);

    if (upcomingStepPoints != null && !upcomingStepPoints.isEmpty()) {
      progressBuilder.upcomingStepPoints(upcomingStepPoints);
    }
    return progressBuilder.build();
  }

  RouteProgress getRouteProgress() {
    return routeProgress;
  }

  void setRouteProgress(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
  }

  private void clearManeuverDistances(OffRoute offRoute) {
    if (offRoute instanceof OffRouteDetector) {
      ((OffRouteDetector) offRoute).clearDistancesAwayFromManeuver();
    }
  }

  private boolean hasInvalidLegs(List<RouteLeg> legs) {
    return legs == null || legs.isEmpty();
  }

  private boolean hasInvalidSteps(List<LegStep> steps) {
    return steps == null || steps.isEmpty();
  }

  /**
   * If the {@link OffRouteCallback#onShouldIncreaseIndex()} has been called by the
   * {@link com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector}, shouldIncreaseIndex
   * will be true and the {@link NavigationIndices} index needs to be increased by one.
   *
   * @param navigation to get the next {@link LegStep#geometry()} and off-route engine
   */
  void checkIncreaseIndex(MapboxNavigation navigation) {
    if (shouldIncreaseIndex) {
      advanceIndices(navigation);
      shouldIncreaseIndex = false;
    }
  }

  Location buildSnappedLocation(MapboxNavigation mapboxNavigation, boolean snapToRouteEnabled,
                                Location rawLocation, RouteProgress routeProgress, boolean userOffRoute) {
    final Location location;
    if (!userOffRoute && snapToRouteEnabled) {
      location = getSnappedLocation(mapboxNavigation, rawLocation, routeProgress);
    } else {
      location = rawLocation;
    }
    return location;
  }

  /**
   * Increases the step index in {@link NavigationIndices} by 1.
   * <p>
   * Decodes the step points for the new step and clears the distances from
   * maneuver stack, as the maneuver has now changed.
   *
   * @param mapboxNavigation to get the next {@link LegStep#geometry()} and {@link OffRoute}
   */
  private void advanceIndices(MapboxNavigation mapboxNavigation) {
    indices = increaseIndex(routeProgress, indices);
    updateStepPoints(mapboxNavigation);
  }

  private void updateStepPoints(MapboxNavigation mapboxNavigation) {
    DirectionsRoute route = mapboxNavigation.getRoute();
    currentStepPoints = decodeStepPoints(route, currentStepPoints, indices.legIndex(), indices.stepIndex());
    int upcomingStepIndex = indices.stepIndex() + 1;
    upcomingStepPoints = decodeStepPoints(route, upcomingStepPoints, indices.legIndex(), upcomingStepIndex);
    clearManeuverDistances(mapboxNavigation.getOffRouteEngine());
  }

  /**
   * Given the current {@link DirectionsRoute} and leg / step index,
   * return a list of {@link Point} representing the current step.
   * <p>
   * This method is only used on a per-step basis as {@link PolylineUtils#decode(String, int)}
   * can be a heavy operation based on the length of the step.
   * <p>
   * Returns null if index is invalid.
   *
   * @param directionsRoute for list of steps
   * @param legIndex        to get current step list
   * @param stepIndex       to get current step
   * @return list of {@link Point} representing the current step
   */
  private List<Point> decodeStepPoints(DirectionsRoute directionsRoute, List<Point> currentPoints,
                                       int legIndex, int stepIndex) {
    List<RouteLeg> legs = directionsRoute.legs();
    if (hasInvalidLegs(legs)) {
      return currentPoints;
    }
    List<LegStep> steps = legs.get(legIndex).steps();
    if (hasInvalidSteps(steps)) {
      return currentPoints;
    }
    boolean invalidStepIndex = stepIndex < 0 || stepIndex > steps.size() - 1;
    if (invalidStepIndex) {
      return currentPoints;
    }
    LegStep step = steps.get(stepIndex);
    if (step == null) {
      return currentPoints;
    }
    String stepGeometry = step.geometry();
    if (stepGeometry != null) {
      return PolylineUtils.decode(stepGeometry, PRECISION_6);
    }
    return currentPoints;
  }

  /**
   * Checks if the route provided is a new route.  If it is, all {@link RouteProgress}
   * data and {@link NavigationIndices} needs to be reset.
   *
   * @param mapboxNavigation to get the current route and off-route engine
   */
  private void checkNewRoute(MapboxNavigation mapboxNavigation) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    if (RouteUtils.isNewRoute(routeProgress, directionsRoute)) {

      currentStepPoints = decodeStepPoints(directionsRoute, currentStepPoints, FIRST_LEG_INDEX, FIRST_STEP_INDEX);
      upcomingStepPoints = decodeStepPoints(directionsRoute, currentStepPoints, FIRST_LEG_INDEX, SECOND_STEP_INDEX);
      clearManeuverDistances(mapboxNavigation.getOffRouteEngine());

      routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(directionsRoute.legs().get(FIRST_LEG_INDEX).steps().get(FIRST_STEP_INDEX).distance())
        .legDistanceRemaining(directionsRoute.legs().get(FIRST_LEG_INDEX).distance())
        .distanceRemaining(directionsRoute.distance())
        .directionsRoute(directionsRoute)
        .currentStepPoints(currentStepPoints)
        .upcomingStepPoints(upcomingStepPoints)
        .stepIndex(FIRST_STEP_INDEX)
        .legIndex(FIRST_LEG_INDEX)
        .build();

      indices = NavigationIndices.create(FIRST_LEG_INDEX, FIRST_STEP_INDEX);
    }
  }

  /**
   * Given a location update, calculate the current step distance remaining.
   *
   * @param location        for current coordinates
   * @param directionsRoute for current {@link LegStep}
   * @return distance remaining in meters
   */
  private double calculateStepDistanceRemaining(Location location, DirectionsRoute directionsRoute) {
    Point snappedPosition = userSnappedToRoutePosition(location, currentStepPoints);
    return stepDistanceRemaining(
      snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, currentStepPoints
    );
  }
}
