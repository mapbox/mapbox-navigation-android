package com.mapbox.navigation.ui.route;

import android.location.Location;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

class MapRouteProgressChangeListener implements ProgressChangeListener {

  private final MapRouteLine routeLine;
  private final MapRouteArrow routeArrow;
  private boolean isVisible = true;

  MapRouteProgressChangeListener(MapRouteLine routeLine, MapRouteArrow routeArrow) {
    this.routeLine = routeLine;
    this.routeArrow = routeArrow;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (!isVisible) {
      return;
    }
    Route currentRoute = routeProgress.directionsRoute();
    List<Route> directionsRoutes = routeLine.retrieveDirectionsRoutes();
    int primaryRouteIndex = routeLine.retrievePrimaryRouteIndex();
    addNewRoute(currentRoute, directionsRoutes, primaryRouteIndex);
    routeArrow.addUpcomingManeuverArrow(routeProgress);
  }

  void updateVisibility(boolean isVisible) {
    this.isVisible = isVisible;
  }

  private void addNewRoute(Route currentRoute, List<Route> directionsRoutes,
                           int primaryRouteIndex) {
    if (isANewRoute(currentRoute, directionsRoutes, primaryRouteIndex)) {
      routeLine.draw(currentRoute);
    }
  }

  private boolean isANewRoute(Route currentRoute, List<Route> directionsRoutes,
                              int primaryRouteIndex) {
    boolean noRoutes = directionsRoutes.isEmpty();
    return noRoutes || !currentRoute.equals(directionsRoutes.get(primaryRouteIndex));
  }
}
