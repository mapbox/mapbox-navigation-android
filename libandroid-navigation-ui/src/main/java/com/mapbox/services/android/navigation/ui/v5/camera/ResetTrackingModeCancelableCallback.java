package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.maps.MapboxMap;

class ResetTrackingModeCancelableCallback implements MapboxMap.CancelableCallback {

  private final NavigationCamera camera;
  private final int trackingMode;

  ResetTrackingModeCancelableCallback(NavigationCamera camera, @NavigationCamera.TrackingMode int trackingMode) {
    this.camera = camera;
    this.trackingMode = trackingMode;
  }

  @Override
  public void onCancel() {
    camera.updateCameraTrackingMode(trackingMode);
  }

  @Override
  public void onFinish() {
    camera.updateCameraTrackingMode(trackingMode);
  }
}
