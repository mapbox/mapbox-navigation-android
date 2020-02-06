package com.mapbox.navigation.ui;

import android.location.Location;

import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

class NavigationViewModelProgressChangeListener implements RouteProgressObserver, LocationObserver {

  private final NavigationViewModel viewModel;

  NavigationViewModelProgressChangeListener(NavigationViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @Override
  public void onRawLocationChanged(@NotNull Location rawLocation) {
    viewModel.updateLocation(rawLocation);
  }

  @Override
  public void onEnhancedLocationChanged(@NotNull Location enhancedLocation) {

  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    viewModel.updateRouteProgress(routeProgress);
  }
}