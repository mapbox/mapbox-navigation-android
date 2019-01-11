package com.mapbox.services.android.navigation.ui.v5.camera;

/**
 * Listener that gets invoked when the navigation camera finishes a transition
 * to a new tracking mode.
 */
public interface OnTrackingModeTransitionListener {

  /**
   * Invoked when the camera has finished a transition to a new tracking mode.
   */
  void onTransitionFinished(@NavigationCamera.TrackingMode int trackingMode);

  /**
   * Invoked when the transition to a new tracking mode has been cancelled.
   */
  void onTransitionCancelled(@NavigationCamera.TrackingMode int trackingMode);
}
