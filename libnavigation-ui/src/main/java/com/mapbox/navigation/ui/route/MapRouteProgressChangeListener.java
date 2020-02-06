package com.mapbox.navigation.ui.route;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class MapRouteProgressChangeListener implements RouteProgressObserver {

  private final MapRouteLine routeLine;
  private final MapRouteArrow routeArrow;
  private boolean isVisible = true;

  MapRouteProgressChangeListener(MapRouteLine routeLine, MapRouteArrow routeArrow) {
    this.routeLine = routeLine;
    this.routeArrow = routeArrow;
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    onProgressChange(routeProgress);
  }

  public void onProgressChange(RouteProgress routeProgress) {
    if (!isVisible) {
      return;
    }
    DirectionsRoute currentRoute = routeProgress.route();
    List<DirectionsRoute> directionsRoutes = routeLine.retrieveDirectionsRoutes();
    int primaryRouteIndex = routeLine.retrievePrimaryRouteIndex();
    addNewRoute(currentRoute, directionsRoutes, primaryRouteIndex);
    routeArrow.addUpcomingManeuverArrow(routeProgress);
  }

  void updateVisibility(boolean isVisible) {
    this.isVisible = isVisible;
  }

  private void addNewRoute(DirectionsRoute currentRoute, List<DirectionsRoute> directionsRoutes,
                           int primaryRouteIndex) {
    if (isANewRoute(currentRoute, directionsRoutes, primaryRouteIndex)) {
      routeLine.draw(currentRoute);
    }
  }

  private boolean isANewRoute(DirectionsRoute currentRoute, List<DirectionsRoute> directionsRoutes,
                              int primaryRouteIndex) {
    boolean noRoutes = directionsRoutes.isEmpty();
    return noRoutes || !currentRoute.equals(directionsRoutes.get(primaryRouteIndex));
  }
}
