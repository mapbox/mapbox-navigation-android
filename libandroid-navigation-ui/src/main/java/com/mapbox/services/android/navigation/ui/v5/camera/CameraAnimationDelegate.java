package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;

class CameraAnimationDelegate {

  private final MapboxMap mapboxMap;

  CameraAnimationDelegate(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  void render(NavigationCameraUpdate update, int durationMs, MapboxMap.CancelableCallback callback) {
    CameraUpdateMode mode = update.getMode();
    CameraUpdate cameraUpdate = update.getCameraUpdate();
    if (mode == CameraUpdateMode.OVERRIDE) {
      mapboxMap.getLocationComponent().setCameraMode(CameraMode.NONE);
      mapboxMap.animateCamera(cameraUpdate, durationMs, callback);
    } else if (!isTracking()) {
      mapboxMap.animateCamera(cameraUpdate, durationMs, callback);
    }
  }

  private boolean isTracking() {
    int cameraMode = mapboxMap.getLocationComponent().getCameraMode();
    return cameraMode != CameraMode.NONE;
  }
}