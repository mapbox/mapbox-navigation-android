package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Callback used with <tt>MapboxOfflineRouter#fetchAvailableTileVersions(String, OnTileVersionsFoundCallback)</tt>.
 */
public interface OnTileVersionsFoundCallback {

  /**
   * A list of available offline tile versions that can be used
   * with <tt>MapboxOfflineRouter#downloadTiles(OfflineTiles, RouteTileDownloadListener)</tt>.
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
