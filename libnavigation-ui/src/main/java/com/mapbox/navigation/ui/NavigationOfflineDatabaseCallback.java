package com.mapbox.navigation.ui;

import com.mapbox.navigation.core.MapboxNavigation;

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
    navigation.registerRouteProgressObserver(mapOfflineManager);
  }

  @Override
  public void onError(String error) {
    Timber.e(error);
  }

  void onDestroy() {
    mapOfflineManager.onDestroy();
  }
}