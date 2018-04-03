package com.mapbox.services.android.navigation.v5.routeprogress;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.List;


/**
 * This is a progress object specific to the current step the user is on.
 * <p>
 * The latest route step progress object can be obtained through either the {@link ProgressChangeListener}
 * or the {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener} callbacks.
 * Note that the route step progress object's immutable.
 * </p>
 *
 * @since 0.1.0
 */
@AutoValue
public abstract class RouteStepProgress {

  private static final String CLASS_TUNNEL = "tunnel";
  public static final int FIRST_STEP_POINT = 0;

  abstract LegStep step();

  abstract List<Point> stepPoints();

  @Nullable
  abstract LegStep nextStep();

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder step(@NonNull LegStep step);

    abstract LegStep step();

    abstract Builder stepPoints(List<Point> stepPoints);

    abstract List<Point> stepPoints();

    abstract Builder nextStep(@Nullable LegStep nextStep);

    abstract LegStep nextStep();

    abstract Builder distanceRemaining(double distanceRemaining);

    abstract double distanceRemaining();

    abstract Builder distanceTraveled(double distanceTraveled);

    abstract Builder fractionTraveled(float fractionTraveled);

    abstract Builder durationRemaining(double durationRemaining);

    abstract Builder intersections(@NonNull List<StepIntersection> intersections);

    abstract Builder tunnelIntersections(@NonNull List<StepIntersection> tunnelIntersections);

    abstract List<StepIntersection> tunnelIntersections();

    abstract Builder tunnelIntersectionDistances(@NonNull List<Pair<Double, Double>> tunnelIntersectionDistances);

    RouteStepProgress build() {
      LegStep step = step();
      double distanceRemaining = distanceRemaining();
      double distanceTraveled = calculateDistanceTraveled(step, distanceRemaining);
      distanceTraveled(distanceTraveled);
      float fractionTraveled = calculateFractionTraveled(step, distanceTraveled);
      fractionTraveled(fractionTraveled);
      durationRemaining(calculateDurationRemaining(step, fractionTraveled));
      LegStep nextStep = nextStep();
      intersections(createIntersectionsList(step, nextStep));
      tunnelIntersections(createTunnelIntersectionsList(step));
      tunnelIntersectionDistances(createDistancesToTunnelIntersections(step, stepPoints(), tunnelIntersections()));

      return autoBuild();
    }

    abstract RouteStepProgress autoBuild();

    private double calculateDistanceTraveled(LegStep step, double distanceRemaining) {
      double distanceTraveled = step.distance() - distanceRemaining;
      if (distanceTraveled < 0) {
        distanceTraveled = 0;
      }
      return distanceTraveled;
    }

    private float calculateFractionTraveled(LegStep step, double distanceTraveled) {
      float fractionTraveled = 1;

      if (step.distance() > 0) {
        fractionTraveled = (float) (distanceTraveled / step.distance());
        if (fractionTraveled < 0) {
          fractionTraveled = 0;
        }
      }
      return fractionTraveled;
    }

    private double calculateDurationRemaining(LegStep step, float fractionTraveled) {
      return (1 - fractionTraveled) * step.duration();
    }

    @NonNull
    private List<StepIntersection> createIntersectionsList(@NonNull LegStep step, LegStep nextStep) {
      List<StepIntersection> intersectionsWithNextManeuver = new ArrayList<>();
      intersectionsWithNextManeuver.addAll(step.intersections());
      if (nextStep != null && !nextStep.intersections().isEmpty()) {
        intersectionsWithNextManeuver.add(nextStep.intersections().get(0));
      }
      return intersectionsWithNextManeuver;
    }

    @NonNull
    private List<StepIntersection> createTunnelIntersectionsList(LegStep step) {
      List<StepIntersection> tunnelIntersections = new ArrayList<>();
      if (step.intersections().isEmpty()) {
        return tunnelIntersections;
      }
      for (StepIntersection intersection : step.intersections()) {
        boolean hasValidClasses = intersection.classes() != null && !intersection.classes().isEmpty();
        if (hasValidClasses) {
          for (String intersectionClass : intersection.classes()) {
            if (intersectionClass.contentEquals(CLASS_TUNNEL)) {
              tunnelIntersections.add(intersection);
            }
          }
        }
      }
      return tunnelIntersections;
    }

