package com.mapbox.services.android.navigation.v5.models;

import com.mapbox.services.Constants;
import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.RouteUtils;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.commons.models.Position;

@Experimental
public class RouteLegProgress {

  private RouteLeg routeLeg;
  private int stepIndex;
  private Position userSnappedPosition;
  private RouteStepProgress currentStepProgress;
  private double legDistance;

  /**
   * Constructor for the route leg routeProgress information.
   *
   * @param routeLeg            The current {@link RouteLeg} the user is traversing along.
   * @param stepIndex           The current step index the user is on.
   * @param userSnappedPosition the users snapped location when routeProgress was last updated.
   * @since 0.1.0
   */
  public RouteLegProgress(RouteLeg routeLeg, int stepIndex, Position userSnappedPosition) {
    this.routeLeg = routeLeg;
    this.stepIndex = stepIndex;
    this.userSnappedPosition = userSnappedPosition;
    currentStepProgress = new RouteStepProgress(routeLeg, stepIndex, userSnappedPosition);

    legDistance = RouteUtils.getDistanceToNextLeg(
      routeLeg.getSteps().get(0).getManeuver().asPosition(),
      routeLeg,
      TurfConstants.UNIT_METERS,
      Constants.PRECISION_6
    );
  }

  /**
   * Gives a {@link RouteStepProgress} object with information about the particular step the user is currently on.
   *
   * @return a {@link RouteStepProgress} object.
   * @since 0.1.0
   */
  public RouteStepProgress getCurrentStepProgress() {
    return currentStepProgress;
  }

  /**
   * Index representing the current step.
   *
   * @return an {@code integer} representing the current step the user is on.
   * @since 0.1.0
   */
  public int getStepIndex() {
    if (stepIndex < 0 || stepIndex > routeLeg.getSteps().size() - 1) {
      throw new ServicesException("RouteProgress step index is outside its index limit.");
    }
    return stepIndex;
  }

  /**
   * Total distance traveled in meters along current leg.
   *
   * @return a double value representing the total distance the user has traveled along the current leg, using unit
   * meters.
   * @since 0.1.0
   */
  public double getDistanceTraveled() {
    return legDistance - getDistanceRemaining();
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return {@code long} value representing the duration remaining till end of route, in unit seconds.
   * @since 0.1.0
   */
  public double getDistanceRemaining() {
    return RouteUtils.getDistanceToNextLeg(
      userSnappedPosition, routeLeg, TurfConstants.UNIT_METERS, Constants.PRECISION_6
    );
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public long getDurationRemaining() {
    return (long) ((1 - getFractionTraveled()) * routeLeg.getDuration());
  }

  /**
   * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the next leg (if another leg exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the current leg.
   * @since 0.1.0
   */
  public float getFractionTraveled() {
    return (float) (getDistanceTraveled() / legDistance);
  }

  /**
   * Get the previous step the user traversed along, if the user is still on the first step, this will return null.
   *
   * @return a {@link LegStep} representing the previous step the user was on, if still on first step in route, returns
   * null.
   * @since 0.1.0
   */
  public LegStep getPreviousStep() {
    if (getStepIndex() == 0) {
      return null;
    }
    return routeLeg.getSteps().get(getStepIndex() - 1);
  }

  /**
   * Returns the current step the user is traversing along.
   *
   * @return a {@link LegStep} representing the step the user is currently on.
   * @since 0.1.0
   */
  public LegStep getCurrentStep() {
    return routeLeg.getSteps().get(getStepIndex());
  }

  /**
   * Get the next/upcoming step immediately after the current step. If the user is on the last step on the last leg,
   * this will return null since a next step doesn't exist.
   *
   * @return a {@link LegStep} representing the next step the user will be on.
   * @since 0.1.0
   */
  public LegStep getUpComingStep() {
    if (routeLeg.getSteps().size() > getStepIndex()) {
      return routeLeg.getSteps().get(getStepIndex() + 1);
    }
    return null;
  }
}
