package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.util.Pair;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteCallback;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkBearingForStepCompletion;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createCurrentAnnotation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createDistancesToIntersections;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createIntersectionsList;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.decodeStepPoints;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findCurrentIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findUpcomingIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.increaseIndex;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.legDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.stepDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.userSnappedToRoutePosition;

class NavigationRouteProcessor implements OffRouteCallback {

  private static final int FIRST_LEG_INDEX = 0;
  private static final int FIRST_STEP_INDEX = 0;
  private static final int ONE_INDEX = 1;

  private RouteProgress routeProgress;
  private List<Point> currentStepPoints;
  private List<Point> upcomingStepPoints;
  private List<StepIntersection> currentIntersections;
  private List<Pair<StepIntersection, Double>> currentIntersectionDistances;
  private RouteLeg currentLeg;
  private LegStep currentStep;
  private LegStep upcomingStep;
  private CurrentLegAnnotation currentLegAnnotation;
  private NavigationIndices indices;
  private double stepDistanceRemaining;
  private boolean shouldIncreaseIndex;
  private RouteUtils routeUtils;

  NavigationRouteProcessor() {
    indices = NavigationIndices.create(FIRST_LEG_INDEX, FIRST_STEP_INDEX);
    routeUtils = new RouteUtils();
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
   * @param navigation for the current route / options
   * @param location   for step / leg / route distance remaining
   * @return new route progress along the route
   */
  RouteProgress buildNewRouteProgress(MapboxNavigation navigation, Location location) {
    DirectionsRoute directionsRoute = navigation.getRoute();
    MapboxNavigationOptions options = navigation.options();
    double completionOffset = options.maxTurnCompletionOffset();
    double maneuverZoneRadius = options.maneuverZoneRadius();
    checkNewRoute(navigation);
    stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
    checkManeuverCompletion(navigation, location, directionsRoute, completionOffset, maneuverZoneRadius);
    return assembleRouteProgress(directionsRoute);
  }

  RouteProgress getRouteProgress() {
    return routeProgress;
  }

  void setRouteProgress(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
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

  /**
   * Checks if the route provided is a new route.  If it is, all {@link RouteProgress}
   * data and {@link NavigationIndices} needs to be reset.
   *
   * @param mapboxNavigation to get the current route and off-route engine
   */
  private void checkNewRoute(MapboxNavigation mapboxNavigation) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    if (routeUtils.isNewRoute(routeProgress, directionsRoute)) {
      createFirstIndices(mapboxNavigation);
      routeProgress = assembleRouteProgress(directionsRoute);
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

  private void checkManeuverCompletion(MapboxNavigation navigation, Location location, DirectionsRoute directionsRoute,
                                       double completionOffset, double maneuverZoneRadius) {
    boolean withinManeuverRadius = stepDistanceRemaining < maneuverZoneRadius;
    boolean bearingMatchesManeuver = checkBearingForStepCompletion(
      location, routeProgress, stepDistanceRemaining, completionOffset
    );
    boolean forceIncreaseIndices = stepDistanceRemaining == 0 && !bearingMatchesManeuver;

    if ((bearingMatchesManeuver && withinManeuverRadius) || forceIncreaseIndices) {
      advanceIndices(navigation);
      stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
    }
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
    processNewIndex(mapboxNavigation);
  }

  /**
   * Initializes or resets the {@link NavigationIndices} for a new route received.
   *
   * @param mapboxNavigation to get the next {@link LegStep#geometry()} and {@link OffRoute}
   */
  private void createFirstIndices(MapboxNavigation mapboxNavigation) {
    indices = NavigationIndices.create(FIRST_LEG_INDEX, FIRST_STEP_INDEX);
    processNewIndex(mapboxNavigation);
  }

  /**
   * Called after {@link NavigationHelper#increaseIndex(RouteProgress, NavigationIndices)}.
   * <p>
   * Processes all new index-based data that is
   * needed for {@link NavigationRouteProcessor#assembleRouteProgress(DirectionsRoute)}.
   *
   * @param mapboxNavigation for the current route
   */
  private void processNewIndex(MapboxNavigation mapboxNavigation) {
    DirectionsRoute route = mapboxNavigation.getRoute();
    int legIndex = indices.legIndex();
    int stepIndex = indices.stepIndex();
    int upcomingStepIndex = stepIndex + ONE_INDEX;
    updateSteps(route, legIndex, stepIndex, upcomingStepIndex);
    updateStepPoints(route, legIndex, stepIndex, upcomingStepIndex);
    updateIntersections();
    clearManeuverDistances(mapboxNavigation.getOffRouteEngine());
  }

  private RouteProgress assembleRouteProgress(DirectionsRoute route) {
    int legIndex = indices.legIndex();
    int stepIndex = indices.stepIndex();

    double legDistanceRemaining = legDistanceRemaining(stepDistanceRemaining, legIndex, stepIndex, route);
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, legIndex, route);
    currentLegAnnotation = createCurrentAnnotation(currentLegAnnotation, currentLeg, legDistanceRemaining);
    double stepDistanceTraveled = currentStep.distance() - stepDistanceRemaining;

    StepIntersection currentIntersection = findCurrentIntersection(
      currentIntersections, currentIntersectionDistances, stepDistanceTraveled
    );
    StepIntersection upcomingIntersection = findUpcomingIntersection(
      currentIntersections, upcomingStep, currentIntersection
    );

    RouteProgress.Builder progressBuilder = RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .intersections(currentIntersections)
      .currentIntersection(currentIntersection)
      .upcomingIntersection(upcomingIntersection)
      .intersectionDistancesAlongStep(currentIntersectionDistances)
      .currentLegAnnotation(currentLegAnnotation);

    addUpcomingStepPoints(progressBuilder);
    return progressBuilder.build();
  }

  private void addUpcomingStepPoints(RouteProgress.Builder progressBuilder) {
    if (upcomingStepPoints != null && !upcomingStepPoints.isEmpty()) {
      progressBuilder.upcomingStepPoints(upcomingStepPoints);
    }
  }

  private void updateSteps(DirectionsRoute route, int legIndex, int stepIndex, int upcomingStepIndex) {
    currentLeg = route.legs().get(legIndex);
    List<LegStep> steps = currentLeg.steps();
    currentStep = steps.get(stepIndex);
    upcomingStep = upcomingStepIndex < steps.size() - ONE_INDEX ? steps.get(upcomingStepIndex) : null;
  }

  private void updateStepPoints(DirectionsRoute route, int legIndex, int stepIndex, int upcomingStepIndex) {
    currentStepPoints = decodeStepPoints(route, currentStepPoints, legIndex, stepIndex);
    upcomingStepPoints = decodeStepPoints(route, null, legIndex, upcomingStepIndex);
  }

  private void updateIntersections() {
    currentIntersections = createIntersectionsList(currentStep, upcomingStep);
    currentIntersectionDistances = createDistancesToIntersections(currentStepPoints, currentIntersections);
  }

  private void clearManeuverDistances(OffRoute offRoute) {
    if (offRoute instanceof OffRouteDetector) {
      ((OffRouteDetector) offRoute).clearDistancesAwayFromManeuver();
    }
  }
}
