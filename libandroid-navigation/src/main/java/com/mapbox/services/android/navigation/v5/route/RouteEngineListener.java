package com.mapbox.services.android.navigation.v5.route;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Response;

/**
 * RouteEngineListener to added to a {@link RouteEngine}.
 * <p>
 * Will fire when either a successful / failed response is received.
 */
public interface RouteEngineListener {

  void onResponseReceived(Response<DirectionsResponse> response, @Nullable RouteProgress routeProgress);

  void onErrorReceived(Throwable throwable);
}
