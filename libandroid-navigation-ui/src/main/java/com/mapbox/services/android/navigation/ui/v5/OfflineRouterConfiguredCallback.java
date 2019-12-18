package com.mapbox.services.android.navigation.ui.v5;


import androidx.annotation.NonNull;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.services.android.navigation.v5.navigation.OfflineError;
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineTilesConfiguredCallback;

class OfflineRouterConfiguredCallback implements OnOfflineTilesConfiguredCallback {

  private final NavigationViewOfflineRouter offlineRouter;

  OfflineRouterConfiguredCallback(NavigationViewOfflineRouter offlineRouter) {
    this.offlineRouter = offlineRouter;
  }

  @Override
  public void onConfigured(int numberOfTiles) {
    offlineRouter.setIsConfigured(true);
  }

  @Override
  public void onConfigurationError(@NonNull OfflineError error) {
    MapboxLogger.INSTANCE.e(new Message(error.getMessage()));
  }
}