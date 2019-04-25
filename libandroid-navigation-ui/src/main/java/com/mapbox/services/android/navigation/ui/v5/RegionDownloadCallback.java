package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.Mapbox;

import timber.log.Timber;

class RegionDownloadCallback implements OfflineRegionDownloadCallback {

  @Override
  public void onComplete() {
    // TODO good to go?
    // TODO Remove debug log after testing
    Mapbox.setConnected(false);
    Timber.d("onComplete!");
  }

  @Override
  public void onError(String error) {
    // TODO fail silently?
    // TODO Remove debug log after testing
    Mapbox.setConnected(false);
    Timber.d("onError %s", error);
  }
}
