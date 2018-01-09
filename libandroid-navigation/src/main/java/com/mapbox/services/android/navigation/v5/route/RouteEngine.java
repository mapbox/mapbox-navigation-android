package com.mapbox.services.android.navigation.v5.route;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class can be used to fetch new routes given a {@link Point} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteEngine implements Callback<DirectionsResponse> {

  private Callback engineCallback;
  private RouteProgress routeProgress;

  public RouteEngine(Callback engineCallback) {
    this.engineCallback = engineCallback;
  }

  public void fetchRoute(Point origin, RouteProgress routeProgress) {
    if (routeProgress == null) {
      return;
    } else {
      this.routeProgress = routeProgress;
    }

    // Build new route request with current route options
    RouteOptions currentOptions = routeProgress.directionsRoute().routeOptions();
    NavigationRoute.builder()
      .origin(origin)
      .routeOptions(currentOptions) // TODO Route options should have waypoints
      .build()
      .getRoute(this);
  }

  @Override
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    // Check for successful response
    if (!response.isSuccessful()) {
      return;
    }
    engineCallback.onResponseReceived(response, routeProgress);
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
    // No-op - fail silently
  }

  public interface Callback {
    void onResponseReceived(Response<DirectionsResponse> response, RouteProgress routeProgress);
  }
}
