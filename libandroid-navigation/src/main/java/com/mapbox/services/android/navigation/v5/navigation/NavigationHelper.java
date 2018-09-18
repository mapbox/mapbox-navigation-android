package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.MaxSpeed;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;

/**
 * This contains several single purpose methods that help out when a new location update occurs and
 * calculations need to be performed on it.
 */
public class NavigationHelper {

  private static final int FIRST_POINT = 0;
  private static final int FIRST_INTERSECTION = 0;
  private static final int ONE_INDEX = 1;
  private static final int INDEX_ZERO = 0;
  private static final String EMPTY_STRING = "";
  private static final double ZERO_METERS = 0d;
  private static final int TWO_POINTS = 2;

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
   * Calculates the distance remaining in the step from the current users snapped position, to the
   * next maneuver position.
   */
  static double stepDistanceRemaining(Point snappedPosition, int legIndex, int stepIndex,
                                      DirectionsRoute directionsRoute, List<Point> coordinates) {
    List<LegStep> steps = directionsRoute.legs().get(legIndex).steps();
    Point nextManeuverPosition = nextManeuverPosition(stepIndex, steps, coordinates);

    LineString lineString = LineString.fromPolyline(steps.get(stepIndex).geometry(),
      Constants.PRECISION_6);
    // If the users snapped position equals the next maneuver
    // position or the linestring coordinate size is less than 2,the distance remaining is zero.
    if (snappedPosition.equals(nextManeuverPosition) || lineString.coordinates().size() < 2) {
      return 0;
    }
    LineString slicedLine = TurfMisc.lineSlice(snappedPosition, nextManeuverPosition, lineString);
    return TurfMeasurement.length(slicedLine, TurfConstants.UNIT_METERS);
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
   * This is used when a user has completed a step maneuver and the indices need to be incremented.
   * The main purpose of this class is to determine if an additional leg exist and the step index
   * has met the first legs total size, a leg index needs to occur and step index should be reset.
   * Otherwise, the step index is incremented while the leg index remains the same.
   * <p>
   * Rather than returning an int array, a new instance of Navigation Indices gets returned. This
   * provides type safety and making the code a bit more readable.
   * </p>
   *
   * @param routeProgress   need a routeProgress in order to get the directions route leg list size
   * @param previousIndices used for adjusting the indices
   * @return a {@link NavigationIndices} object which contains the new leg and step indices
   */
  static NavigationIndices increaseIndex(RouteProgress routeProgress,
                                         NavigationIndices previousIndices) {
    DirectionsRoute route = routeProgress.directionsRoute();
    int previousStepIndex = previousIndices.stepIndex();
    int previousLegIndex = previousIndices.legIndex();
    int routeLegSize = route.legs().size();
    int legStepSize = route.legs().get(routeProgress.legIndex()).steps().size();

    boolean isOnLastLeg = previousLegIndex == routeLegSize - 1;
    boolean isOnLastStep = previousStepIndex == legStepSize - 1;

    if (isOnLastStep && !isOnLastLeg) {
      return NavigationIndices.create((previousLegIndex + 1), 0);
    }

    if (isOnLastStep) {
      return previousIndices;
    }
    return NavigationIndices.create(previousLegIndex, (previousStepIndex + 1));
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
   * Given a current and upcoming step, this method assembles a list of {@link StepIntersection}
   * consisting of all of the current step intersections, as well as the first intersection of
   * the upcoming step (if the upcoming step isn't null).
   *
   * @param currentStep  for intersections list
   * @param upcomingStep for first intersection, if not null
   * @return complete list of intersections
   * @since 0.13.0
   */
  @NonNull
  public static List<StepIntersection> createIntersectionsList(@NonNull LegStep currentStep, LegStep upcomingStep) {
    List<StepIntersection> intersectionsWithNextManeuver = new ArrayList<>();
    intersectionsWithNextManeuver.addAll(currentStep.intersections());
    if (upcomingStep != null && !upcomingStep.intersections().isEmpty()) {
      intersectionsWithNextManeuver.add(upcomingStep.intersections().get(FIRST_POINT));
    }
    return intersectionsWithNextManeuver;
  }

  /**
   * Creates a list of pairs {@link StepIntersection} and double distance in meters along a step.
   * <p>
   * Each pair represents an intersection on the given step and its distance along the step geometry.
   * <p>
   * The first intersection is the same point as the first point of the list of step points, so will
   * always be zero meters.
   *
   * @param stepPoints    representing the step geometry
   * @param intersections along the step to be measured
   * @return list of measured intersection pairs
   * @since 0.13.0
   */
  @NonNull
  public static List<Pair<StepIntersection, Double>> createDistancesToIntersections(List<Point> stepPoints,
                                                                             List<StepIntersection> intersections) {
    boolean lessThanTwoStepPoints = stepPoints.size() < TWO_POINTS;
    boolean noIntersections = intersections.isEmpty();
    if (lessThanTwoStepPoints || noIntersections) {
      return Collections.emptyList();
    }

    LineString stepLineString = LineString.fromLngLats(stepPoints);
    Point firstStepPoint = stepPoints.get(FIRST_POINT);
    List<Pair<StepIntersection, Double>> distancesToIntersections = new ArrayList<>();

    for (StepIntersection intersection : intersections) {
      Point intersectionPoint = intersection.location();
      if (firstStepPoint.equals(intersectionPoint)) {
        distancesToIntersections.add(new Pair<>(intersection, ZERO_METERS));
      } else {
        LineString beginningLineString = TurfMisc.lineSlice(firstStepPoint, intersectionPoint, stepLineString);
        double distanceToIntersectionInMeters = TurfMeasurement.length(beginningLineString, TurfConstants.UNIT_METERS);
        distancesToIntersections.add(new Pair<>(intersection, distanceToIntersectionInMeters));
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
   * @since 0.13.0
   */
  public static StepIntersection findCurrentIntersection(@NonNull List<StepIntersection> intersections,
                                                  @NonNull List<Pair<StepIntersection, Double>> measuredIntersections,
                                                  double stepDistanceTraveled) {
    for (Pair<StepIntersection, Double> measuredIntersection : measuredIntersections) {
      double intersectionDistance = measuredIntersection.second;
      int intersectionIndex = measuredIntersections.indexOf(measuredIntersection);
      int nextIntersectionIndex = intersectionIndex + ONE_INDEX;
      int measuredIntersectionSize = measuredIntersections.size();
      boolean hasValidNextIntersection = nextIntersectionIndex < measuredIntersectionSize;

      if (hasValidNextIntersection) {
        double nextIntersectionDistance = measuredIntersections.get(nextIntersectionIndex).second;
        if (stepDistanceTraveled > intersectionDistance && stepDistanceTraveled < nextIntersectionDistance) {
          return measuredIntersection.first;
        }
      } else if (stepDistanceTraveled > measuredIntersection.second) {
        return measuredIntersection.first;
      } else {
        return measuredIntersections.get(FIRST_INTERSECTION).first;
      }
    }
    return intersections.get(FIRST_INTERSECTION);
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
   * @since 0.13.0
   */
  public static StepIntersection findUpcomingIntersection(@NonNull List<StepIntersection> intersections,
                                                   @Nullable LegStep upcomingStep,
                                                   StepIntersection currentIntersection) {
    int intersectionIndex = intersections.indexOf(currentIntersection);
    int nextIntersectionIndex = intersectionIndex + ONE_INDEX;
    int intersectionSize = intersections.size();
    boolean isValidUpcomingIntersection = nextIntersectionIndex < intersectionSize;
    if (isValidUpcomingIntersection) {
      return intersections.get(nextIntersectionIndex);
    } else if (upcomingStep != null) {
      List<StepIntersection> upcomingIntersections = upcomingStep.intersections();
      if (upcomingIntersections != null && !upcomingIntersections.isEmpty()) {
        return upcomingIntersections.get(FIRST_INTERSECTION);
      }
    }
    return null;
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

  /**
   * Retrieves the next steps maneuver position if one exist, otherwise it decodes the current steps
   * geometry and uses the last coordinate in the position list.
   */
  static Point nextManeuverPosition(int stepIndex, List<LegStep> steps, List<Point> coords) {
    // If there is an upcoming step, use it's maneuver as the position.
    if (steps.size() > (stepIndex + 1)) {
      return steps.get(stepIndex + 1).maneuver().location();
    }
    return !coords.isEmpty() ? coords.get(coords.size() - 1) : coords.get(coords.size());
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
