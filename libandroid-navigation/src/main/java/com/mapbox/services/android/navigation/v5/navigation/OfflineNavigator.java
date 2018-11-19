package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

class OfflineNavigator {
  private static final String EMPTY_TRANSLATIONS_DIR_PATH = "";
  private Navigator navigator;

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
   * {@link MapboxOfflineNavigator#findOfflineRoute(OfflineRoute)} could be called safely
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
  RouterResult retrieveRouteFor(OfflineRoute offlineRoute) {
    String offlineUri = offlineRoute.buildUrl();
    synchronized (this) {
      return navigator.getRoute(offlineUri);
    }
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param srcPath where TAR file is located
   * @param destPath to the destination directory
   */
  void unpackTiles(String srcPath, String destPath) {
    navigator.unpackTiles(srcPath, destPath);
  }
}
