package com.mapbox.services.android.navigation.v5.navigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

public class MapboxOfflineRouter {
  private final OfflineNavigator offlineNavigator;

  public MapboxOfflineRouter() {
    offlineNavigator = new OfflineNavigator(new Navigator());
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilesDirPath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxOfflineRouter#findOfflineRoute(OfflineRoute, RouteFoundCallback)}
   *                 can be called safely.
   */
  public void initializeOfflineData(String tilesDirPath, OnOfflineDataInitialized callback) {
    offlineNavigator.configure(tilesDirPath, callback);
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like JSON
   *
   * @param route the {@link OfflineRoute} to get a {@link DirectionsRoute} from
   * @param callback a callback to pass back the result
   * @return the offline {@link DirectionsRoute}
   */
  @Nullable
  public void findOfflineRoute(@NonNull OfflineRoute route,
                               RouteFoundCallback callback) {
    offlineNavigator.retrieveRouteFor(route, callback);
  }

  public void downloadTiles(Context context, OfflineTiles offlineTiles, RouteTileDownloadListener listener) {
    RouteTileDownloader routeTileDownloader = new RouteTileDownloader();
    routeTileDownloader.setListener(listener);
    if (ActivityCompat.checkSelfPermission(context,
      Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return;
    }
    routeTileDownloader.startDownload(offlineTiles);
  }
}
