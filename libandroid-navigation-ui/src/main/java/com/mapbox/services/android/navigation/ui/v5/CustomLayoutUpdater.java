package com.mapbox.services.android.navigation.ui.v5;

import android.support.v4.view.AsyncLayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import timber.log.Timber;

public class CustomLayoutUpdater {

  private final AsyncLayoutInflater inflater;

  public CustomLayoutUpdater(AsyncLayoutInflater inflater) {
    this.inflater = inflater;
  }

  public void update(FrameLayout frame, int layoutResId, OnLayoutReplacedListener replacedListener) {
    frame.removeAllViews();
    inflater.inflate(layoutResId, frame, new CustomLayoutInflateFinishedListener(replacedListener));
  }

  public void update(FrameLayout frame, View customView) {
    if (customView == null) {
      Timber.e("Custom layout update failed: null View was provided.");
      return;
    }
    frame.removeAllViews();
    frame.addView(customView);
  }
}
