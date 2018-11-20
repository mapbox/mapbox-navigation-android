package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

import java.io.File;

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
   * {@link MapboxOfflineNavigator#findOfflineRoute(OfflineRoute, CallbackAsyncTask.Callback)}
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
  void retrieveRouteFor(OfflineRoute offlineRoute, CallbackAsyncTask.Callback<DirectionsRoute> callback) {
    new OfflineRouteRetrievalTask(navigator, callback).execute(offlineRoute);
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param srcPath where TAR file is located
   * @param destPath to the destination directory
   */
  void unpackTiles(String srcPath, String destPath, UnpackUpdateTask.ProgressUpdateListener
    progressUpdateListener) {
    new UnpackerTask(navigator).execute(srcPath, destPath);
    new UnpackUpdateTask(progressUpdateListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new
      File(srcPath));
  }
}
