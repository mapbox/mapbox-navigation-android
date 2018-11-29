package com.mapbox.services.android.navigation.v5.navigation;

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
  void onVersionsFound(List<String> availableVersions);

  // TODO provide error object?
  void onError();
}
