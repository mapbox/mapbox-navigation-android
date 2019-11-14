package com.mapbox.services.android.navigation.v5.navigation.camera;

import android.location.Location;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * This class holds all information related to a route and a user's progress along the route. It
 * also provides useful information (screen configuration and target distance) which can be used to
 * make additional configuration changes to the map's camera.
 *
 * @since 0.10.0
 */
@AutoValue
public abstract class RouteInformation {

  /**
   * The current route the user is navigating along. This value will update when reroutes occur
   * and it will be null if the {@link RouteInformation} is generated from an update to route
   * progress or from an orientation change.
   * @return current route
   * @since 0.10.0
   */
  @Nullable
  public abstract DirectionsRoute route();

  /**
   * The user's current location along the route. This value will update when orientation changes
   * occur as well as when progress along a route changes.
   * @return current location
   * @since 0.10.0
   */
  @Nullable
  public abstract Location location();

  /**
   * The user's current progress along the route.
   * @return current progress along the route.
   * @since 0.10.0
   */
  @Nullable
  public abstract RouteProgress routeProgress();

  public static RouteInformation create(@Nullable DirectionsRoute route, @Nullable Location location,
                                        @Nullable RouteProgress routeProgress) {
    return new AutoValue_RouteInformation(route, location, routeProgress);
  }
}
