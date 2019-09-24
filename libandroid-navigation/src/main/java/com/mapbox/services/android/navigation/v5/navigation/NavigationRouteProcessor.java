package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Geometry;
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
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.decodeStepPoints;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;

class NavigationRouteProcessor {

  private static final int ONE_INDEX = 1;
  private static final double ONE_SECOND_IN_MILLISECONDS = 1000.0;
  private static final int FIRST_BANNER_INSTRUCTION = 0;
  private final RouteProgressStateMap progressStateMap = new RouteProgressStateMap();
  private RouteProgress previousRouteProgress;
  private NavigationStatus previousStatus;
  private DirectionsRoute route;
  private RouteLeg currentLeg;
  private LegStep currentStep;
  private List<Point> currentStepPoints;
  private List<Point> upcomingStepPoints;
  private CurrentLegAnnotation currentLegAnnotation;
  private Geometry routeGeometryWithBuffer;

  RouteProgress buildNewRouteProgress(MapboxNavigator navigator, NavigationStatus status, DirectionsRoute route) {
    previousStatus = status;
    updateRoute(route, navigator);
    return buildRouteProgressFrom(status, navigator);
  }

  void updatePreviousRouteProgress(RouteProgress routeProgress) {
    previousRouteProgress = routeProgress;
  }

  @Nullable
  RouteProgress retrievePreviousRouteProgress() {
    return previousRouteProgress;
  }

  @Nullable
  NavigationStatus retrievePreviousStatus() {
    return previousStatus;
  }

  private void updateRoute(DirectionsRoute route, MapboxNavigator navigator) {
    if (this.route == null || !this.route.equals(route)) {
      this.route = route;
      routeGeometryWithBuffer = navigator.retrieveRouteGeometryWithBuffer();
    }
  }

  private RouteProgress buildRouteProgressFrom(NavigationStatus status, MapboxNavigator navigator) {
    int legIndex = status.getLegIndex();
    int stepIndex = status.getStepIndex();
    int upcomingStepIndex = stepIndex + ONE_INDEX;
    updateSteps(route, legIndex, stepIndex);
    updateStepPoints(route, legIndex, stepIndex, upcomingStepIndex);

    double legDistanceRemaining = status.getRemainingLegDistance();
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, legIndex, route);
    double stepDistanceRemaining = status.getRemainingStepDistance();
    double legDurationRemaining = status.getRemainingLegDuration() / ONE_SECOND_IN_MILLISECONDS;

    currentLegAnnotation = createCurrentAnnotation(currentLegAnnotation, currentLeg, legDistanceRemaining);
    RouteState routeState = status.getRouteState();
    RouteProgressState currentRouteState = progressStateMap.get(routeState);

    RouteProgress.Builder progressBuilder = RouteProgress.builder()
      .distanceRemaining(routeDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .legDurationRemaining(legDurationRemaining)
      .stepDistanceRemaining(stepDistanceRemaining)
      .directionsRoute(route)
      .currentStep(currentStep)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .inTunnel(status.getInTunnel())
      .currentState(currentRouteState);

    addRouteGeometries(progressBuilder);
    addVoiceInstructions(status, progressBuilder);
    addBannerInstructions(status, navigator, progressBuilder);
    addUpcomingStepPoints(progressBuilder);
    return progressBuilder.build();
  }

  private void updateSteps(DirectionsRoute route, int legIndex, int stepIndex) {
    List<RouteLeg> legs = route.legs();
    if (legIndex < legs.size()) {
      currentLeg = legs.get(legIndex);
    }
    List<LegStep> steps = currentLeg.steps();
    if (stepIndex < steps.size()) {
      currentStep = steps.get(stepIndex);
    }
  }

  private void updateStepPoints(DirectionsRoute route, int legIndex, int stepIndex, int upcomingStepIndex) {
    currentStepPoints = decodeStepPoints(route, currentStepPoints, legIndex, stepIndex);
    upcomingStepPoints = decodeStepPoints(route, null, legIndex, upcomingStepIndex);
  }

  private void addUpcomingStepPoints(RouteProgress.Builder progressBuilder) {
    if (upcomingStepPoints != null && !upcomingStepPoints.isEmpty()) {
      progressBuilder.upcomingStepPoints(upcomingStepPoints);
    }
  }

  private void addRouteGeometries(RouteProgress.Builder progressBuilder) {
    progressBuilder.routeGeometryWithBuffer(routeGeometryWithBuffer);
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
