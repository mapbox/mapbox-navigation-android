package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.navigation.OfflineError;
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineRouteFoundCallback;

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
