package com.mapbox.navigation.ui;

import android.location.Location;

import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class NavigationViewModelProgressChangeListener implements RouteProgressObserver, LocationObserver {

  private final NavigationViewModel viewModel;

  NavigationViewModelProgressChangeListener(NavigationViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @Override
  public void onRawLocationChanged(@NotNull Location rawLocation) {
  }

  @Override
  public void onEnhancedLocationChanged(@NotNull Location enhancedLocation, @NotNull List<? extends Location> keyPoints) {
    viewModel.updateLocation(enhancedLocation);
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    viewModel.updateRouteProgress(routeProgress);
  }
}