package com.mapbox.services.android.navigation.ui.v5.map;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.camera.OnTrackingModeChangedListener;

class FpsDelegate implements MapboxMap.OnCameraIdleListener, OnTrackingModeChangedListener {
  private final MapboxMap mapboxMap;
  private final LocationComponent locationComponent;
  private final MapView mapView;

  boolean locationThrottlingEnabled;
  private FpsMap locationFpsMap = null;
  private int currentLocationFps;

  boolean mapThrottlingEnabled;
  private FpsMap mapFpsMap = null;
  private int currentMapFps;

  FpsDelegate(@NonNull MapboxMap mapboxMap, @NonNull LocationComponent locationComponent,
              @NonNull MapView mapView) {
    this.mapboxMap = mapboxMap;
    this.locationComponent = locationComponent;
    this.mapView = mapView;
    mapboxMap.addOnCameraIdleListener(this);
  }

  @Override
  public void onCameraIdle() {
    throttle();
  }

  private double getCurrentZoom() {
    return mapboxMap.getCameraPosition().zoom;
  }

  private void throttle() {
    double zoom = getCurrentZoom();

    if (locationThrottlingEnabled && locationFpsMap != null) {
      throttleLocationComponent(zoom);
    }

    if (mapThrottlingEnabled && mapFpsMap != null) {
      throttleMap(zoom);
    }
  }

  private void throttleLocationComponent(double zoom) {
    int throttleFps = locationFpsMap.getFps(zoom);

    if (currentLocationFps != throttleFps) {
      currentLocationFps = throttleFps;
      locationComponent.setMaxAnimationFps(throttleFps);
    }
  }

  private void throttleMap(double zoom) {
    int throttleFps = mapFpsMap.getFps(zoom);

    if (currentMapFps != throttleFps) {
      currentMapFps = throttleFps;
      mapView.setMaximumFps(throttleFps);
    }
  }

  // location
  boolean isLocationThrottlingEnabled() {
    return mapThrottlingEnabled;
  }

  void updateLocationThrottlingEnabled(boolean isEnabled) {
    this.locationThrottlingEnabled = isEnabled;
    if (locationThrottlingEnabled) {
      throttleLocationComponent(getCurrentZoom());
    } else {
      throttleLocationComponent(Double.MAX_VALUE);
    }
  }

  ThrottleConfig retrieveLocationThrottleConfig() {
    return locationFpsMap.getThrottleConfig();
  }

  void updateLocationThrottleConfig(ThrottleConfig throttleConfig) {
    locationFpsMap = new FpsMap(throttleConfig);
  }

  // map
  boolean isMapThrottlingEnabled() {
    return mapThrottlingEnabled;
  }

  // miust be called after setting throttleconfig
  void updateMapThrottlingEnabled(boolean isEnabled) {
    this.mapThrottlingEnabled = isEnabled;
    if (mapThrottlingEnabled) {
      throttleMap(getCurrentZoom());
    } else {
      throttleMap(Double.MAX_VALUE);
    }
  }

  ThrottleConfig retrieveMapThrottleConfig() {
    return mapFpsMap.getThrottleConfig();
  }

  // miust be called after setting throttleconfig
  void updateMapThrottleConfig(ThrottleConfig throttleConfig) {
    mapFpsMap = new FpsMap(throttleConfig);
  }

  @Override
  public void onTrackingModeChanged(int trackingMode) {
    // todo where is this set on the camera?
    if (trackingMode == NavigationCamera.NAVIGATION_TRACKING_MODE_NONE) {
      throttleMap(Double.MAX_VALUE);
    }
  }

  void onStart() {
    mapboxMap.addOnCameraIdleListener(this);
  }

  void onStop() {
    mapboxMap.removeOnCameraIdleListener(this);
  }
}
