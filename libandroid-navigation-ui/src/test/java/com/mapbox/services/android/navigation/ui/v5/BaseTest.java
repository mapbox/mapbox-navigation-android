package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.google.gson.JsonParser;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.mapbox.core.constants.Constants.PRECISION_6;
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

    StepIntersection currentIntersection = findCurrentIntersection(intersections,
      intersectionDistances, currentStep.distance() - stepDistanceRemaining);
    StepIntersection upcomingIntersection = findUpcomingIntersection(intersections, upcomingStep, currentIntersection);

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
  private static List<StepIntersection> createIntersectionsList(@NonNull LegStep currentStep, LegStep upcomingStep) {
    List<StepIntersection> intersectionsWithNextManeuver = new ArrayList<>();
    intersectionsWithNextManeuver.addAll(currentStep.intersections());
    if (upcomingStep != null && !upcomingStep.intersections().isEmpty()) {
      intersectionsWithNextManeuver.add(upcomingStep.intersections().get(FIRST_POINT));
    }
    return intersectionsWithNextManeuver;
  }

  @NonNull
  private static List<Pair<StepIntersection, Double>> createDistancesToIntersections(List<Point> stepPoints,
                                                                                     List<StepIntersection> intersections) {
    List<Pair<StepIntersection, Double>> distancesToIntersections = new ArrayList<>();
    List<StepIntersection> stepIntersections = new ArrayList<>(intersections);
    if (stepPoints.isEmpty()) {
      return distancesToIntersections;
    }
    if (stepIntersections.isEmpty()) {
      return distancesToIntersections;
    }

    LineString stepLineString = LineString.fromLngLats(stepPoints);
    Point firstStepPoint = stepPoints.get(FIRST_POINT);

    for (StepIntersection intersection : stepIntersections) {
      Point intersectionPoint = intersection.location();
      if (!firstStepPoint.equals(intersectionPoint)) {
        LineString beginningLineString = TurfMisc.lineSlice(firstStepPoint, intersectionPoint, stepLineString);
        double distanceToIntersection = TurfMeasurement.length(beginningLineString, TurfConstants.UNIT_METERS);
        distancesToIntersections.add(new Pair<>(intersection, distanceToIntersection));
      }
    }
    return distancesToIntersections;
  }

  /**
   * Based on the list of measured intersections and the step distance traveled, finds
   * the current intersection a user is traveling along.
   *
   * @param intersections         along the step
   * @param measuredIntersections measured intersections along the step
   * @param stepDistanceTraveled  how far the user has traveled along the step
   * @return the current step intersection
   */
  static StepIntersection findCurrentIntersection(@NonNull List<StepIntersection> intersections,
                                                  @NonNull List<Pair<StepIntersection, Double>> measuredIntersections,
                                                  double stepDistanceTraveled) {
    for (Pair<StepIntersection, Double> measuredIntersection : measuredIntersections) {
      double intersectionDistance = measuredIntersection.second;
      int intersectionIndex = measuredIntersections.indexOf(measuredIntersection);
      int nextIntersectionIndex = intersectionIndex + 1;
      int measuredIntersectionSize = measuredIntersections.size() - 1;
      boolean hasValidNextIntersection = nextIntersectionIndex < measuredIntersectionSize;

      if (hasValidNextIntersection) {
        double nextIntersectionDistance = measuredIntersections.get(nextIntersectionIndex).second;
        if (stepDistanceTraveled > intersectionDistance && stepDistanceTraveled < nextIntersectionDistance) {
          return measuredIntersection.first;
        }
      } else if (stepDistanceTraveled > measuredIntersection.second) {
        return measuredIntersection.first;
      } else {
        return measuredIntersections.get(0).first;
      }
    }
    return intersections.get(0);
  }

  /**
   * Based on the current intersection index, add one and try to get the upcoming.
   * <p>
   * If there is not an upcoming intersection on the step, check for an upcoming step and
   * return the first intersection from the upcoming step.
   *
   * @param intersections       for the current step
   * @param upcomingStep        for the first intersection if needed
   * @param currentIntersection being traveled along
   * @return the upcoming intersection on the step
   */
  static StepIntersection findUpcomingIntersection(@NonNull List<StepIntersection> intersections,
                                                   @Nullable LegStep upcomingStep,
                                                   StepIntersection currentIntersection) {
    int intersectionIndex = intersections.indexOf(currentIntersection);
    int nextIntersectionIndex = intersectionIndex + 1;
    int intersectionSize = intersections.size() - 1;
    boolean isValidUpcomingIntersection = nextIntersectionIndex < intersectionSize;
    if (isValidUpcomingIntersection) {
      return intersections.get(nextIntersectionIndex);
    } else if (upcomingStep != null) {
      List<StepIntersection> upcomingIntersections = upcomingStep.intersections();
      if (upcomingIntersections != null && !upcomingIntersections.isEmpty()) {
        return upcomingIntersections.get(0);
      }
    }
    return null;
  }

  private List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
