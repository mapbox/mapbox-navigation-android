package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

import java.io.IOException;

import okhttp3.Callback;
import timber.log.Timber;

class RouteJsonCallback implements Callback {

  private final Navigator navigator;
  private final MapboxNavigation navigation;
  private final DirectionsRoute route;

  RouteJsonCallback(Navigator navigator, MapboxNavigation navigation, DirectionsRoute route) {
    this.navigator = navigator;
    this.navigation = navigation;
    this.route = route;
  }

  @Override
  public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException exception) {
    Timber.e(exception);
  }

  @Override
  public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response jsonResponse) throws IOException {
    String routeJson = jsonResponse.body().string();
    navigator.setDirections(routeJson);
    navigation.startNavigationWith(route);
  }
}