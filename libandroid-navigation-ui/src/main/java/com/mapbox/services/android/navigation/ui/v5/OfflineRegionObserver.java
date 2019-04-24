package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

class OfflineRegionObserver implements OfflineRegion.OfflineRegionObserver {

  private final OfflineRegionDownloadCallback callback;

  OfflineRegionObserver(OfflineRegionDownloadCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onStatusChanged(OfflineRegionStatus status) {
    if (status.isComplete()) {
      callback.onComplete();
    }
  }

  @Override
  public void onError(OfflineRegionError error) {
    callback.onError(String.format("%s %s", error.getMessage(), error.getReason()));
  }

  @Override
  public void mapboxTileCountLimitExceeded(long limit) {
    callback.onError(String.format("Offline map tile limit reached %s", limit));
  }
}
