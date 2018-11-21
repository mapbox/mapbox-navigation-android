package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AsyncLayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class CustomLayoutInflateFinishedListener implements AsyncLayoutInflater.OnInflateFinishedListener {

  private final OnLayoutReplacedListener listener;

  CustomLayoutInflateFinishedListener(@NonNull OnLayoutReplacedListener listener) {
    this.listener = listener;
  }

  @Override
  public void onInflateFinished(@NonNull View customView, int resId, @Nullable ViewGroup frame) {
    if (frame == null) {
      return;
    }
    frame.addView(customView);
    listener.onLayoutReplaced();
  }
}
