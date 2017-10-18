package com.mapbox.services.android.navigation.v5.routeprogress;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;

/**
 * This is a progress object specific to the current leg the user is on. If there is only one leg
 * in the directions route, much of this information will be identical to the parent
 * {@link RouteProgress}.
 * <p>
 * The latest route leg progress object can be obtained through either the {@link ProgressChangeListener}
 * or the {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener} callbacks.
 * Note that the route leg progress object's immutable.
 * </p>
 *
 * @since 0.1.0
 */
@AutoValue
public abstract class RouteLegProgress {

  /**
   * Not public since developer can access same information from {@link RouteProgress}.
   */
  abstract RouteLeg routeLeg();

  /**
   * Constructor for the route leg progress information.
   *
   * @param routeLeg            the current {@link RouteLeg} the user is traversing along
   * @param stepIndex           the current step index the user is on
   * @param legDistanceRemaining the leg distance remaining which is calculated in navigation engine
   * @param stepDistanceRemaining the step distance remaining which is calculated in navigation engine
   * @since 0.1.0
   */
  static RouteLegProgress create(RouteLeg routeLeg, int stepIndex, double legDistanceRemaining,
                                 double stepDistanceRemaining) {
    RouteStepProgress stepProgress = RouteStepProgress.create(
      routeLeg.steps().get(stepIndex), stepDistanceRemaining);
    return new AutoValue_RouteLegProgress(
      routeLeg, stepIndex, legDistanceRemaining, stepProgress);
  }

  /**
   * Index representing the current step the user is on.
   *
   * @return an integer representing the current step the user is on
   * @since 0.1.0
   */
  public abstract int stepIndex();

  /**
   * Total distance traveled in meters along current leg.
   *
   * @return a double value representing the total distance the user has traveled along the current
   * leg, using unit meters.
   * @since 0.1.0
   */
  public double distanceTraveled() {
    double distanceTraveled = routeLeg().distance() - distanceRemaining();
    if (distanceTraveled < 0) {
      distanceTraveled = 0;
    }
    return distanceTraveled;
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return long value representing the duration remaining till end of route, in unit seconds
   * @since 0.1.0
   */
  public abstract double distanceRemaining();

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return long value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public double durationRemaining() {
    return (1 - fractionTraveled()) * routeLeg().duration();
  }

  /**
   * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and
   * isn't guaranteed to reach 1 before the user reaches the next waypoint.
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the
   * current leg
   * @since 0.1.0
   */
  public float fractionTraveled() {
    float fractionTraveled = 1;

    if (routeLeg().distance() > 0) {
      fractionTraveled = (float) (distanceTraveled() / routeLeg().distance());
      if (fractionTraveled < 0) {
        fractionTraveled = 0;
      }
    }
    return fractionTraveled;
  }

  /**
   * Get the previous step the user traversed along, if the user is still on the first step, this
   * will return null.
   *
   * @return a {@link LegStep} representing the previous step the user was on, if still on first
   * step in route, returns null
   * @since 0.1.0
   */
  @Nullable
  public LegStep previousStep() {
    if (stepIndex() == 0) {
      return null;
    }
    return routeLeg().steps().get(stepIndex() - 1);
  }

  /**
   * Returns the current step the user is traversing along.
   *
   * @return a {@link LegStep} representing the step the user is currently on
   * @since 0.1.0
   */
  @NonNull
  public LegStep currentStep() {
    return routeLeg().steps().get(stepIndex());
  }

  /**
   * Get the next/upcoming step immediately after the current step. If the user is on the last step
   * on the last leg, this will return null since a next step doesn't exist.
   *
   * @return a {@link LegStep} representing the next step the user will be on.
   * @since 0.1.0
   */
  @Nullable
  public LegStep upComingStep() {
    if (routeLeg().steps().size() - 1 > stepIndex()) {
      return routeLeg().steps().get(stepIndex() + 1);
    }
    return null;
  }

  /**
   * This will return the {@link LegStep} two steps ahead of the current step the user's on. If the
   * user's current step is within 2 steps of their final destination this will return null.
   *
   * @return the {@link LegStep} after the {@link #upComingStep()}
   * @since 0.5.0
   */
  @Nullable
  public LegStep followOnStep() {
    if (routeLeg().steps().size() - 2 > stepIndex()) {
      return routeLeg().steps().get(stepIndex() + 2);
    }
    return null;
  }

  /**
   * Gives a {@link RouteStepProgress} object with information about the particular step the user
   * is currently on.
   *
   * @return a {@link RouteStepProgress} object
   * @since 0.1.0
   */
  public abstract RouteStepProgress currentStepProgress();
}