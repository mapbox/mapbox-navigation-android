package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;

/**
 * Listener that needs to be added to
 * <tt>MapboxOfflineRouter#configure(String, OnOfflineTilesConfiguredCallback)</tt> to know when
 * offline data is initialized and
 * <tt>MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)</tt> could be called.
 */
public interface OnOfflineTilesConfiguredCallback {

  /**
   * Called whe the offline data is initialized and
   * <tt>MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)</tt>.
   * could be called safely.
   *
   * @param numberOfTiles initialized in the path provided
   */
  void onConfigured(int numberOfTiles);

  /**
   * Called when an error has occurred configuring
   * the offline tile data.
   *
   * @param error with message explanation
   */
  void onConfigurationError(@NonNull OfflineError error);
}
