package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import timber.log.Timber;

class NavigationFasterRouteListener implements RouteListener {

  private static final int FIRST_ROUTE = 0;

  private final NavigationEventDispatcher eventDispatcher;
  private final FasterRoute fasterRouteEngine;

  NavigationFasterRouteListener(NavigationEventDispatcher eventDispatcher, FasterRoute fasterRouteEngine) {
    this.eventDispatcher = eventDispatcher;
    this.fasterRouteEngine = fasterRouteEngine;
  }

  @Override
  public void onResponseReceived(DirectionsResponse response, @Nullable RouteProgress routeProgress) {
    if (fasterRouteEngine.isFasterRoute(response, routeProgress)) {
      eventDispatcher.onFasterRouteEvent(response.routes().get(FIRST_ROUTE));
    }
  }

  @Override
  public void onErrorReceived(Throwable throwable) {
    Timber.e(throwable);
  }
}
