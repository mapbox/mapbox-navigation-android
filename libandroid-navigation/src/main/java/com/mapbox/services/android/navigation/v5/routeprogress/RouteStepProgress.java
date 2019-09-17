package com.mapbox.services.android.navigation.v5.routeprogress;


import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.LegStep;


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

  abstract LegStep step();

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder step(LegStep step);

    abstract LegStep step();

    abstract Builder distanceRemaining(double distanceRemaining);

    abstract double distanceRemaining();

    abstract Builder distanceTraveled(double distanceTraveled);

    abstract Builder fractionTraveled(float fractionTraveled);

    abstract Builder durationRemaining(double durationRemaining);

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