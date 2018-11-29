package com.mapbox.services.android.navigation.v5.navigation;

/**
 * Listener that needs to be added to
 * {@link MapboxOfflineRouter#initializeOfflineData(String, OnOfflineDataInitialized)} to know when
 * offline data is initialized and
 * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, RouteFoundCallback)} could be called.
 */
public interface OnOfflineDataInitialized {

  /**
   * Will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, RouteFoundCallback)}
   * could be called safely.
   */
  void onOfflineDataInitialized(OfflineData offlineData);
}
