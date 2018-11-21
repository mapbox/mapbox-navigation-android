package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.Navigator;

class OfflineNavigator {
  private static final String EMPTY_TRANSLATIONS_DIR_PATH = "";
  private final Navigator navigator;

  static {
    NavigationLibraryLoader.load();
  }

  OfflineNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilesPath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, OfflineRouteFoundCallback)}
   *                 can be called safely
   */
  void configure(String tilesPath, OnOfflineDataInitialized callback) {
    new ConfigureRouterTask(navigator, tilesPath, EMPTY_TRANSLATIONS_DIR_PATH, callback).execute();
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
   *
   * @param offlineRoute an offline navigation route
   * @return a RouterResult object with the json and a success/fail bool
   */
  void retrieveRouteFor(OfflineRoute offlineRoute, OfflineRouteFoundCallback callback) {
    new OfflineRouteRetrievalTask(navigator, callback).execute(offlineRoute);
  }
}