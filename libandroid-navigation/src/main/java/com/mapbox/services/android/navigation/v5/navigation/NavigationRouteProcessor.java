package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.BannerInstruction;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.RouteState;
import com.mapbox.navigator.VoiceInstruction;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressStateMap;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createCurrentAnnotation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createDistancesToIntersections;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createIntersectionsList;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.decodeStepPoints;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findCurrentIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findUpcomingIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;

class NavigationRouteProcessor {

  private static final int ONE_INDEX = 1;
  private static final double ONE_SECOND_IN_MILLISECONDS = 1000.0;
  private static final int FIRST_BANNER_INSTRUCTION = 0;
  private final RouteProgressStateMap progressStateMap = new RouteProgressStateMap();
  private RouteProgress previousRouteProgress;
  private DirectionsRoute route;
  private RouteLeg currentLeg;
  private LegStep currentStep;
  private List<Point> currentStepPoints;
  private LegStep upcomingStep;
  private List<Point> upcomingStepPoints;
  private List<StepIntersection> currentIntersections;
  private List<Pair<StepIntersection, Double>> currentIntersectionDistances;
  private CurrentLegAnnotation currentLegAnnotation;

  RouteProgress buildNewRouteProgress(MapboxNavigator navigator, NavigationStatus status, DirectionsRoute route) {
    updateRoute(route);
    return buildRouteProgressFrom(status, navigator);
  }

  void updatePreviousRouteProgress(RouteProgress routeProgress) {
    previousRouteProgress = routeProgress;
  }

  @Nullable
  RouteProgress retrievePreviousRouteProgress() {
    return previousRouteProgress;
  }

  private void updateRoute(DirectionsRoute route) {
    if (this.route == null || !this.route.equals(route)) {
      this.route = route;
    }
  }

  private RouteProgress buildRouteProgressFrom(NavigationStatus status, MapboxNavigator navigator) {
    int legIndex = status.getLegIndex();
    int stepIndex = status.getStepIndex();
    int upcomingStepIndex = stepIndex + ONE_INDEX;
    updateSteps(route, legIndex, stepIndex, upcomingStepIndex);
    updateStepPoints(route, legIndex, stepIndex, upcomingStepIndex);
    updateIntersections();

    double legDistanceRemaining = status.getRemainingLegDistance();
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, legIndex, route);
    double stepDistanceRemaining = status.getRemainingStepDistance();
    double stepDistanceTraveled = currentStep.distance() - stepDistanceRemaining;
    double legDurationRemaining = status.getRemainingLegDuration() / ONE_SECOND_IN_MILLISECONDS;

    currentLegAnnotation = createCurrentAnnotation(currentLegAnnotation, currentLeg, legDistanceRemaining);
    StepIntersection currentIntersection = findCurrentIntersection(
      currentIntersections, currentIntersectionDistances, stepDistanceTraveled
    );
    StepIntersection upcomingIntersection = findUpcomingIntersection(
      currentIntersections, upcomingStep, currentIntersection
    );
    RouteState routeState = status.getRouteState();
    RouteProgressState currentRouteState = progressStateMap.get(routeState);

    RouteProgress.Builder progressBuilder = RouteProgress.builder()
      .distanceRemaining(routeDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .legDurationRemaining(legDurationRemaining)
      .stepDistanceRemaining(stepDistanceRemaining)
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .intersections(currentIntersections)
      .currentIntersection(currentIntersection)
      .upcomingIntersection(upcomingIntersection)
      .intersectionDistancesAlongStep(currentIntersectionDistances)
      .currentLegAnnotation(currentLegAnnotation)
      .inTunnel(status.getInTunnel())
      .currentState(currentRouteState);

    addVoiceInstructions(status, progressBuilder);
    addBannerInstructions(status, navigator, progressBuilder);
    addUpcomingStepPoints(progressBuilder);
    return progressBuilder.build();
  }

  private void updateSteps(DirectionsRoute route, int legIndex, int stepIndex, int upcomingStepIndex) {
    List<RouteLeg> legs = route.legs();
    if (legIndex < legs.size()) {
      currentLeg = legs.get(legIndex);
    }
    List<LegStep> steps = currentLeg.steps();
    if (stepIndex < steps.size()) {
      currentStep = steps.get(stepIndex);
    }
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

  private void addUpcomingStepPoints(RouteProgress.Builder progressBuilder) {
    if (upcomingStepPoints != null && !upcomingStepPoints.isEmpty()) {
      progressBuilder.upcomingStepPoints(upcomingStepPoints);
    }
  }

  private void addVoiceInstructions(NavigationStatus status, RouteProgress.Builder progressBuilder) {
    VoiceInstruction voiceInstruction = status.getVoiceInstruction();
    progressBuilder.voiceInstruction(voiceInstruction);
  }

  private void addBannerInstructions(NavigationStatus status, MapboxNavigator navigator,
                                     RouteProgress.Builder progressBuilder) {
    BannerInstruction bannerInstruction = status.getBannerInstruction();
    if (status.getRouteState() == RouteState.INITIALIZED) {
      bannerInstruction = navigator.retrieveBannerInstruction(FIRST_BANNER_INSTRUCTION);
    }
    progressBuilder.bannerInstruction(bannerInstruction);
  }
}
