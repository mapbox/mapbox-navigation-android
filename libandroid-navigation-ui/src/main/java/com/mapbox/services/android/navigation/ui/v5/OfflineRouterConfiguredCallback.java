package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.navigation.OfflineError;
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineTilesConfiguredCallback;

import timber.log.Timber;

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
    Timber.e(error.getMessage());
  }
}