package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import timber.log.Timber;

class NavigationOfflineDatabaseCallback implements OfflineDatabaseLoadedCallback {

  private final MapboxNavigation navigation;
  private final MapOfflineManager mapOfflineManager;

  NavigationOfflineDatabaseCallback(MapboxNavigation navigation, MapOfflineManager mapOfflineManager) {
    this.navigation = navigation;
    this.mapOfflineManager = mapOfflineManager;
  }

  @Override
  public void onComplete() {
    // TODO Remove debug log after testing
    Timber.d("NavigationOfflineDatabaseCallback#onComplete");
    navigation.addProgressChangeListener(mapOfflineManager);
  }

  @Override
  public void onError(String error) {
    // TODO Remove debug log after testing
    Timber.d("NavigationOfflineDatabaseCallback#onError %s", error);
    Timber.e(error);
  }
}