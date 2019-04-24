package com.mapbox.services.android.navigation.v5.navigation;

import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import timber.log.Timber;

/**
 * This class allows the developer to interact with the Directions Refresh API, receiving updated
 * annotations for a route previously requested with the enableRefresh flag.
 */
public final class RouteRefresh {

  private static final String INVALID_DIRECTIONS_ROUTE = "RouteProgress passed has invalid DirectionsRoute";
  private final String accessToken;
  private RefreshCallback refreshCallback;

  /**
   * Creates a {@link RouteRefresh} object which calls the {@link RefreshCallback} with updated
   * routes.
   *
   * @param accessToken     mapbox access token
   * @param refreshCallback to call with updated routes
   * @deprecated use {@link RouteRefresh(String)} instead
   */
  @Deprecated
  public RouteRefresh(String accessToken, RefreshCallback refreshCallback) {
    this.accessToken = accessToken;
    this.refreshCallback = refreshCallback;
  }

  /**
   * Creates a {@link RouteRefresh} object.
   *
   * @param accessToken mapbox access token
   */
  public RouteRefresh(String accessToken) {
    this.accessToken = accessToken;
  }

  /**
   * Refreshes the {@link DirectionsRoute} included in the {@link RouteProgress} and returns it
   * to the callback that was originally passed in. The client will then have to update their
   * {@link DirectionsRoute} with the {@link com.mapbox.api.directions.v5.models.LegAnnotation}s
   * returned in this response. The leg annotations start at the current leg index of the
   * {@link RouteProgress}
   * <p>
   * Note that if {@link RefreshCallback} is not passed in {@link RouteRefresh(String, RefreshCallback)} this call
   * will be ignored.
   * </p>
   *
   * @param routeProgress to refresh via the route and current leg index
   * @deprecated use {@link RouteRefresh#refresh(RouteProgress, RefreshCallback)} instead
   */
  @Deprecated
  public void refresh(RouteProgress routeProgress) {
    if (refreshCallback == null) {
      Timber.e("RefreshCallback cannot be null.");
      return;
    }
    refresh(routeProgress.directionsRoute(), routeProgress.legIndex(), refreshCallback);
  }

  /**
   * Refreshes the {@link DirectionsRoute} included in the {@link RouteProgress} and returns it
   * to the callback that was originally passed in. The client will then have to update their
   * {@link DirectionsRoute} with the {@link com.mapbox.api.directions.v5.models.LegAnnotation}s
   * returned in this response. The leg annotations start at the current leg index of the
   * {@link RouteProgress}
   *
   * @param routeProgress   to refresh via the route and current leg index
   * @param refreshCallback to call with updated routes
   */
  public void refresh(RouteProgress routeProgress, RefreshCallback refreshCallback) {
    refresh(routeProgress.directionsRoute(), routeProgress.legIndex(), refreshCallback);
  }

  private void refresh(final DirectionsRoute directionsRoute, final int legIndex, RefreshCallback refreshCallback) {
    if (isInvalid(directionsRoute, refreshCallback)) {
      return;
    }
    MapboxDirectionsRefresh.builder()
      .requestId(directionsRoute.routeOptions().requestUuid())
      .routeIndex(Integer.valueOf(directionsRoute.routeIndex()))
      .legIndex(legIndex)
      .accessToken(accessToken)
      .build().enqueueCall(new RouteRefreshCallback(directionsRoute, legIndex, refreshCallback));
  }

  private boolean isInvalid(DirectionsRoute directionsRoute, RefreshCallback refreshCallback) {
    String requestUuid = directionsRoute.routeOptions().requestUuid();
    if (TextUtils.isEmpty(requestUuid) || directionsRoute.routeIndex() == null) {
      refreshCallback.onError(new RefreshError(INVALID_DIRECTIONS_ROUTE));
      return true;
    }
    return false;
  }
}
