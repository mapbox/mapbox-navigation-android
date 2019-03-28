package com.mapbox.services.android.navigation.ui.v5.camera;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.camera.CameraUpdate;

/**
 * Used with {@link NavigationCamera#update(NavigationCameraUpdate)}.
 * <p>
 * This class wraps a Maps SDK {@link CameraUpdate}.  It adds an option
 * for {@link CameraUpdateMode} that determine how the camera update behaves
 * with tracking modes.
 */
public class NavigationCameraUpdate {

  private final CameraUpdate cameraUpdate;
  private CameraUpdateMode mode = CameraUpdateMode.DEFAULT;

  /**
   * Creates and instance of this class, taking a {@link CameraUpdate}
   * that has already been built.
   *
   * @param cameraUpdate with map camera parameters
   */
  public NavigationCameraUpdate(@NonNull CameraUpdate cameraUpdate) {
    this.cameraUpdate = cameraUpdate;
  }

  /**
   * Updates the {@link CameraUpdateMode} that will determine this updates
   * behavior based on the current tracking mode in {@link NavigationCamera}.
   *
   * @param mode either default or override
   */
  public void setMode(CameraUpdateMode mode) {
    this.mode = mode;
  }

  @NonNull
  CameraUpdate getCameraUpdate() {
    return cameraUpdate;
  }

  @NonNull
  CameraUpdateMode getMode() {
    return mode;
  }
}