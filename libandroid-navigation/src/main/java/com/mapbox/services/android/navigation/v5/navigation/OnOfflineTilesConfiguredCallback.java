package com.mapbox.services.android.navigation.v5.navigation;

/**
 * Listener that needs to be added to
 * {@link MapboxOfflineRouter#configure(String, OnOfflineTilesConfiguredCallback)} to know when
 * offline data is initialized and
 * {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)} could be called.
 */
public interface OnOfflineTilesConfiguredCallback {

  /**
   * Will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)}
   * could be called safely.
   */
  void onOfflineDataInitialized(OfflineData offlineData);
}
