package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.maps.MapboxMap;

class CameraOverviewCancelableCallback implements MapboxMap.CancelableCallback {

  private static final int OVERVIEW_UPDATE_DURATION_IN_MILLIS = 750;

  private CameraUpdate overviewUpdate;
  private MapboxMap mapboxMap;

  CameraOverviewCancelableCallback(CameraUpdate overviewUpdate, MapboxMap mapboxMap) {
    this.overviewUpdate = overviewUpdate;
    this.mapboxMap = mapboxMap;
  }

  @Override
  public void onCancel() {
    // No-op
  }

  @Override
  public void onFinish() {
    mapboxMap.animateCamera(overviewUpdate, OVERVIEW_UPDATE_DURATION_IN_MILLIS);
  }
}