    @NonNull
    private List<Pair<Double, Double>> createDistancesToTunnelIntersections(LegStep step,
                                                                            List<Point> stepPoints,
                                                                            List<StepIntersection> tunnels) {
      List<Pair<Double, Double>> distancesToTunnelIntersections = new ArrayList<>();
      if (stepPoints.isEmpty()) {
        return distancesToTunnelIntersections;
      }
      if (tunnels.isEmpty()) {
        return distancesToTunnelIntersections;
      }
      List<StepIntersection> stepIntersections = step.intersections();
      if (stepIntersections == null || stepIntersections.isEmpty()) {
        return distancesToTunnelIntersections;
      }

      LineString stepLineString = LineString.fromLngLats(stepPoints);
      Point firstStepPoint = stepPoints.get(FIRST_STEP_POINT);

      for (int i = 0; i < tunnels.size(); i++) {
        StepIntersection tunnelIntersection = tunnels.get(i);

        Point tunnelBeginningPoint = tunnelIntersection.location();
        LineString beginningLineString = TurfMisc.lineSlice(firstStepPoint, tunnelBeginningPoint, stepLineString);
        double distanceToBeginningOfTunnel = TurfMeasurement.length(beginningLineString, TurfConstants.UNIT_METERS);

        int tunnelIntersectionIndex = stepIntersections.indexOf(tunnelIntersection);
        Point tunnelEndingPoint = stepIntersections.get(tunnelIntersectionIndex + 1).location();
        LineString endLineString = TurfMisc.lineSlice(firstStepPoint, tunnelEndingPoint, stepLineString);
        double distanceToEndOfTunnel = TurfMeasurement.length(endLineString, TurfConstants.UNIT_METERS);

        distancesToTunnelIntersections.add(new Pair<>(distanceToBeginningOfTunnel, distanceToEndOfTunnel));
      }
      return distancesToTunnelIntersections;
    }
  }

  public static Builder builder() {
    return new AutoValue_RouteStepProgress.Builder();
  }

  /**
   * Total distance in meters from user to end of step.
   *
   * @return double value representing the distance the user has remaining till they reach the end
   * of the current step. Uses unit meters.
   * @since 0.1.0
   */
  public abstract double distanceRemaining();

  /**
   * Returns distance user has traveled along current step in unit meters.
   *
   * @return double value representing the distance the user has traveled so far along the current
   * step. Uses unit meters.
   * @since 0.1.0
   */
  public abstract double distanceTraveled();

  /**
   * Get the fraction traveled along the current step, this is a float value between 0 and 1 and
   * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along
   * the current step.
   * @since 0.1.0
   */
  public abstract float fractionTraveled();

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public abstract double durationRemaining();

  /**
   * A collection of all the current steps intersections and the next steps maneuver location
   * (if one exist).
   *
   * @return a list of {@link StepIntersection}s which may include the next steps maneuver
   * intersection if it exist
   * @since 0.7.0
   */
  public abstract List<StepIntersection> intersections();

  /**
   * Provides a list of intersections that have tunnel classes for
   * the current step.
   * <p>
   * The returned list will be empty if not intersections are found.
   *
   * @return list of intersections containing a tunnel class
   * @since 0.13.0
   */
  public abstract List<StepIntersection> tunnelIntersections();

  /**
   * Provides a list of pairs containing two distances, in meters, along the route.
   * <p>
   * The first distance in the pair is the tunnel entrance along the step geometry.
   * The second distance is the tunnel exit along the step geometry.
   *
   * @return list of pairs containing tunnnel entrance and exit distances
   * @since 0.13.0
   */
  public abstract List<Pair<Double, Double>> tunnelIntersectionDistances();
}