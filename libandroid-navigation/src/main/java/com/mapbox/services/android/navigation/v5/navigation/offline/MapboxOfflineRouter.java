package com.mapbox.services.android.navigation.v5.navigation.offline;

import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationLibraryLoader;
import com.mapbox.services.android.navigation.v5.navigation.OfflineRoute;
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineDataInitialized;

public class MapboxOfflineRouter {
  private static final String EMPTY_TRANSLATIONS_DIR_PATH = "";
  Navigator navigator;

  static {
    NavigationLibraryLoader.load();
  }

  public MapboxOfflineRouter() {
    this(new Navigator());
  }

  MapboxOfflineRouter(Navigator navigator) {
    this.navigator = navigator;
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param srcPath where TAR file is located
   * @param destPath to the destination directory
   */
  synchronized void unpackTiles(String srcPath, String destPath) {
    navigator.unpackTiles(srcPath, destPath);
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilesPath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxNavigation#findOfflineRoute(OfflineRoute)} could be called safely
   */
  public synchronized void configure(String tilesPath, OnOfflineDataInitialized callback) {
    new ConfigureRouterTask(navigator, tilesPath, EMPTY_TRANSLATIONS_DIR_PATH, callback).execute();
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
   *
   * @param offlineRoute an offline navigation route
   * @return a RouterResult object with the json and a success/fail bool
   */
  public synchronized RouterResult retrieveRouteFor(OfflineRoute offlineRoute) {
    String offlineUri = offlineRoute.buildUrl();
    return navigator.getRoute(offlineUri);
  }
}
