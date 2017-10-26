package com.mapbox.services.android.navigation.v5.routeprogress;


import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.LegStep;


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

  abstract LegStep step();

  public static RouteStepProgress create(LegStep step, double stepDistanceRemaining) {
    return new AutoValue_RouteStepProgress(step, stepDistanceRemaining);
  }

  /**
   * Returns distance user has traveled along current step in unit meters.
   *
   * @return double value representing the distance the user has traveled so far along the current
   * step. Uses unit meters.
   * @since 0.1.0
   */
  public double distanceTraveled() {
    double distanceTraveled = step().distance() - distanceRemaining();
    if (distanceTraveled < 0) {
      distanceTraveled = 0;
    }
    return distanceTraveled;
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
   * Get the fraction traveled along the current step, this is a float value between 0 and 1 and
   * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along
   * the current step.
   * @since 0.1.0
   */
  public float fractionTraveled() {
    float fractionTraveled = 1;

    if (step().distance() > 0) {
      fractionTraveled = (float) (distanceTraveled() / step().distance());
      if (fractionTraveled < 0) {
        fractionTraveled = 0;
      }
    }
    return fractionTraveled;
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public double durationRemaining() {
    return (1 - fractionTraveled()) * step().duration();
  }
}