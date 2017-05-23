package com.mapbox.services.android.navigation.v5.models;


import android.support.annotation.NonNull;

import com.mapbox.services.Constants;
import com.mapbox.services.Experimental;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

@Experimental
public class RouteStepProgress {

  private LegStep step;
  private Position userSnappedPosition;
  private double stepDistance;

  /**
   * Constructor for the step progress.
   *
   * @param routeLeg            the current leg the user is on.
   * @param userSnappedPosition the users snapped location when routeProgress was last updated.
   * @since 0.1.0
   */
  RouteStepProgress(@NonNull RouteLeg routeLeg, int stepIndex, @NonNull Position userSnappedPosition) {
    this.userSnappedPosition = userSnappedPosition;
    this.step = routeLeg.getSteps().get(stepIndex);

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(step.getGeometry(), Constants.PRECISION_6);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(step.getManeuver().asPosition()),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );
    stepDistance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
  }

  /**
   * Returns distance user has traveled along current step in unit meters.
   *
   * @return double value representing the distance the user has traveled so far along the current step. Uses unit
   * meters.
   * @since 0.1.0
   */
  public double getDistanceTraveled() {
    return stepDistance - getDistanceRemaining();
  }

  /**
   * Total distance in meters from user to end of step.
   *
   * @return double value representing the distance the user has remaining till they reach the end of the current step.
   * Uses unit meters.
   * @since 0.1.0
   */
  public double getDistanceRemaining() {

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(step.getGeometry(), Constants.PRECISION_6);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(userSnappedPosition),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );
    return TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
  }

  /**
   * Get the fraction traveled along the current step, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the next step (if another step exist in route).
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the current step.
   * @since 0.1.0
   */
  public float getFractionTraveled() {
    return (float) (getDistanceTraveled() / stepDistance);
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the current step.
   *
   * @return {@code long} value representing the duration remaining till end of step, in unit seconds.
   * @since 0.1.0
   */
  public long getDurationRemaining() {
    return (long) ((1 - getFractionTraveled()) * step.getDuration());
  }


}
