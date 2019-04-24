package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

class MergeOfflineRegionsCallback implements OfflineManager.MergeOfflineRegionsCallback {

  private final OfflineDatabaseLoadedCallback callback;

  MergeOfflineRegionsCallback(OfflineDatabaseLoadedCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onMerge(OfflineRegion[] offlineRegions) {
    callback.onComplete();
  }

  @Override
  public void onError(String error) {
    callback.onError(error);
  }
}
