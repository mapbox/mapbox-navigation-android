package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class MapRouteProgressChangeListener implements ProgressChangeListener {

  private final NavigationMapRoute mapRoute;

  public MapRouteProgressChangeListener(NavigationMapRoute mapRoute) {
    this.mapRoute = mapRoute;
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    DirectionsRoute currentRoute = routeProgress.directionsRoute();
    List<DirectionsRoute> directionsRoutes = mapRoute.retrieveDirectionsRoutes();
    int primaryRouteIndex = mapRoute.retrievePrimaryRouteIndex();
    addNewRoute(currentRoute, directionsRoutes, primaryRouteIndex);
    mapRoute.addUpcomingManeuverArrow(routeProgress);
  }

  private void addNewRoute(DirectionsRoute currentRoute, List<DirectionsRoute> directionsRoutes,
                           int primaryRouteIndex) {
    if (isANewRoute(currentRoute, directionsRoutes, primaryRouteIndex)) {
      mapRoute.addRoute(currentRoute);
    }
  }

  private boolean isANewRoute(DirectionsRoute currentRoute, List<DirectionsRoute> directionsRoutes,
                              int primaryRouteIndex) {
    boolean noRoutes = directionsRoutes.isEmpty();
    return noRoutes || !currentRoute.equals(directionsRoutes.get(primaryRouteIndex));
  }
}
