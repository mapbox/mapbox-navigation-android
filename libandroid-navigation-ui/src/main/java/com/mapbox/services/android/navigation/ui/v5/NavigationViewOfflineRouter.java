package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineRouter;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.OfflineRoute;

import timber.log.Timber;

class NavigationViewOfflineRouter {

  private final MapboxOfflineRouter offlineRouter;
  private final NavigationViewRouter router;
  private boolean isConfigured;
  private String tileVersion;

  NavigationViewOfflineRouter(MapboxOfflineRouter offlineRouter, NavigationViewRouter router) {
    this.offlineRouter = offlineRouter;
    this.router = router;
  }

  void configure(String tileVersion) {
    if (!isConfigured || isNew(tileVersion)) {
      offlineRouter.configure(tileVersion, new OfflineRouterConfiguredCallback(this));
    }
    this.tileVersion = tileVersion;
  }

  void setIsConfigured(boolean isConfigured) {
    this.isConfigured = isConfigured;
  }

  boolean isConfigured() {
    return isConfigured;
  }

  void findRouteWith(NavigationRoute.Builder builder) {
    if (!isConfigured) {
      Timber.e("Cannot find route - offline router is not configured");
      return;
    }

    OfflineRoute offlineRoute = OfflineRoute.builder(builder).build();
    offlineRouter.findRoute(offlineRoute, new OfflineRouteFoundCallback(router));
  }

  private boolean isNew(String tileVersion) {
    return !this.tileVersion.equals(tileVersion);
  }
}