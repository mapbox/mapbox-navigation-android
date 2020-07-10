package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.MaxSpeed;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteCallback;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.utils.MathUtils;
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
   * Takes in a raw location, converts it to a point, and snaps it to the closest point along the
   * route. This is isolated as separate logic from the snap logic provided because we will always
   * need to snap to the route in order to get the most accurate information.
   */
  static Point userSnappedToRoutePosition(Location location, List<Point> coordinates) {
    if (coordinates.size() < 2) {
      return Point.fromLngLat(location.getLongitude(), location.getLatitude());
    }

    Point locationToPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.nearestPointOnLine(locationToPoint, coordinates);
    return ((Point) feature.geometry());
  }

  static Location buildSnappedLocation(MapboxNavigation mapboxNavigation, boolean snapToRouteEnabled,
                                       Location rawLocation, RouteProgress routeProgress, boolean userOffRoute) {
    final Location location;
    if (!userOffRoute && snapToRouteEnabled) {
      location = getSnappedLocation(mapboxNavigation, rawLocation, routeProgress);
    } else {
      location = rawLocation;
    }
    return location;
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

    // When the coordinates are empty, no distance can be calculated
    if(nextManeuverPosition == null) {
      return 0;
    }

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
   * Takes in the already calculated step distance and iterates through the step list from the
   * step index value plus one till the end of the leg.
   */
  static double legDistanceRemaining(double stepDistanceRemaining, int legIndex, int stepIndex,
                                     DirectionsRoute directionsRoute) {
    List<LegStep> steps = directionsRoute.legs().get(legIndex).steps();
    if ((steps.size() < stepIndex + 1)) {
      return stepDistanceRemaining;
    }
    for (int i = stepIndex + 1; i < steps.size(); i++) {
      stepDistanceRemaining += steps.get(i).distance();
    }
    return stepDistanceRemaining;
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
   * Checks whether the user's bearing matches the next step's maneuver provided bearingAfter
   * variable. This is one of the criteria's required for the user location to be recognized as
   * being on the next step or potentially arriving.
   * <p>
   * If the expected turn angle is less than the max turn completion offset, this method will
   * wait for the step distance remaining to be 0.  This way, the step index does not increase
   * prematurely.
   *
   * @param userLocation          the location of the user
   * @param previousRouteProgress used for getting the most recent route information
   * @return boolean true if the user location matches (using a tolerance) the final heading
   * @since 0.2.0
   */
  static boolean checkBearingForStepCompletion(Location userLocation, RouteProgress previousRouteProgress,
                                               double stepDistanceRemaining, double maxTurnCompletionOffset) {
    if (previousRouteProgress.currentLegProgress().upComingStep() == null) {
      return false;
    }

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we
    // count this as within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    StepManeuver maneuver = previousRouteProgress.currentLegProgress().upComingStep().maneuver();
    double initialBearing = maneuver.bearingBefore();
    double initialBearingNormalized = MathUtils.wrap(initialBearing, 0, 360);
    double finalBearing = maneuver.bearingAfter();
    double finalBearingNormalized = MathUtils.wrap(finalBearing, 0, 360);

    double expectedTurnAngle = MathUtils.differenceBetweenAngles(initialBearingNormalized, finalBearingNormalized);

    double userBearingNormalized = MathUtils.wrap(userLocation.getBearing(), 0, 360);
    double userAngleFromFinalBearing = MathUtils.differenceBetweenAngles(finalBearingNormalized, userBearingNormalized);

    if (expectedTurnAngle <= maxTurnCompletionOffset) {
      return stepDistanceRemaining == 0;
    } else {
      return userAngleFromFinalBearing <= maxTurnCompletionOffset;
    }
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
      if (measuredIntersection.first == null)
        return intersections.get(0);
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
   * This method runs through the list of milestones in {@link MapboxNavigation#getMilestones()}
   * and returns a list of occurring milestones (if any), based on their individual criteria.
   *
   * @param previousRouteProgress for checking if milestone is occurring
   * @param routeProgress         for checking if milestone is occurring
   * @param mapboxNavigation      for list of milestones
   * @return list of occurring milestones
   */
  static List<Milestone> checkMilestones(RouteProgress previousRouteProgress,
                                         RouteProgress routeProgress,
                                         MapboxNavigation mapboxNavigation) {
    List<Milestone> milestones = new ArrayList<>();
    for (Milestone milestone : mapboxNavigation.getMilestones()) {
      if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
        milestones.add(milestone);
      }
    }
    return milestones;
  }

  /**
   * This method checks if off route detection is enabled or disabled.
   * <p>
   * If enabled, the off route engine is retrieved from {@link MapboxNavigation} and
   * {@link OffRouteDetector#isUserOffRoute(Location, RouteProgress, MapboxNavigationOptions)} is called
   * to determine if the location is on or off route.
   *
   * @param navigationLocationUpdate containing new location and navigation objects
   * @param routeProgress    to be used in off route check
   * @param callback         only used if using our default {@link OffRouteDetector}
   * @return true if on route, false otherwise
   */
  static boolean isUserOffRoute(NavigationLocationUpdate navigationLocationUpdate, RouteProgress routeProgress,
                                OffRouteCallback callback) {
    MapboxNavigationOptions options = navigationLocationUpdate.mapboxNavigation().options();
    if (!options.enableOffRouteDetection()) {
      return false;
    }
    OffRoute offRoute = navigationLocationUpdate.mapboxNavigation().getOffRouteEngine();
    setOffRouteDetectorCallback(offRoute, callback);
    Location location = navigationLocationUpdate.location();
    return offRoute.isUserOffRoute(location, routeProgress, options);
  }

  static boolean shouldCheckFasterRoute(NavigationLocationUpdate navigationLocationUpdate,
                                        RouteProgress routeProgress) {
    if(navigationLocationUpdate == null)
      return false;
    FasterRoute fasterRoute = navigationLocationUpdate.mapboxNavigation().getFasterRouteEngine();
    return fasterRoute.shouldCheckFasterRoute(navigationLocationUpdate.location(), routeProgress);
  }

  /**
   * Retrieves the next steps maneuver position if one exist, otherwise it decodes the current steps
   * geometry and uses the last coordinate in the position list.
   */
  @Nullable
  static Point nextManeuverPosition(int stepIndex, List<LegStep> steps, List<Point> coords) {
    // If there is an upcoming step, use it's maneuver as the position.
    if (steps.size() > (stepIndex + 1)) {
      return steps.get(stepIndex + 1).maneuver().location();
    }
    return !coords.isEmpty() ? coords.get(coords.size() - 1) : null;
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

  private static Location getSnappedLocation(MapboxNavigation mapboxNavigation, Location location,
                                             RouteProgress routeProgress) {
    Snap snap = mapboxNavigation.getSnapEngine();
    return snap.getSnappedLocation(location, routeProgress);
  }

  private static void setOffRouteDetectorCallback(OffRoute offRoute, OffRouteCallback callback) {
    if (offRoute instanceof OffRouteDetector) {
      ((OffRouteDetector) offRoute).setOffRouteCallback(callback);
    }
  }

  private static boolean hasInvalidLegs(List<RouteLeg> legs) {
    return legs == null || legs.isEmpty();
  }

  private static boolean hasInvalidSteps(List<LegStep> steps) {
    return steps == null || steps.isEmpty();
  }
}
