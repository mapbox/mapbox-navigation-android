package com.mapbox.services.android.navigation.v5.models;


import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.RouteUtils;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.commons.models.Position;

@Experimental
public class RouteStepProgress {

  private LegStep step;
  private RouteLeg routeLeg;
  private int stepIndex;
  private Position userSnappedPosition;
  private double stepDistance;

  /**
   * Constructor for the step progress.
   *
   * @param routeLeg            the current leg the user is on.
   * @param userSnappedPosition the users snapped location when routeProgress was last updated.
   * @since 2.1.0
   */
  public RouteStepProgress(RouteLeg routeLeg, int stepIndex, Position userSnappedPosition) {
    this.userSnappedPosition = userSnappedPosition;
    this.step = routeLeg.getSteps().get(stepIndex);
    this.stepIndex = stepIndex;
    this.routeLeg = routeLeg;

    stepDistance = RouteUtils.getDistanceToNextStep(
      routeLeg.getSteps().get(stepIndex).getManeuver().asPosition(),
      routeLeg,
      stepIndex,
      TurfConstants.UNIT_METERS
    );
  }

  /**
   * Returns distance user has traveled along current step in unit meters.
   *
   * @return double value representing the distance the user has traveled so far along the current step. Uses unit
   * meters.
   * @since 2.1.0
   */
  public double getDistanceTraveled() {
    return stepDistance - getDistanceRemaining();
  }

  /**
   * Total distance in meters from user to end of step.
   *
   * @return double value representing the distance the user has remaining till they reach the end of the current step.
   * Uses unit meters.
   * @since 2.1.0
   */
  public double getDistanceRemaining() {
    return RouteUtils.getDistanceToNextStep(userSnappedPosition, routeLeg, stepIndex, TurfConstants.UNIT_METERS);
  }

  /**
   * Get the fraction traveled along the current step, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the next step (if another step exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the current step.
   * @since 2.1.0
   */
  public float getFractionTraveled() {
    return (float) (getDistanceTraveled() / stepDistance);
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 2.1.0
   */
  public long getDurationRemaining() {
    return (long) ((1 - getFractionTraveled()) * step.getDuration());
  }


}
