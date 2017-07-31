package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.auto.value.AutoValue;
import com.mapbox.services.Experimental;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;

@Experimental
@AutoValue
public abstract class RouteLegProgress {

  public abstract RouteLeg routeLeg();

  /**
   * Gives a {@link RouteStepProgress} object with information about the particular step the user is currently on.
   *
   * @return a {@link RouteStepProgress} object.
   * @since 0.1.0
   */
  public abstract RouteStepProgress currentStepProgress();

  /**
   * Index representing the current step.
   *
   * @return an {@code integer} representing the current step the user is on.
   * @since 0.1.0
   */
  public abstract int stepIndex();

  /**
   * Total distance traveled in meters along current leg.
   *
   * @return a double value representing the total distance the user has traveled along the current leg, using unit
   * meters.
   * @since 0.1.0
   */
  public double distanceTraveled() {
    double distanceTraveled = routeLeg().getDistance() - distanceRemaining();
    if (distanceTraveled < 0) {
      distanceTraveled = 0;
    }
    return distanceTraveled;
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return {@code long} value representing the duration remaining till end of route, in unit seconds.
   * @since 0.1.0
   */
  public abstract double distanceRemaining();

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public double durationRemaining() {
    return (1 - fractionTraveled()) * routeLeg().getDuration();
  }

  /**
   * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the next leg (if another leg exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the current leg.
   * @since 0.1.0
   */
  public float fractionTraveled() {
    float fractionTraveled = 1;

    if (routeLeg().getDistance() > 0) {
      fractionTraveled = (float) (distanceTraveled() / routeLeg().getDistance());
      if (fractionTraveled < 0) {
        fractionTraveled = 0;
      }
    }
    return fractionTraveled;
  }

  /**
   * Get the previous step the user traversed along, if the user is still on the first step, this will return null.
   *
   * @return a {@link LegStep} representing the previous step the user was on, if still on first step in route, returns
   * null.
   * @since 0.1.0
   */
  public LegStep previousStep() {
    if (stepIndex() == 0) {
      return null;
    }
    return routeLeg().getSteps().get(stepIndex() - 1);
  }

  /**
   * Returns the current step the user is traversing along.
   *
   * @return a {@link LegStep} representing the step the user is currently on.
   * @since 0.1.0
   */
  public LegStep currentStep() {
    return routeLeg().getSteps().get(stepIndex());
  }

  /**
   * Get the next/upcoming step immediately after the current step. If the user is on the last step on the last leg,
   * this will return null since a next step doesn't exist.
   *
   * @return a {@link LegStep} representing the next step the user will be on.
   * @since 0.1.0
   */
  public LegStep upComingStep() {
    if (routeLeg().getSteps().size() - 1 > stepIndex()) {
      return routeLeg().getSteps().get(stepIndex() + 1);
    }
    return null;
  }

  @AutoValue.Builder
  abstract static class Builder {

    private double stepDistanceRemaining;

    abstract Builder routeLeg(RouteLeg routeLeg);

    abstract Builder stepIndex(int stepIndex);

    abstract Builder distanceRemaining(double legDistanceRemaining);

    Builder stepDistanceRemaining(double stepDistanceRemaining) {
      this.stepDistanceRemaining = stepDistanceRemaining;
      return this;
    }

    abstract Builder currentStepProgress(RouteStepProgress routeStepProgress);

    abstract int stepIndex();

    abstract RouteLeg routeLeg();

    abstract RouteLegProgress autoBuild(); // not public

    public RouteLegProgress build() {
      RouteStepProgress stepProgress = RouteStepProgress.create(routeLeg().getSteps().get(stepIndex()), stepDistanceRemaining);
      currentStepProgress(stepProgress);
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_RouteLegProgress.Builder();
  }
}
