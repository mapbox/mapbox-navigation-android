package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.Navigator;

class OfflineNavigator {
  private final Navigator navigator;

  OfflineNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilePath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   *                 {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)}
   *                 can be called safely
   */
  void configure(String tilePath, OnOfflineTilesConfiguredCallback callback) {
    new ConfigureRouterTask(navigator, tilePath, callback).execute();
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
   *
   * @param offlineRoute an offline navigation route
   * @param callback     which receives a RouterResult object with the json and a success/fail bool
   */
  void retrieveRouteFor(OfflineRoute offlineRoute, OnOfflineRouteFoundCallback callback) {
    new OfflineRouteRetrievalTask(navigator, callback).execute(offlineRoute);
  }


  /**
   * Unpacks tar file into a specified destination path.
   *
   * @param tarPath         to find file to be unpacked
   * @param destinationPath where the tar will be unpacked
   */
  void unpackTiles(String tarPath, String destinationPath) {
    navigator.unpackTiles(tarPath, destinationPath);
  }
}