package com.mapbox.services.android.navigation.v5.routeprogress;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

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
   * Build a new {@link RouteProgress} object.
   *
   * @return a {@link Builder} object for creating this object
   * @since 0.7.0
   */
  public static Builder builder() {
    return new AutoValue_RouteProgress.Builder();
  }

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
  @Nullable
  public abstract RouteLegProgress currentLegProgress();

  /**
   * Provides the previous steps geometry as points along the route the users currently traversing
   * along. If the users at the very first step along the route, this will return null, otherwise a
   * value should always be given.
   *
   * @return either a list of the points making up the previous geometry or null if the user's
   * currently on the first step along the route
   * @since 0.7.0
   */
  @Nullable
  public abstract List<Point> priorStepCoordinates();

  /**
   * The current step's geometry found along the route. Since a step requires at least a geometry
   * made up of one {@link Point}, this should never return null.
   *
   * @return a list of points making up the current step geometry
   */
  @NonNull
  public abstract List<Point> currentStepCoordinates();

  /**
   * The next step's geometry found along the route. If the user is already on the last step, this
   * will return null.
   *
   * @return either a list of the points making up the upcoming step geometry or null if there isn't
   * an upcoming step
   * @since 0.7.0
   */
  @Nullable
  public abstract List<Point> upcomingStepCoordinates();

  /**
   * Will return a list of {@link Point}s which includes the prior, current, and upcoming points in
   * a single list.
   *
   * @return a list of points making up the prior, current, and upcoming points
   * @since 0.7.0
   */
  @NonNull
  public List<Point> nearbyCoordinates() {
    List<Point> nearbyCoordinates = new ArrayList<>();
    if (priorStepCoordinates() != null) {
      nearbyCoordinates.addAll(priorStepCoordinates());
    }
    nearbyCoordinates.addAll(currentStepCoordinates());
    if (upcomingStepCoordinates() != null) {
      nearbyCoordinates.addAll(upcomingStepCoordinates());
    }
    return nearbyCoordinates;
  }

  /**
   * Provides a new instance of the {@link RouteProgress.Builder} with the default values for each
   * field being the current {@link RouteProgress} instance values. This allows for easily changing
   * a single value in the Route Progress object and then quickly creating a new immutable instance
   * of this class.
   *
   * @return a new instance of {@link RouteProgress.Builder} with the values already set to the
   * current {@link RouteProgress} instance
   * @since 0.7.0
   */
  public abstract Builder toBuilder();

  abstract double stepDistanceRemaining();

  abstract double legDistanceRemaining();

  abstract int stepIndex();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    abstract DirectionsRoute directionsRoute();

    abstract int legIndex();

    public abstract Builder legIndex(int legIndex);

    public abstract Builder distanceRemaining(double distanceRemaining);

    public abstract Builder stepIndex(int stepIndex);

    public abstract Builder legDistanceRemaining(double legDistanceRemaining);

    public abstract Builder stepDistanceRemaining(double stepDistanceRemaining);

    public abstract Builder currentLegProgress(RouteLegProgress routeLegProgress);

    public abstract Builder priorStepCoordinates(List<Point> priorStepCoordinates);

    public abstract Builder upcomingStepCoordinates(List<Point> upcomingStepCoordinates);

    public abstract Builder currentStepCoordinates(List<Point> currentStepCoordinates);

    abstract RouteProgress autoBuild(); // not public

    public RouteProgress build() {
      RouteProgress routeProgress = autoBuild();

      LegStep nextStep
        = routeProgress.stepIndex() == (directionsRoute().legs().get(legIndex()).steps().size() - 1)
        ? null
        : directionsRoute().legs().get(legIndex()).steps().get(routeProgress.stepIndex() + 1);

      RouteLegProgress legProgress = RouteLegProgress.builder()
        .currentStepProgress(RouteStepProgress.create(
          directionsRoute().legs().get(legIndex()).steps().get(routeProgress.stepIndex()), nextStep,
          routeProgress.stepDistanceRemaining()))
        .routeLeg(directionsRoute().legs().get(legIndex()))
        .stepIndex(routeProgress.stepIndex())
        .distanceRemaining(routeProgress.legDistanceRemaining())
        .build();
      currentLegProgress(legProgress);
      return autoBuild();
    }
  }
}