package com.mapbox.services.android.navigation.ui.v5.map;


import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapboxMap;

class LocationFpsDelegate implements MapboxMap.OnCameraIdleListener {

  private static final int ZOOM_LEVEL_FIVE = 5;
  private static final int ZOOM_LEVEL_TEN = 10;
  private static final int ZOOM_LEVEL_FOURTEEN = 14;
  private static final int ZOOM_LEVEL_SIXTEEN = 16;
  private static final int ZOOM_LEVEL_EIGHTEEN = 18;
  private static final int MAX_ANIMATION_FPS_THREE = 3;
  private static final int MAX_ANIMATION_FPS_FIVE = 5;
  private static final int MAX_ANIMATION_FPS_TEN = 10;
  private static final int MAX_ANIMATION_FPS_FIFTEEN = 15;
  private static final int MAX_ANIMATION_FPS_TWENTY_FIVE = 25;
  private static final int MAX_ANIMATION_FPS = Integer.MAX_VALUE;
  private final MapboxMap mapboxMap;
  private final LocationComponent locationComponent;
  private int currentFps = MAX_ANIMATION_FPS;
  private boolean isEnabled = true;

  LocationFpsDelegate(@NonNull MapboxMap mapboxMap, @NonNull LocationComponent locationComponent) {
    this.mapboxMap = mapboxMap;
    this.locationComponent = locationComponent;
    mapboxMap.addOnCameraIdleListener(this);
  }

  @Override
  public void onCameraIdle() {
    if (!isEnabled) {
      return;
    }
    updateMaxFps();
  }

  void onStart() {
    mapboxMap.addOnCameraIdleListener(this);
  }

  void onStop() {
    mapboxMap.removeOnCameraIdleListener(this);
  }

  void updateEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    resetMaxFps();
  }

  boolean isEnabled() {
    return isEnabled;
  }

  private void updateMaxFps() {
    double zoom = mapboxMap.getCameraPosition().zoom;
    int maxAnimationFps = buildFpsFrom(zoom);
    if (currentFps != maxAnimationFps) {
      locationComponent.setMaxAnimationFps(maxAnimationFps);
      currentFps = maxAnimationFps;
    }
  }

  private int buildFpsFrom(double zoom) {
    int maxAnimationFps;
    if (zoom < ZOOM_LEVEL_FIVE) {
      maxAnimationFps = MAX_ANIMATION_FPS_THREE;
    } else if (zoom < ZOOM_LEVEL_TEN) {
      maxAnimationFps = MAX_ANIMATION_FPS_FIVE;
    } else if (zoom < ZOOM_LEVEL_FOURTEEN) {
      maxAnimationFps = MAX_ANIMATION_FPS_TEN;
    } else if (zoom < ZOOM_LEVEL_SIXTEEN) {
      maxAnimationFps = MAX_ANIMATION_FPS_FIFTEEN;
    } else if (zoom < ZOOM_LEVEL_EIGHTEEN) {
      maxAnimationFps = MAX_ANIMATION_FPS_TWENTY_FIVE;
    } else {
      maxAnimationFps = MAX_ANIMATION_FPS;
    }
    return maxAnimationFps;
  }

  private void resetMaxFps() {
    if (!isEnabled) {
      locationComponent.setMaxAnimationFps(MAX_ANIMATION_FPS);
    }
  }
}
