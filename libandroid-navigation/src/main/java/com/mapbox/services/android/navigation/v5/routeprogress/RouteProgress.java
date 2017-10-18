package com.mapbox.services.android.navigation.v5.routeprogress;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;

/**
 * This class contains all progress information at any given time during a navigation session. This
 * progress includes information for the current route, leg and step the user is traversing along.
 * With every new valid location update, a new route progress will be generated using the latest
 * information.
 * <p>
 * The latest route progress object can be obtained through either the {@link ProgressChangeListener}
 * or the {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener} callbacks.
 * Note that the route progress object's immutable.
 * </p>
 *
 * @since 0.1.0
 */
@AutoValue
public abstract class RouteProgress {

  /**
   * Get the route the navigation session is currently using. When a reroute occurs and a new
   * directions route gets obtained, with the next location update this directions route should
   * reflect the new route. All direction route get passed in through
   * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation#startNavigation(DirectionsRoute)}.
   *
   * @return a {@link DirectionsRoute} currently being used for the navigation session
   * @since 0.1.0
   */
  public abstract DirectionsRoute directionsRoute();

  /**
   * Index representing the current leg the user is on. If the directions route currently in use
   * contains more then two waypoints, the route is likely to have multiple legs representing the
   * distance between the two points.
   *
   * @return an integer representing the current leg the user is on
   * @since 0.1.0
   */
  public abstract int legIndex();

  /**
   * Provides the current {@link RouteLeg} the user is on.
   *
   * @return a {@link RouteLeg} the user is currently on
   * @since 0.1.0
   */
  @NonNull
  public RouteLeg currentLeg() {
    return directionsRoute().legs().get(legIndex());
  }

  /**
   * Total distance traveled in meters along route.
   *
   * @return a double value representing the total distance the user has traveled along the route,
   * using unit meters
   * @since 0.1.0
   */
  public double distanceTraveled() {
    double distanceTraveled = directionsRoute().distance() - distanceRemaining();
    if (distanceTraveled < 0) {
      distanceTraveled = 0;
    }
    return distanceTraveled;
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return {@code long} value representing the duration remaining till end of route, in unit
   * seconds
   * @since 0.1.0
   */
  public double durationRemaining() {
    return (1 - fractionTraveled()) * directionsRoute().duration();
  }

  /**
   * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
   * isn't guaranteed to reach 1 before the user reaches the end of the route.
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the
   * route
   * @since 0.1.0
   */
  public float fractionTraveled() {
    float fractionRemaining = 1;

    if (directionsRoute().distance() > 0) {
      fractionRemaining = (float) (distanceTraveled() / directionsRoute().distance());
    }
    return fractionRemaining;
  }

  /**
   * Provides the distance remaining in meters till the user reaches the end of the route.
   *
   * @return {@code long} value representing the distance remaining till end of route, in unit meters
   * @since 0.1.0
   */
  public abstract double distanceRemaining();

  /**
   * Number of waypoints remaining on the current route.
   *
   * @return integer value representing the number of way points remaining along the route
   * @since 0.5.0
   */
  public int remainingWaypoints() {
    return directionsRoute().legs().size() - legIndex();
  }

  /**
   * Gives a {@link RouteLegProgress} object with information about the particular leg the user is
   * currently on.
   *
   * @return a {@link RouteLegProgress} object
   * @since 0.1.0
   */
  public abstract RouteLegProgress currentLegProgress();

  @AutoValue.Builder
  public abstract static class Builder {

    private int stepIndex;
    private double legDistanceRemaining;
    private double stepDistanceRemaining;

    abstract DirectionsRoute directionsRoute();

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    abstract int legIndex();

    public abstract Builder legIndex(int legIndex);

    public abstract Builder distanceRemaining(double distanceRemaining);

    public Builder stepIndex(int stepIndex) {
      this.stepIndex = stepIndex;
      return this;
    }

    public Builder legDistanceRemaining(double legDistanceRemaining) {
      this.legDistanceRemaining = legDistanceRemaining;
      return this;
    }

    public Builder stepDistanceRemaining(double stepDistanceRemaining) {
      this.stepDistanceRemaining = stepDistanceRemaining;
      return this;
    }

    abstract Builder currentLegProgress(RouteLegProgress routeLegProgress);

    abstract RouteProgress autoBuild(); // not public

    public RouteProgress build() {
      RouteLegProgress legProgress = RouteLegProgress.create(
        directionsRoute().legs().get(legIndex()),
        stepIndex,
        legDistanceRemaining,
        stepDistanceRemaining
      );
      currentLegProgress(legProgress);
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_RouteProgress.Builder();
  }
}