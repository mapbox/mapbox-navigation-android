package com.mapbox.navigation.ui;


import androidx.annotation.NonNull;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.services.android.navigation.v5.navigation.OfflineError;
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineRouteFoundCallback;

class OfflineRouteFoundCallback implements OnOfflineRouteFoundCallback {

  private final NavigationViewRouter router;

  OfflineRouteFoundCallback(NavigationViewRouter router) {
    this.router = router;
  }

  @Override
  public void onRouteFound(@NonNull Route offlineRoute) {
    router.updateCurrentRoute(offlineRoute);
    router.updateCallStatusReceived();
  }

  @Override
  public void onError(@NonNull OfflineError error) {
    router.onRequestError(error.getMessage());
    router.updateCallStatusReceived();
  }
}
