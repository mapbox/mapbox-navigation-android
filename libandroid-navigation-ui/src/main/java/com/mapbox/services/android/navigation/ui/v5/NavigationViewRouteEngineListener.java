package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteListener;


class NavigationViewRouteEngineListener implements ViewRouteListener {

  private final NavigationViewModel navigationViewModel;

  NavigationViewRouteEngineListener(NavigationViewModel navigationViewModel) {
    this.navigationViewModel = navigationViewModel;
  }

  @Override
  public void onRouteUpdate(DirectionsRoute directionsRoute) {
    if (!navigationViewModel.isRunning() || navigationViewModel.isOffRoute()) {
      navigationViewModel.updateRoute(directionsRoute);
    }
  }

  @Override
  public void onRouteRequestError(Throwable throwable) {
    if (navigationViewModel.isOffRoute()) {
      String errorMessage = throwable.getMessage();
      navigationViewModel.sendEventFailedReroute(errorMessage);
    }
  }

  @Override
  public void onDestinationSet(Point destination) {
    navigationViewModel.retrieveDestination().setValue(destination);
  }
}
