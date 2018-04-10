package com.mapbox.services.android.navigation.v5;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createDistancesToIntersections;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createIntersectionsList;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findCurrentIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findUpcomingIntersection;

class TestRouteProgressBuilder {

  RouteProgress buildDefaultTestRouteProgress(DirectionsRoute testRoute) throws Exception {
    return buildTestRouteProgress(testRoute, 100, 100,
      100, 0, 0);
  }

  RouteProgress buildTestRouteProgress(DirectionsRoute route,
                                       double stepDistanceRemaining,
                                       double legDistanceRemaining,
                                       double distanceRemaining,
                                       int stepIndex,
                                       int legIndex) throws Exception {
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
    List<StepIntersection> intersections = createIntersectionsList(currentStep, upcomingStep);
    List<Pair<StepIntersection, Double>> intersectionDistances = createDistancesToIntersections(
      currentStepPoints, intersections
    );

    StepIntersection currentIntersection = createCurrentIntersection(stepDistanceRemaining, currentStep,
      intersections, intersectionDistances);
    StepIntersection upcomingIntersection = createUpcomingIntersection(upcomingStep, intersections,
      currentIntersection);

    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(distanceRemaining)
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .upcomingStepPoints(upcomingStepPoints)
      .intersections(intersections)
      .currentIntersection(currentIntersection)
      .upcomingIntersection(upcomingIntersection)
      .intersectionDistancesAlongStep(intersectionDistances)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .build();
  }

  @NonNull
  private List<Point> buildCurrentStepPoints(LegStep currentStep) {
    String currentStepGeometry = currentStep.geometry();
    return buildStepPointsFromGeometry(currentStepGeometry);
  }

  private StepIntersection createCurrentIntersection(double stepDistanceRemaining, LegStep currentStep,
                                                     List<StepIntersection> intersections,
                                                     List<Pair<StepIntersection, Double>> intersectionDistances) {
    double stepDistanceTraveled = currentStep.distance() - stepDistanceRemaining;
    return findCurrentIntersection(intersections,
      intersectionDistances, stepDistanceTraveled
    );
  }

  private StepIntersection createUpcomingIntersection(LegStep upcomingStep, List<StepIntersection> intersections,
                                                      StepIntersection currentIntersection) {
    return findUpcomingIntersection(
        intersections, upcomingStep, currentIntersection
      );
  }

  private List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
