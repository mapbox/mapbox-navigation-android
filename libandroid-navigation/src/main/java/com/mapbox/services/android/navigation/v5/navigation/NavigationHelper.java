package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.MaxSpeed;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;

/**
 * This contains several single purpose methods that help out when a new location update occurs and
 * calculations need to be performed on it.
 */
public class NavigationHelper {

  private static final int INDEX_ZERO = 0;
  private static final String EMPTY_STRING = "";

  private NavigationHelper() {
    // Empty private constructor to prevent users creating an instance of this class.
  }

  /**
   * When a milestones triggered, it's instruction needs to be built either using the provided
   * string or an empty string.
   */
  static String buildInstructionString(RouteProgress routeProgress, Milestone milestone) {
    if (milestone.getInstruction() != null) {
      // Create a new custom instruction based on the Instruction packaged with the Milestone
      return milestone.getInstruction().buildInstruction(routeProgress);
    }
    return EMPTY_STRING;
  }

  /**
   * Takes in the leg distance remaining value already calculated and if additional legs need to be
   * traversed along after the current one, adds those distances and returns the new distance.
   * Otherwise, if the route only contains one leg or the users on the last leg, this value will
   * equal the leg distance remaining.
   */
  static double routeDistanceRemaining(double legDistanceRemaining, int legIndex,
                                       DirectionsRoute directionsRoute) {
    if (directionsRoute.legs().size() < 2) {
      return legDistanceRemaining;
    }

    for (int i = legIndex + 1; i < directionsRoute.legs().size(); i++) {
      legDistanceRemaining += directionsRoute.legs().get(i).distance();
    }
    return legDistanceRemaining;
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
  static List<Point> decodeStepPoints(DirectionsRoute directionsRoute, List<Point> currentPoints,
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
   * Given a list of distance annotations, find the current annotation index.  This index retrieves the
   * current annotation from any provided annotation list in {@link LegAnnotation}.
   *
   * @param currentLegAnnotation current annotation being traveled along
   * @param leg                  holding each list of annotations
   * @param legDistanceRemaining to determine the new set of annotations
   * @return a current set of annotation data for the user's position along the route
   */
  @Nullable
  public static CurrentLegAnnotation createCurrentAnnotation(CurrentLegAnnotation currentLegAnnotation,
                                                      RouteLeg leg, double legDistanceRemaining) {
    LegAnnotation legAnnotation = leg.annotation();
    if (legAnnotation == null) {
      return null;
    }
    List<Double> distanceList = legAnnotation.distance();
    if (distanceList == null || distanceList.isEmpty()) {
      return null;
    }

    CurrentLegAnnotation.Builder annotationBuilder = CurrentLegAnnotation.builder();
    int annotationIndex = findAnnotationIndex(
      currentLegAnnotation, annotationBuilder, leg, legDistanceRemaining, distanceList
    );

    annotationBuilder.distance(distanceList.get(annotationIndex));
    List<Double> durationList = legAnnotation.duration();
    if (durationList != null) {
      annotationBuilder.duration(durationList.get(annotationIndex));
    }
    List<Double> speedList = legAnnotation.speed();
    if (speedList != null) {
      annotationBuilder.speed(speedList.get(annotationIndex));
    }
    List<MaxSpeed> maxspeedList = legAnnotation.maxspeed();
    if (maxspeedList != null) {
      annotationBuilder.maxspeed(maxspeedList.get(annotationIndex));
    }
    List<String> congestionList = legAnnotation.congestion();
    if (congestionList != null) {
      annotationBuilder.congestion(congestionList.get(annotationIndex));
    }
    annotationBuilder.index(annotationIndex);
    return annotationBuilder.build();
  }

  private static int findAnnotationIndex(CurrentLegAnnotation currentLegAnnotation,
                                         CurrentLegAnnotation.Builder annotationBuilder, RouteLeg leg,
                                         double legDistanceRemaining, List<Double> distanceAnnotationList) {
    List<Double> legDistances = new ArrayList<>(distanceAnnotationList);
    Double totalLegDistance = leg.distance();
    double distanceTraveled = totalLegDistance - legDistanceRemaining;

    int distanceIndex = 0;
    double annotationDistancesTraveled = 0;
    if (currentLegAnnotation != null) {
      distanceIndex = currentLegAnnotation.index();
      annotationDistancesTraveled = currentLegAnnotation.distanceToAnnotation();
    }
    for (int i = distanceIndex; i < legDistances.size(); i++) {
      Double distance = legDistances.get(i);
      annotationDistancesTraveled += distance;
      if (annotationDistancesTraveled > distanceTraveled) {
        double distanceToAnnotation = annotationDistancesTraveled - distance;
        annotationBuilder.distanceToAnnotation(distanceToAnnotation);
        return i;
      }
    }
    return INDEX_ZERO;
  }

  private static boolean hasInvalidLegs(List<RouteLeg> legs) {
    return legs == null || legs.isEmpty();
  }

  private static boolean hasInvalidSteps(List<LegStep> steps) {
    return steps == null || steps.isEmpty();
  }
}
