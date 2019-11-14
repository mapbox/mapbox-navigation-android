package com.mapbox.services.android.navigation.v5.routeprogress;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

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

  /**
   * Provides a list of points that represent the current step
   * step geometry.
   *
   * @return list of points representing the current step
   * @since 0.12.0
   */
  public abstract List<Point> currentStepPoints();

  /**
   * Provides a list of points that represent the upcoming step
   * step geometry.
   *
   * @return list of points representing the upcoming step
   * @since 0.12.0
   */
  @Nullable
  public abstract List<Point> upcomingStepPoints();

  /**
   * Provides the current annotation data for a leg segment determined by
   * the distance traveled along the route.
   * <p>
   * This object will only be present when a {@link com.mapbox.api.directions.v5.models.DirectionsRoute}
   * requested with {@link com.mapbox.api.directions.v5.DirectionsCriteria#ANNOTATION_DISTANCE}.
   * <p>
   * This will be provided by default with {@link NavigationRoute#builder(Context)}.
   *
   * @return object current annotation data
   * @since 0.13.0
   */
  @Nullable
  public abstract CurrentLegAnnotation currentLegAnnotation();

  /**
   * Not public since developer can access same information from {@link RouteProgress}.
   */
  abstract RouteLeg routeLeg();

  abstract double stepDistanceRemaining();

  abstract List<StepIntersection> intersections();

  abstract StepIntersection currentIntersection();

  @Nullable
  abstract StepIntersection upcomingIntersection();

  abstract List<Pair<StepIntersection, Double>> intersectionDistancesAlongStep();

  @AutoValue.Builder
  public abstract static class Builder {

    abstract Builder routeLeg(RouteLeg routeLeg);

    abstract RouteLeg routeLeg();

    abstract Builder stepIndex(int stepIndex);

    abstract int stepIndex();

    abstract Builder stepDistanceRemaining(double stepDistanceRemaining);

    abstract double stepDistanceRemaining();

    abstract Builder distanceRemaining(double distanceRemaining);

    abstract Builder currentStepProgress(RouteStepProgress routeStepProgress);

    abstract Builder currentStepPoints(List<Point> currentStepPoints);

    abstract Builder upcomingStepPoints(@Nullable List<Point> upcomingStepPoints);

    abstract Builder intersections(List<StepIntersection> intersections);

    abstract List<StepIntersection> intersections();

    abstract Builder intersectionDistancesAlongStep(
      List<Pair<StepIntersection, Double>> intersectionDistancesAlongStep
    );

    abstract List<Pair<StepIntersection, Double>> intersectionDistancesAlongStep();

    abstract Builder currentIntersection(StepIntersection currentIntersection);

    abstract StepIntersection currentIntersection();

    abstract Builder upcomingIntersection(@Nullable StepIntersection upcomingIntersection);

    abstract StepIntersection upcomingIntersection();

    abstract Builder currentLegAnnotation(@Nullable CurrentLegAnnotation currentLegAnnotation);

    abstract RouteLegProgress autoBuild(); // not public

    public RouteLegProgress build() {
      int lastStepIndex = routeLeg().steps().size() - 1;
      boolean isOnLastStep = stepIndex() == lastStepIndex;
      int nextStepIndex = stepIndex() + 1;
      LegStep nextStep = isOnLastStep ? null : routeLeg().steps().get(nextStepIndex);

      LegStep currentStep = routeLeg().steps().get(stepIndex());
      RouteStepProgress stepProgress = RouteStepProgress.builder()
        .step(currentStep)
        .nextStep(nextStep)
        .distanceRemaining(stepDistanceRemaining())
        .intersections(intersections())
        .currentIntersection(currentIntersection())
        .upcomingIntersection(upcomingIntersection())
        .intersectionDistancesAlongStep(intersectionDistancesAlongStep())
        .build();
      currentStepProgress(stepProgress);

      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_RouteLegProgress.Builder();
  }
}