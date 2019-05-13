package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

class MergeOfflineRegionsCallback implements OfflineManager.MergeOfflineRegionsCallback {

  private OfflineDatabaseLoadedCallback callback;

  MergeOfflineRegionsCallback(OfflineDatabaseLoadedCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onMerge(OfflineRegion[] offlineRegions) {
    if (callback != null) {
      callback.onComplete();
    }
  }

  @Override
  public void onError(String error) {
    if (callback != null) {
      callback.onError(error);
    }
  }

  OfflineDatabaseLoadedCallback onDestroy() {
    callback = null;
    return callback;
  }
}
