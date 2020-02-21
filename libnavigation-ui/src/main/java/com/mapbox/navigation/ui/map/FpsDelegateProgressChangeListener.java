package com.mapbox.navigation.ui.map;

import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

class FpsDelegateProgressChangeListener implements RouteProgressObserver {

  private final MapFpsDelegate fpsDelegate;

  FpsDelegateProgressChangeListener(MapFpsDelegate fpsDelegate) {
    this.fpsDelegate = fpsDelegate;
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    fpsDelegate.adjustFpsFor(routeProgress);
  }
}
