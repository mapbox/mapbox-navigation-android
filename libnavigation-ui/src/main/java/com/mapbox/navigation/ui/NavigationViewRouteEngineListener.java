package com.mapbox.navigation.ui;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.base.route.Router;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class NavigationViewRouteEngineListener implements Router.Callback {

  private final NavigationViewModel navigationViewModel;

  NavigationViewRouteEngineListener(NavigationViewModel navigationViewModel) {
    this.navigationViewModel = navigationViewModel;
  }

  @Override
  public void onResponse(@NotNull List<? extends DirectionsRoute> routes) {
    navigationViewModel.updateRoute(routes.get(0));
  }

  @Override
  public void onFailure(@NotNull Throwable throwable) {
    navigationViewModel.sendEventFailedReroute(throwable.getLocalizedMessage());
  }

  @Override
  public void onCanceled() {

  }
}
