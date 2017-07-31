package com.mapbox.services.android.navigation.v5.routeprogress;

import android.location.Location;

import com.google.auto.value.AutoValue;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;

/**
 * The {@code routeProgress} class contains all progress information of user along the route, leg
 * and step.
 * <p>
 * You can use this together with MapboxNavigation to obtain this object from the
 * {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener}
 * or the {@link ProgressChangeListener}. This object is immutable and a new, updated routeProgress
 * object will be provided with each new location update.
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 0.1.0
 */
@AutoValue
public abstract class RouteProgress {

  abstract Location location();

  /**
   * Gives a {@link RouteLegProgress} object with information about the particular leg the user is currently on.
   *
   * @return a {@link RouteLegProgress} object.
   * @since 0.1.0
   */
  public abstract RouteLegProgress currentLegProgress();

  /**
   * Index representing the current leg.
   *
   * @return an {@code integer} representing the current leg the user is on.
   * @since 0.1.0
   */
  public abstract int legIndex();

  /**
   * Provides the current {@link RouteLeg} the user is on.
   *
   * @return a {@link RouteLeg} the user is currently on.
   */
  public RouteLeg currentLeg() {
    return directionsRoute().getLegs().get(legIndex());
  }

  /**
   * Total distance traveled in meters along route.
   *
   * @return a double value representing the total distance the user has traveled along the route, using unit meters.
   * @since 0.1.0
   */
  public double distanceTraveled() {
    double distanceTraveled = directionsRoute().getDistance() - distanceRemaining();
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
  public double durationRemaining() {
    return (1 - fractionTraveled()) * directionsRoute().getDuration();
  }

  /**
   * Get the fraction traveled along the current route, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the end of the route.
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the route.
   * @since 0.1.0
   */
  public float fractionTraveled() {
    float fractionRemaining = 1;

    if (directionsRoute().getDistance() > 0) {
      fractionRemaining = (float) (distanceTraveled() / directionsRoute().getDistance());
    }
    return fractionRemaining;
  }

  /**
   * Provides the distance remaining in meters till the user reaches the end of the route.
   *
   * @return {@code long} value representing the distance remaining till end of route, in unit meters.
   * @since 0.1.0
   */
  public abstract double distanceRemaining();

  /**
   * Get the route the navigation session is currently using.
   *
   * @return a {@link DirectionsRoute} currently being used for the navigation session.
   * @since 0.1.0
   */
  public abstract DirectionsRoute directionsRoute();

  @AutoValue.Builder
  public abstract static class Builder {

    private int stepIndex;
    private double legDistanceRemaining;
    private double stepDistanceRemaining;

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder location(Location location);

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

    abstract DirectionsRoute directionsRoute();

    abstract int legIndex();

    abstract RouteProgress autoBuild(); // not public

    public RouteProgress build() {
      RouteLegProgress legProgress = RouteLegProgress.builder()
        .distanceRemaining(legDistanceRemaining)
        .stepIndex(stepIndex)
        .stepDistanceRemaining(stepDistanceRemaining)
        .routeLeg(directionsRoute().getLegs().get(legIndex()))
        .build();
      currentLegProgress(legProgress);
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_RouteProgress.Builder();
  }
}