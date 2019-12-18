package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineRouter;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.OfflineRoute;

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
      MapboxLogger.INSTANCE.e(new Message("Cannot find route - offline router is not configured"));
      return;
    }

    OfflineRoute offlineRoute = OfflineRoute.builder(builder).build();
    offlineRouter.findRoute(offlineRoute, new OfflineRouteFoundCallback(router));
  }

  private boolean isNew(String tileVersion) {
    return !this.tileVersion.equals(tileVersion);
  }
}