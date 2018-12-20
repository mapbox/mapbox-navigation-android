package com.mapbox.services.android.navigation.ui.v5.route;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
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
    DirectionsRoute currentRoute = routeProgress.directionsRoute();
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
