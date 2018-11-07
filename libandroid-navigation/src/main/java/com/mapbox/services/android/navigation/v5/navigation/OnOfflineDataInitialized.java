package com.mapbox.services.android.navigation.v5.navigation;

/**
 * Listener that needs to be added to
 * {@link MapboxNavigation#initializeOfflineData(String, String, OnOfflineDataInitialized)} to know when offline data
 * is initialized and {@link MapboxNavigation#findOfflineRoute(OfflineRoute)} could be called.
 */
public interface OnOfflineDataInitialized {

  /**
   * Will be fired when the offline data is initialized and {@link MapboxNavigation#findOfflineRoute(OfflineRoute)}
   * could be called safely.
   */
  void onOfflineDataInitialized();
}
