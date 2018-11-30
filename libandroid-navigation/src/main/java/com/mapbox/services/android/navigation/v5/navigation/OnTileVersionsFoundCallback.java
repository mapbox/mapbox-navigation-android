package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Callback used with {@link MapboxOfflineRouter#fetchAvailableTileVersions(String, OnTileVersionsFoundCallback)}.
 */
public interface OnTileVersionsFoundCallback {

  /**
   * A list of available offline tile versions that can be used
   * with {@link MapboxOfflineRouter#downloadTiles(OfflineTiles, RouteTileDownloadListener)}.
   *
   * @param availableVersions for offline tiles
   */
  void onVersionsFound(@NonNull List<String> availableVersions);


  /**
   * Called when an error has occurred fetching
   * offline versions.
   *
   * @param error with message explanation
   */
  void onError(@NonNull OfflineError error);
}
