package com.mapbox.services.android.navigation.v5.route;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Response;

/**
 * RouteEngineCallback to be passed into the constructor of {@link RouteEngine}.
 * <p>
 * Will fire when either a successful / failed response is received.
 */
public interface RouteEngineCallback {

  void onResponseReceived(Response<DirectionsResponse> response, RouteProgress routeProgress);

  void onErrorReceived(Throwable throwable);
}
