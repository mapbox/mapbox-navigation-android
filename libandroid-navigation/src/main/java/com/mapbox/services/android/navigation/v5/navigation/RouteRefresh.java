package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh;
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Callback;

/**
 *
 */
public final class RouteRefresh {
  private final MapboxDirectionsRefresh mapboxDirectionsRefresh;

  /**
   * Creates a {@link RouteRefresh} object configured with the specified {@link RouteProgress}.
   *
   * @param routeProgress to extract a route and current leg index
   */
  public RouteRefresh(RouteProgress routeProgress) {
    this(routeProgress.directionsRoute(), routeProgress.legIndex());

  }

  RouteRefresh(DirectionsRoute directionsRoute, int legIndex) {
    this(MapboxDirectionsRefresh.builder()
      .requestId(directionsRoute.routeOptions().requestUuid())
      .routeIndex(Integer.valueOf(directionsRoute.routeIndex()))
      .legIndex(legIndex)
      .build());
  }

  RouteRefresh(MapboxDirectionsRefresh mapboxDirectionsRefresh) {
    this.mapboxDirectionsRefresh = mapboxDirectionsRefresh;
  }

  /**
   * Refreshes the {@link DirectionsRoute} included in the {@link RouteProgress} and returns it
   * to the callback passed in. The client will then have to update their {@link DirectionsRoute}
   * with the {@link com.mapbox.api.directions.v5.models.LegAnnotation}s returned in this
   * response. The leg annotations start at the current leg index of the {@link RouteProgress},
   * so the client should keep
   *
   * @param callback to return {@link DirectionsRefreshResponse}
   */
  public void refresh(Callback<DirectionsRefreshResponse> callback) {
    mapboxDirectionsRefresh.enqueueCall(callback);
  }
}
