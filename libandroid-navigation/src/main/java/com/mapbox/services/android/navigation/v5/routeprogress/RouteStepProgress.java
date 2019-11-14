package com.mapbox.services.android.navigation.v5.routeprogress;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;

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
   * The current intersection that has been passed along the route.
   * <p>
   * An intersection is considered a current intersection once passed through
   * and will remain so until a different intersection is passed through.
   *
   * @return current intersection the user has passed through
   * @since 0.13.0
   */
  public abstract StepIntersection currentIntersection();

  /**
   * The intersection being traveled towards on the route.
   * <p>
   * Will be null if the upcoming step is null (last step of the leg).
   *
   * @return intersection being traveled towards
   * @since 0.13.0
   */
  @Nullable
  public abstract StepIntersection upcomingIntersection();

  /**
   * Provides a list of pairs containing two distances, in meters, along the route.
   * <p>
   * The first distance in the pair is the tunnel entrance along the step geometry.
   * The second distance is the tunnel exit along the step geometry.
   *
   * @return list of pairs containing tunnnel entrance and exit distances
   * @since 0.13.0
   */
  public abstract List<Pair<StepIntersection, Double>> intersectionDistancesAlongStep();

  abstract LegStep step();

  @Nullable
  abstract LegStep nextStep();

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder step(LegStep step);

    abstract LegStep step();

    abstract Builder distanceRemaining(double distanceRemaining);

    abstract double distanceRemaining();

    abstract Builder nextStep(@Nullable LegStep nextStep);

    abstract Builder distanceTraveled(double distanceTraveled);

    abstract Builder fractionTraveled(float fractionTraveled);

    abstract Builder durationRemaining(double durationRemaining);

    abstract Builder intersections(@NonNull List<StepIntersection> intersections);

    abstract Builder currentIntersection(StepIntersection currentIntersection);

    abstract Builder upcomingIntersection(@Nullable StepIntersection upcomingIntersection);

    abstract Builder intersectionDistancesAlongStep(List<Pair<StepIntersection, Double>> intersections);

    abstract RouteStepProgress autoBuild();

    RouteStepProgress build() {
      LegStep step = step();
      double distanceRemaining = distanceRemaining();
      double distanceTraveled = calculateDistanceTraveled(step, distanceRemaining);
      distanceTraveled(distanceTraveled);
      float fractionTraveled = calculateFractionTraveled(step, distanceTraveled);
      fractionTraveled(fractionTraveled);
      durationRemaining(calculateDurationRemaining(step, fractionTraveled));

      return autoBuild();
    }

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
  }
}