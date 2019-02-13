package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh;
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 */
public final class RouteRefresh {
  private final RefreshCallback refreshCallback;
  private final RouteAnnotationUpdater routeAnnotationUpdater;
//  private final MapboxDirectionsRefresh mapboxDirectionsRefresh;

  /**
   * Creates a {@link RouteRefresh} object configured with the specified {@link RouteProgress}.
   *
   * @param routeProgress to extract a route and current leg index
   */
  public RouteRefresh(RefreshCallback refreshCallback) {
    this(refreshCallback, new RouteAnnotationUpdater());
  }

  RouteRefresh(RefreshCallback refreshCallback, RouteAnnotationUpdater routeAnnotationUpdater) {
    this.refreshCallback = refreshCallback;
  }

  /**
   * Refreshes the {@link DirectionsRoute} included in the {@link RouteProgress} and returns it
   * to the callback that was originally passed in. The client will then have to update their
   * {@link DirectionsRoute} with the {@link com.mapbox.api.directions.v5.models.LegAnnotation}s
   * returned in this response. The leg annotations start at the current leg index of the
   * {@link RouteProgress}
   *
   * @param routeProgress to refresh
   */
  public void refresh(RouteProgress routeProgress) {
    refresh(routeProgress.directionsRoute(), routeProgress.legIndex());
  }

  void refresh(final DirectionsRoute directionsRoute, final int legIndex) {
    MapboxDirectionsRefresh.builder()
      .requestId(directionsRoute.routeOptions().requestUuid())
      .routeIndex(Integer.valueOf(directionsRoute.routeIndex()))
      .legIndex(legIndex)
      .build().enqueueCall(new Callback<DirectionsRefreshResponse>() {
      @Override
      public void onResponse(Call<DirectionsRefreshResponse> call, Response<DirectionsRefreshResponse> response) {
        refreshCallback.onRefresh(routeAnnotationUpdater.update(directionsRoute,
          response.body().route(), legIndex));
      }

      @Override
      public void onFailure(Call<DirectionsRefreshResponse> call, Throwable throwable) {
        refreshCallback.onError("There was a network error.");
      }
    });
  }
}
