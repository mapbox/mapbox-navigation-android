package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

public class MapboxOfflineNavigator {
  private OfflineNavigator offlineNavigator;

  public MapboxOfflineNavigator() {
    offlineNavigator = new OfflineNavigator(new Navigator());
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilesDirPath        directory path where the tiles are located
   * @param callback            a callback that will be fired when the offline data is initialized and
   *                            {@link MapboxOfflineNavigator#findOfflineRoute(OfflineRoute)} could
   *                            be called
   *                            safely
   */
  public void initializeOfflineData(String tilesDirPath, OnOfflineDataInitialized callback) {
    offlineNavigator.configure(tilesDirPath, callback);
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like JSON
   *
   * @param route the {@link OfflineRoute} to get a {@link DirectionsRoute} from
   * @return the offline {@link DirectionsRoute}
   */
  @Nullable
  public DirectionsRoute findOfflineRoute(@NonNull OfflineRoute route) {
    return retrieveOfflineRoute(route);
  }

  @Nullable
  private DirectionsRoute retrieveOfflineRoute(@NonNull OfflineRoute offlineRoute) {
    RouterResult response = offlineNavigator.retrieveRouteFor(offlineRoute);
    return offlineRoute.retrieveOfflineRoute(response);
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param srcPath where TAR file is located
   * @param destPath to the destination directory
   */
  synchronized void unpackTiles(String srcPath, String destPath) {
    offlineNavigator.unpackTiles(srcPath, destPath);
  }
}
