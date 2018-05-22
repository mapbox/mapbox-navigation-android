package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

class AddProgressListenerCancelableCallback implements MapboxMap.CancelableCallback {

  private MapboxNavigation navigation;
  private ProgressChangeListener progressChangeListener;

  AddProgressListenerCancelableCallback(MapboxNavigation navigation,
                                        ProgressChangeListener progressChangeListener) {
    this.navigation = navigation;
    this.progressChangeListener = progressChangeListener;
  }

  @Override
  public void onCancel() {
    addProgressChangeListener();
  }

  @Override
  public void onFinish() {
    addProgressChangeListener();
  }

  private void addProgressChangeListener() {
    navigation.addProgressChangeListener(progressChangeListener);
  }
}
