package com.mapbox.services.android.navigation.v5.route;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteEngine implements Callback<DirectionsResponse> {

  private Callback engineCallback;

  public RouteEngine(Callback engineCallback) {
    this.engineCallback = engineCallback;
  }

  public void fetchFasterRoute(Point origin, RouteOptions options) {
    if (options == null) {
      return;
    }

    NavigationRoute.builder()
      .origin(origin)
      .routeOptions(options)
      .build()
      .getRoute(this);
  }

  @Override
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    // Check for successful response
    if (!response.isSuccessful()) {
      return;
    }

    if (isFasterRoute(response.body())) {
      engineCallback.onFasterRouteFound(response.body().routes().get(0));
    }
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {

  }

  public interface Callback {
    void onFasterRouteFound(DirectionsRoute route);
  }

  private boolean isFasterRoute(DirectionsResponse response) {

    // TODO determine is faster route

    return false;
  }
}
