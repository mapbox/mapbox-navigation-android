package com.mapbox.services.android.navigation.v5;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;

class TestRouteProgressBuilder {

  RouteProgress buildDefaultTestRouteProgress(DirectionsRoute testRoute) {
    return buildTestRouteProgress(testRoute, 100, 100,
      100, 0, 0);
  }

  RouteProgress buildTestRouteProgress(DirectionsRoute route,
                                       double stepDistanceRemaining,
                                       double legDistanceRemaining,
                                       double distanceRemaining,
                                       int stepIndex,
                                       int legIndex) {
    double legDurationRemaining = route.legs().get(0).duration();
    List<LegStep> steps = route.legs().get(legIndex).steps();
    LegStep currentStep = steps.get(stepIndex);
    List<Point> currentStepPoints = buildCurrentStepPoints(currentStep);
    int upcomingStepIndex = stepIndex + 1;
    List<Point> upcomingStepPoints = null;
    LegStep upcomingStep = null;
    if (upcomingStepIndex < steps.size()) {
      upcomingStep = steps.get(upcomingStepIndex);
      String upcomingStepGeometry = upcomingStep.geometry();
      upcomingStepPoints = buildStepPointsFromGeometry(upcomingStepGeometry);
    }

    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .legDurationRemaining(legDurationRemaining)
      .distanceRemaining(distanceRemaining)
      .directionsRoute(route)
      .currentStep(currentStep)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .inTunnel(false)
      .build();
  }

  @NonNull
  private List<Point> buildCurrentStepPoints(LegStep currentStep) {
    String currentStepGeometry = currentStep.geometry();
    return buildStepPointsFromGeometry(currentStepGeometry);
  }

  private List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
