package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

class NavigationOfflineDatabaseCallback implements OfflineDatabaseLoadedCallback {

  private final MapboxNavigation navigation;
  private final MapOfflineManager mapOfflineManager;

  NavigationOfflineDatabaseCallback(MapboxNavigation navigation, MapOfflineManager mapOfflineManager) {
    this.navigation = navigation;
    this.mapOfflineManager = mapOfflineManager;
  }

  @Override
  public void onComplete() {
    navigation.addProgressChangeListener(mapOfflineManager);
  }

  @Override
  public void onError(String error) {
    MapboxLogger.INSTANCE.e(new Message(error));
  }

  void onDestroy() {
    mapOfflineManager.onDestroy();
  }
}