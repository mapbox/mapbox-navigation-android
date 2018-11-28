package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.Navigator;

class OfflineNavigator {
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
   * @param tilePath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, RouteFoundCallback)}
   *                 can be called safely
   */
  void configure(String tilePath, OnOfflineDataInitialized callback) {
    new ConfigureRouterTask(navigator, tilePath, callback).execute();
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
   *
   * @param offlineRoute an offline navigation route
   * @return a RouterResult object with the json and a success/fail bool
   */
  void retrieveRouteFor(OfflineRoute offlineRoute, RouteFoundCallback callback) {
    new OfflineRouteRetrievalTask(navigator, callback).execute(offlineRoute);
  }
}