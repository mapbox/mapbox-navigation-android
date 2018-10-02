package com.mapbox.services.android.navigation.ui.v5;

import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.mapboxsdk.maps.MapboxMap;

public class NavigationOnFlingListener implements MapboxMap.OnFlingListener {

  private final NavigationPresenter navigationPresenter;
  private final BottomSheetBehavior summaryBehavior;

  NavigationOnFlingListener(NavigationPresenter navigationPresenter, BottomSheetBehavior summaryBehavior) {
    this.navigationPresenter = navigationPresenter;
    this.summaryBehavior = summaryBehavior;
  }

  @Override
  public void onFling() {
    if (summaryBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
      navigationPresenter.onMapScroll();
    }
  }
}
