package com.mapbox.navigation.ui.map;

import androidx.annotation.NonNull;

import java.util.List;

class MapWayNameChangedListener implements OnWayNameChangedListener {

  private final List<OnWayNameChangedListener> listeners;

  MapWayNameChangedListener(List<OnWayNameChangedListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void onWayNameChanged(@NonNull String wayName) {
    for (OnWayNameChangedListener listener : listeners) {
      listener.onWayNameChanged(wayName);
    }
  }
}
