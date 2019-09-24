package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class NavigationRouteCallback implements Callback<DirectionsResponse> {

  private final NavigationTelemetry telemetry;
  private final NavigationRouteEventListener listener;
  private final Callback<DirectionsResponse> callback;

  NavigationRouteCallback(NavigationRouteEventListener listener, Callback<DirectionsResponse> callback) {
    this(NavigationTelemetry.getInstance(), listener, callback);
  }

  NavigationRouteCallback(NavigationTelemetry telemetry, NavigationRouteEventListener listener,
                          Callback<DirectionsResponse> callback) {
    this.telemetry = telemetry;
    this.listener = listener;
    this.callback = callback;
  }

  @Override
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    callback.onResponse(call, response);
    if (isValid(response)) {
      String uuid = response.body().uuid();
      sendEventWith(listener.getTime(), uuid);
    }
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
    callback.onFailure(call, throwable);
  }

  private boolean isValid(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }

  private void sendEventWith(ElapsedTime time, String uuid) {
    telemetry.routeRetrievalEvent(time, uuid);
  }
}