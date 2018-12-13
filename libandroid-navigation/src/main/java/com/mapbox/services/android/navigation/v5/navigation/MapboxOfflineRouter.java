package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

import java.io.File;

/**
 * Class used for offline routing.
 */
public class MapboxOfflineRouter {

  static {
    NavigationLibraryLoader.load();
  }

  private static final String TILE_PATH_NAME = "tiles";
  private final String tilePath;
  private final OfflineNavigator offlineNavigator;
  private final OfflineTileVersions offlineTileVersions;

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
    offlineTileVersions = new OfflineTileVersions();
  }

  // Package private (no modifier) for testing purposes
  MapboxOfflineRouter(String tilePath, OfflineNavigator offlineNavigator, OfflineTileVersions offlineTileVersions) {
    this.tilePath = tilePath;
    this.offlineNavigator = offlineNavigator;
    this.offlineTileVersions = offlineTileVersions;
  }

  /**
   * Configures the navigator for getting offline routes.
   *
   * @param version  version of offline tiles to use
   * @param callback a callback that will be fired when the offline data is configured and
   *                 {@link MapboxOfflineRouter#findRoute(OfflineRoute, OnOfflineRouteFoundCallback)}
   *                 can be called safely
   */
  public void configure(String version, OnOfflineTilesConfiguredCallback callback) {
    offlineNavigator.configure(new File(tilePath, version).getAbsolutePath(), callback);
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like JSON.
   *
   * @param route    the {@link OfflineRoute} to get a {@link DirectionsRoute} from
   * @param callback a callback to pass back the result
   */
  public void findRoute(@NonNull OfflineRoute route, OnOfflineRouteFoundCallback callback) {
    offlineNavigator.retrieveRouteFor(route, callback);
  }

  /**
   * Starts the download of tiles specified by the provided {@link OfflineTiles} object.
   *
   * @param offlineTiles object specifying parameters for the tile request
   * @param listener     which is updated on error, on progress update and on completion
   */
  public void downloadTiles(OfflineTiles offlineTiles, RouteTileDownloadListener listener) {
    new RouteTileDownloader(offlineNavigator, tilePath, listener).startDownload(offlineTiles);
  }

  /**
   * Call this method to fetch the latest available offline tile versions that
   * can be used with {@link MapboxOfflineRouter#downloadTiles(OfflineTiles, RouteTileDownloadListener)}.
   *
   * @param accessToken Mapbox access token to call the version API
   * @param callback    with the available versions
   */
  public void fetchAvailableTileVersions(String accessToken, OnTileVersionsFoundCallback callback) {
    offlineTileVersions.fetchRouteTileVersions(accessToken, callback);
  }
}
