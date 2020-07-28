package com.mapbox.navigation.ui;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteStepProgress;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;

class TestRouteProgressBuilder {

  RouteProgress buildTestRouteProgress(DirectionsRoute route,
                                       double stepDistanceRemaining,
                                       double legDistanceRemaining,
                                       double distanceRemaining,
                                       int stepIndex,
                                       int legIndex) {
    final double legDurationRemaining = route.legs().get(0).duration();
    List<LegStep> steps = route.legs().get(legIndex).steps();
    LegStep currentStep = steps.get(stepIndex);
    String currentStepGeometry = currentStep.geometry();
    final List<Point> currentStepPoints = buildStepPointsFromGeometry(currentStepGeometry);
    int upcomingStepIndex = stepIndex + 1;
    List<Point> upcomingStepPoints = null;
    LegStep upcomingStep = null;
    if (upcomingStepIndex < steps.size()) {
      upcomingStep = steps.get(upcomingStepIndex);
      String upcomingStepGeometry = upcomingStep.geometry();
      upcomingStepPoints = buildStepPointsFromGeometry(upcomingStepGeometry);
    }

    // SBNOTE: this is probably not 100% correct but it's correct enough for the tests i'm working
    // on to pass.
    float distanceTraveled = (float)(currentStep.distance() - distanceRemaining);
    RouteStepProgress.Builder stepProgressBuilder = new RouteStepProgress.Builder();
    stepProgressBuilder.stepIndex(stepIndex);
    stepProgressBuilder.step(currentStep);
    stepProgressBuilder.distanceTraveled(distanceTraveled);
    stepProgressBuilder.fractionTraveled((float)(distanceTraveled / currentStep.distance()));
    stepProgressBuilder.distanceRemaining((float)stepDistanceRemaining);
    stepProgressBuilder.durationRemaining((long) (legDurationRemaining  / 1000.0));

    double routeLegDistanceTraveled = route.legs().get(0).distance() - legDistanceRemaining;
    RouteLegProgress.Builder legProgressBuilder = new RouteLegProgress.Builder();
    legProgressBuilder.currentStepProgress(stepProgressBuilder.build());
    legProgressBuilder.legIndex(legIndex);
    legProgressBuilder.routeLeg(route.legs().get(0));
    legProgressBuilder.distanceTraveled((float) routeLegDistanceTraveled);
    legProgressBuilder.distanceRemaining((float) legDurationRemaining);
    legProgressBuilder.upcomingStep(upcomingStep);

    return new RouteProgress.Builder(route)
            .distanceRemaining((float) legDistanceRemaining)
            .upcomingStepPoints(currentStepPoints)
            .currentLegProgress(legProgressBuilder.build())
      .build();
  }

  private List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
