package com.mapbox.services.android.navigation.ui.v5.map;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class MapWaynameProgressChangeListener implements ProgressChangeListener {

  private final MapWayname mapWayname;

  MapWaynameProgressChangeListener(MapWayname mapWayname) {
    this.mapWayname = mapWayname;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    mapWayname.updateProgress(location, routeProgress.currentStepPoints());
  }
}
