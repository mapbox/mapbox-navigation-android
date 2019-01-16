package com.mapbox.services.android.navigation.ui.v5;

import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;

/**
 * Listener used to detect user interaction with the map while driving.
 * <p>
 * If the camera tracking is dismissed, we notify the presenter to adjust UI accordingly.
 */
class NavigationOnCameraTrackingChangedListener implements OnCameraTrackingChangedListener {

  private final NavigationPresenter navigationPresenter;
  private final BottomSheetBehavior summaryBehavior;

  NavigationOnCameraTrackingChangedListener(NavigationPresenter navigationPresenter,
                                            BottomSheetBehavior summaryBehavior) {
    this.navigationPresenter = navigationPresenter;
    this.summaryBehavior = summaryBehavior;
  }

  @Override
  public void onCameraTrackingDismissed() {
    if (summaryBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
      navigationPresenter.onCameraTrackingDismissed();
    }
  }

  @Override
  public void onCameraTrackingChanged(int currentMode) {
    // Intentionally empty
  }
}
