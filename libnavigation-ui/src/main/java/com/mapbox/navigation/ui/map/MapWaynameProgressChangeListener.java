package com.mapbox.navigation.ui.map;

import android.location.Location;

import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

class MapWaynameProgressChangeListener implements RouteProgressObserver, LocationObserver {

  private final MapWayName mapWayName;

  MapWaynameProgressChangeListener(MapWayName mapWayName) {
    this.mapWayName = mapWayName;
  }

  @Override
  public void onRawLocationChanged(@NotNull Location rawLocation) {
  }

  @Override
  public void onEnhancedLocationChanged(@NotNull Location enhancedLocation) {
    mapWayName.updateLocation(enhancedLocation);
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    mapWayName.updateProgress(routeProgress.currentLegProgress().currentStepProgress().stepPoints());
  }
}
