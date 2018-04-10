package com.mapbox.services.android.navigation.ui.v5;

import android.support.v4.util.Pair;

import com.google.gson.JsonParser;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createDistancesToIntersections;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.createIntersectionsList;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findCurrentIntersection;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.findUpcomingIntersection;
import static junit.framework.Assert.assertEquals;
import static okhttp3.internal.Util.UTF_8;

public class BaseTest {

  public static final double DELTA = 1E-10;
  public static final double LARGE_DELTA = 0.1;

  public static final String ACCESS_TOKEN = "pk.XXX";
  private static final int FIRST_POINT = 0;

  public void compareJson(String json1, String json2) {
    JsonParser parser = new JsonParser();
    assertEquals(parser.parse(json1), parser.parse(json2));
  }

  protected String loadJsonFixture(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  protected RouteProgress buildRouteProgress(DirectionsRoute route,
                                             double stepDistanceRemaining,
                                             double legDistanceRemaining,
                                             double distanceRemaining,
                                             int stepIndex,
                                             int legIndex) throws Exception {
    List<LegStep> steps = route.legs().get(legIndex).steps();
    LegStep currentStep = steps.get(stepIndex);
    String currentStepGeometry = currentStep.geometry();
    List<Point> currentStepPoints = buildStepPointsFromGeometry(currentStepGeometry);
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

    double stepDistanceTraveled = currentStep.distance() - stepDistanceRemaining;
    StepIntersection currentIntersection = findCurrentIntersection(intersections,
      intersectionDistances, stepDistanceTraveled
    );
    StepIntersection upcomingIntersection = findUpcomingIntersection(
      intersections, upcomingStep, currentIntersection
    );

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

  private List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
