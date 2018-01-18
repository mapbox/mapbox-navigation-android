package com.mapbox.services.android.navigation.v5.navigation.camera;

import android.content.res.Configuration;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * This class holds all information related to a route and a user's progress along the route. It
 * also provides useful information (screen configuration and target distance) which can be used to
 * make additional configuration changes to the map's camera.
 *
 * @since 0.8.1
 */
@AutoValue
public abstract class RouteInformation {

  /**
   * The device's current configuration which can be used to return different zoom values given
   * it's orientation.
   * @return device's current configuration
   * @since 0.8.1
   */
  @NonNull
  public abstract Configuration configuration();

  /**
   * The camera target distance as a percentage of the total phone screen the view uses.
   * @return camera target distance
   * @since 0.8.1
   */
  public abstract double targetDistance();

  /**
   * The current route the user is navigating along. This value will update when reroutes occur
   * and it will be null if the {@link RouteInformation} is generated from an update to route
   * progress or from an orientation change.
   * @return current route
   * @since 0.8.1
   */
  @Nullable
  public abstract DirectionsRoute route();

  /**
   * The user's current location along the route. This value will update when orientation changes
   * occur as well as when progress along a route changes.
   * @return current location
   * @since 0.8.1
   */
  @Nullable
  public abstract Location location();

  /**
   * The user's current progress along the route.
   * @return current progress along the route.
   * @since 0.8.1
   */
  @Nullable
  public abstract RouteProgress routeProgress();

  public static RouteInformation create(@NonNull Configuration configuration, double targetDistance,
                                        @Nullable DirectionsRoute route, @Nullable Location location,
                                        @Nullable RouteProgress routeProgress) {
    return new AutoValue_RouteInformation(configuration, targetDistance, route, location,
            routeProgress);
  }
}
