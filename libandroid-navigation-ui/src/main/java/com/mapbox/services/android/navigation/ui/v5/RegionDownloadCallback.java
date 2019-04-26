package com.mapbox.services.android.navigation.ui.v5;

import timber.log.Timber;

class RegionDownloadCallback implements OfflineRegionDownloadCallback {

  private static final Boolean DISCONNECT_STATE = false;
  private final MapConnectivityController connectivityController;

  RegionDownloadCallback(MapConnectivityController connectivityController) {
    this.connectivityController = connectivityController;
  }

  @Override
  public void onComplete() {
    // TODO good to go?
    // TODO Remove debug log after testing
    connectivityController.assign(DISCONNECT_STATE);
    Timber.d("onComplete!");
  }

  @Override
  public void onError(String error) {
    // TODO fail silently?
    // TODO Remove debug log after testing
    connectivityController.assign(DISCONNECT_STATE);
    Timber.d("onError %s", error);
  }
}
