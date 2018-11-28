package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

import java.io.File;

public class MapboxOfflineRouter {
  private final String TILE_PATH_NAME = "tiles";
  private final OfflineNavigator offlineNavigator;
  private final String tilePath;

  /**
   * Creates an offline router which uses the specified offline path for storing and retrieving
   * data.
   *
   * @param offlinePath directory path where the offline data is located
   */
  public MapboxOfflineRouter(String offlinePath) {
    File tileDir = new File(offlinePath, TILE_PATH_NAME);
    if (!tileDir.exists()) {
      tileDir.mkdirs();
    }

    this.tilePath = tileDir.getAbsolutePath();
    offlineNavigator = new OfflineNavigator(new Navigator());
  }

  /**
   * Configures the navigator for getting offline routes.
   *
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, RouteFoundCallback)}
   *                 can be called safely.
   */
  public void initializeOfflineData(String version, OnOfflineDataInitialized callback) {
    offlineNavigator.configure(new File(tilePath, version).getAbsolutePath(), callback);
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like JSON
   *
   * @param route the {@link OfflineRoute} to get a {@link DirectionsRoute} from
   * @param callback a callback to pass back the result
   * @return the offline {@link DirectionsRoute}
   */
  @Nullable
  public void findOfflineRoute(@NonNull OfflineRoute route, RouteFoundCallback callback) {
    offlineNavigator.retrieveRouteFor(route, callback);
  }

  /**
   * Starts the download of tiles specified by the provided {@link OfflineTiles} object.
   *
   * @param offlineTiles object specifying parameters for the tile request
   * @param listener which is updated on error, on progress update and on completion
   */
  public void downloadTiles(OfflineTiles offlineTiles, RouteTileDownloadListener listener) {
    new RouteTileDownloader(tilePath, listener).startDownload(offlineTiles);
  }
}
