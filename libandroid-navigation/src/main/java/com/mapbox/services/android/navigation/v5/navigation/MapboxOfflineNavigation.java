package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;

public class MapboxOfflineNavigation {
  private OfflineNavigator offlineNavigator;

  public MapboxOfflineNavigation() {
    offlineNavigator = new OfflineNavigator(new Navigator());
  }

  /**
   * Configures the navigator for getting offline routes
   *
   * @param tilesDirPath directory path where the tiles are located
   * @param callback a callback that will be fired when the offline data is initialized and
   * {@link MapboxOfflineNavigation#findOfflineRoute(OfflineRoute, CallbackAsyncTask.Callback)}
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
                               CallbackAsyncTask.Callback<DirectionsRoute> callback) {
    offlineNavigator.retrieveRouteFor(route, callback);
  }
}