package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.v5.navigation.OnOfflineTilesConfiguredCallback;

class OfflineRouterConfiguredCallback implements OnOfflineTilesConfiguredCallback {

  private final NavigationViewOfflineRouter offlineRouter;

  OfflineRouterConfiguredCallback(NavigationViewOfflineRouter offlineRouter) {
    this.offlineRouter = offlineRouter;
  }

  @Override
  public void onConfigured() {
    offlineRouter.setIsConfigured(true);
  }
}