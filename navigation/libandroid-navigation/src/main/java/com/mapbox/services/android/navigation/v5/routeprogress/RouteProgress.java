package com.mapbox.services.android.navigation.v5.routeprogress;

import android.location.Location;

import com.google.auto.value.AutoValue;
import com.mapbox.services.Constants;
import com.mapbox.services.Experimental;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;

import static com.mapbox.services.Constants.PRECISION_6;

/**
 * The {@code routeProgress} class contains all progress information of user along the route, leg and step.
 * <p>
 * You can use this together with MapboxNavigation to obtain this object from the
 * {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener}
 * or the {@link ProgressChangeListener}. This object is immutable
 * and a new, updated routeProgress object will be provided with each new location update.
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 0.1.0
 */
@Experimental
@AutoValue
public abstract class RouteProgress {

  public abstract RouteLegProgress currentLegProgress();

  public abstract DirectionsRoute route();

  public abstract Location location();

  public abstract int legIndex();

  /**
   * Constructor for the route routeProgress information.
   *
   * @param route     the {@link DirectionsRoute} being used for the navigation session. When a user is
   *                  rerouted this route is updated
   * @param location  the users raw location not adjusted for snapping
   * @param stepIndex an {@code integer} representing the current step index the user is on
   * @since 0.1.0
   */
  public static RouteProgress create(
    DirectionsRoute route, Location location, int legIndex, int stepIndex) {
    RouteLegProgress routeLegProgress
      = RouteLegProgress.create(route.getLegs().get(legIndex), stepIndex, userSnappedToRoutePosition(location,
      legIndex, stepIndex, route));
    return new AutoValue_RouteProgress(routeLegProgress, route, location, legIndex);
  }

  /**
   * Gives a {@link RouteLegProgress} object with information about the particular leg the user is currently on.
   *
   * @return a {@link RouteLegProgress} object.
   * @since 0.1.0
   */
  public RouteLegProgress getCurrentLegProgress() {
    return currentLegProgress();
  }

  /**
   * Index representing the current leg.
   *
   * @return an {@code integer} representing the current leg the user is on.
   * @since 0.1.0
   */
  public int getLegIndex() {
    return legIndex();
  }

  /**
   * Provides the current {@link RouteLeg} the user is on.
   *
   * @return a {@link RouteLeg} the user is currently on.
   */
  public RouteLeg getCurrentLeg() {
    return route().getLegs().get(getLegIndex());
  }

  /**
   * Total distance traveled in meters along route.
   *
   * @return a double value representing the total distance the user has traveled along the route, using unit meters.
   * @since 0.1.0
   */
  public double getDistanceTraveled() {
    double distanceTraveled = route().getDistance() - getDistanceRemaining();
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
  public double getDurationRemaining() {
    return (1 - getFractionTraveled()) * route().getDuration();
  }

  /**
   * Get the fraction traveled along the current route, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the end of the route.
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the route.
   * @since 0.1.0
   */
  public float getFractionTraveled() {
    float fractionRemaining = 1;

    if (route().getDistance() > 0) {
      fractionRemaining = (float) (getDistanceTraveled() / route().getDistance());
    }
    return fractionRemaining;
  }

  /**
   * Provides the distance remaining in meters till the user reaches the end of the route.
   *
   * @return {@code long} value representing the distance remaining till end of route, in unit meters.
   * @since 0.1.0
   */
  public double getDistanceRemaining() {
    double distanceRemaining = 0;
    List<Position> coords = PolylineUtils.decode(getCurrentLegProgress().getCurrentStep().getGeometry(),
      Constants.PRECISION_6);
    if (coords.size() > 1) {
      LineString slicedLine = TurfMisc.lineSlice(
        Point.fromCoordinates(userSnappedToRoutePosition(location(), legIndex(), currentLegProgress().stepIndex(),
          route())),
        Point.fromCoordinates(coords.get(coords.size() - 1)),
        LineString.fromCoordinates(coords)
      );
      distanceRemaining += TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
    }
    for (int i = currentLegProgress().getStepIndex() + 1; i < getCurrentLeg().getSteps().size(); i++) {
      distanceRemaining += getCurrentLeg().getSteps().get(i).getDistance();
    }

    // Add any additional leg distances the user hasn't navigated to yet.
    if (route().getLegs().size() - 1 > legIndex()) {
      for (int i = legIndex() + 1; i < route().getLegs().size(); i++) {
        distanceRemaining += route().getLegs().get(i).getDistance();
      }
    }
    return distanceRemaining;
  }

  /**
   * Get the route the navigation session is currently using.
   *
   * @return a {@link DirectionsRoute} currently being used for the navigation session.
   * @since 0.1.0
   */
  public DirectionsRoute getRoute() {
    return route();
  }

  // Always get the closest position on the route to the actual
  // raw location so that can accurately calculate values.
  private static Position userSnappedToRoutePosition(Location location, int legIndex, int stepIndex,
                                                     DirectionsRoute route) {
    Point locationToPoint = Point.fromCoordinates(
      new double[] {location.getLongitude(), location.getLatitude()}
    );

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(route.getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry(),
      PRECISION_6);

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
    return ((Point) feature.getGeometry()).getCoordinates();
  }
}
