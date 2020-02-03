package com.mapbox.navigation.ui.map;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class MapWaynameProgressChangeListener implements ProgressChangeListener {

  private final MapWayName mapWayName;

  MapWaynameProgressChangeListener(MapWayName mapWayName) {
    this.mapWayName = mapWayName;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    mapWayName.updateProgress(location, routeProgress.currentStepPoints());
  }
}
