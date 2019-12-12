package com.mapbox.navigation.ui;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.geojson.Point;


class NavigationViewRouteEngineListener implements ViewRouteListener {

  private final NavigationViewModel navigationViewModel;

  NavigationViewRouteEngineListener(NavigationViewModel navigationViewModel) {
    this.navigationViewModel = navigationViewModel;
  }

  @Override
  public void onRouteUpdate(Route route) {
    navigationViewModel.updateRoute(directionsRoute);
  }

  @Override
  public void onRouteRequestError(String errorMessage) {
    if (navigationViewModel.isOffRoute()) {
      navigationViewModel.sendEventFailedReroute(errorMessage);
    }
  }

  @Override
  public void onDestinationSet(Point destination) {
    navigationViewModel.retrieveDestination().setValue(destination);
  }
}
