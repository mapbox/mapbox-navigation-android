package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * This class allows the developer to interact with the Directions Refresh API, receiving updated
 * annotations for a route previously requested with the enableRefresh flag.
 */
public final class RouteRefresh {
  private final RefreshCallback refreshCallback;
  private final String accessToken;

  /**
   * Creates a {@link RouteRefresh} object which calls the {@link RefreshCallback} with updated
   * routes.
   *
   * @param accessToken mapbox access token
   * @param refreshCallback to call with updated routes
   */
  public RouteRefresh(String accessToken, RefreshCallback refreshCallback) {
    this.accessToken = accessToken;
    this.refreshCallback = refreshCallback;
  }

  /**
   * Refreshes the {@link DirectionsRoute} included in the {@link RouteProgress} and returns it
   * to the callback that was originally passed in. The client will then have to update their
   * {@link DirectionsRoute} with the {@link com.mapbox.api.directions.v5.models.LegAnnotation}s
   * returned in this response. The leg annotations start at the current leg index of the
   * {@link RouteProgress}
   *
   * @param routeProgress to refresh via the route and current leg index
   */
  public void refresh(RouteProgress routeProgress) {
    refresh(routeProgress.directionsRoute(), routeProgress.legIndex());
  }

  private void refresh(final DirectionsRoute directionsRoute, final int legIndex) {
    MapboxDirectionsRefresh.builder()
      .requestId(directionsRoute.routeOptions().requestUuid())
      .routeIndex(Integer.valueOf(directionsRoute.routeIndex()))
      .legIndex(legIndex)
      .accessToken(accessToken)
      .build().enqueueCall(new RouteRefreshCallback(directionsRoute, legIndex, refreshCallback));
  }
}
