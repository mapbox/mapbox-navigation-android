package com.mapbox.services.android.navigation.v5.navigation;

import android.support.v4.util.Pair;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.RouteState;
import com.mapbox.navigator.VoiceInstruction;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;

import java.util.List;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createCurrentAnnotation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createDistancesToIntersections;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createIntersectionsList;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.decodeStepPoints;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findCurrentIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findUpcomingIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;

class NavigationRouteProcessor {

  private static final int ONE_INDEX = 1;
  private final RingBuffer<RouteProgress> previousProgressList = new RingBuffer<>(2);
  private DirectionsRoute route;
  private RouteLeg currentLeg;
  private LegStep currentStep;
  private List<Point> currentStepPoints;
  private LegStep upcomingStep;
  private List<Point> upcomingStepPoints;
  private List<StepIntersection> currentIntersections;
  private List<Pair<StepIntersection, Double>> currentIntersectionDistances;
  private CurrentLegAnnotation currentLegAnnotation;

  RouteProgress buildNewRouteProgress(NavigationStatus status, DirectionsRoute route) {
    updateRoute(route);
    return buildRouteProgressFrom(status);
  }

  RouteProgress retrievePreviousRouteProgress() {
    return previousProgressList.pollLast();
  }

  private void updateRoute(DirectionsRoute route) {
    if (this.route == null || !this.route.equals(route)) {
      this.route = route;
    }
  }

  private RouteProgress buildRouteProgressFrom(NavigationStatus status) {
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
    currentLegAnnotation = createCurrentAnnotation(currentLegAnnotation, currentLeg, legDistanceRemaining);
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
      .currentLegAnnotation(currentLegAnnotation)
      .inTunnel(status.getInTunnel());

    Timber.d("NavigationRouteProcessor#stale? %s", status.getRouteState() == RouteState.STALE);

    // TODO build banner instructions from status here
    addVoiceInstructions(status, progressBuilder);
    addUpcomingStepPoints(progressBuilder);
    RouteProgress routeProgress = progressBuilder.build();
    previousProgressList.add(routeProgress);
    return routeProgress;
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

  private void addUpcomingStepPoints(RouteProgress.Builder progressBuilder) {
    if (upcomingStepPoints != null && !upcomingStepPoints.isEmpty()) {
      progressBuilder.upcomingStepPoints(upcomingStepPoints);
    }
  }

  private void addVoiceInstructions(NavigationStatus status, RouteProgress.Builder progressBuilder) {
    VoiceInstruction voiceInstruction = status.getVoiceInstruction();
    if (voiceInstruction != null) {
      progressBuilder.currentAnnouncement(voiceInstruction.getAnnouncement());
      progressBuilder.currentSsmlAnnouncement(voiceInstruction.getSsmlAnnouncement());
    }
  }
}
