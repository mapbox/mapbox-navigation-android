package com.mapbox.services.android.navigation.ui.v5.camera;

/**
 * This class is passed to {@link NavigationCameraUpdate} to
 * determine the update's behavior when passed to {@link NavigationCamera}.
 */
public enum CameraUpdateMode {

  /**
   * For a given {@link NavigationCameraUpdate}, this default mode means the
   * {@link NavigationCamera} will ignore the update when tracking is already
   * enabled.
   * <p>
   * If tracking is disabled, the update animation will execute.
   */
  DEFAULT,

  /**
   * For a given {@link NavigationCameraUpdate}, this override mode means the
   * {@link NavigationCamera} will stop tracking (if tracking) and execute the
   * given update animation.
   */
  OVERRIDE
}