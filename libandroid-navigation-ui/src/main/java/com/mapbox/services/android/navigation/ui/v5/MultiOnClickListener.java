package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

import java.util.HashSet;
import java.util.Set;

class MultiOnClickListener implements View.OnClickListener {
  private Set<View.OnClickListener> onClickListeners;

  MultiOnClickListener() {
    this.onClickListeners = new HashSet<>();
  }

  void addListener(View.OnClickListener onClickListener) {
    onClickListeners.add(onClickListener);
  }

  void removeListener(View.OnClickListener onClickListener) {
    onClickListeners.remove(onClickListener);
  }

  void clearListeners() {
    onClickListeners.clear();
  }

  @Override
  public void onClick(View view) {
    for (View.OnClickListener onClickListener : onClickListeners) {
      onClickListener.onClick(view);
    }
  }
}
