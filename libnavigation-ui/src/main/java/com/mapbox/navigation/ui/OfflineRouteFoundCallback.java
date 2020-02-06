package com.mapbox.navigation.ui;


import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.navigation.OfflineError;
import com.mapbox.navigation.ui.navigation.OnOfflineRouteFoundCallback;

class OfflineRouteFoundCallback implements OnOfflineRouteFoundCallback {

  private final NavigationViewRouter router;

  OfflineRouteFoundCallback(NavigationViewRouter router) {
    this.router = router;
  }

  @Override
  public void onRouteFound(@NonNull DirectionsRoute offlineRoute) {
    router.updateCurrentRoute(offlineRoute);
    router.updateCallStatusReceived();
  }

  @Override
  public void onError(@NonNull OfflineError error) {
    router.onRequestError(error.getMessage());
    router.updateCallStatusReceived();
  }
}
