package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

/**
 * Listener that needs to be added to
 * {@link MapboxOfflineRouter#configure(String, OnOfflineTilesConfiguredCallback)} to know when
 * offline data is initialized and
 * {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)} could be called.
 */
public interface OnOfflineTilesConfiguredCallback {

  /**
   * Called whe the offline data is initialized and
   * {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)}.
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
