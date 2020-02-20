package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class NavigationViewModelProgressChangeListener implements ProgressChangeListener {

  private final NavigationViewModel viewModel;

  NavigationViewModelProgressChangeListener(NavigationViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    viewModel.updateRouteProgress(routeProgress);
    viewModel.updateLocation(location);
  }
}