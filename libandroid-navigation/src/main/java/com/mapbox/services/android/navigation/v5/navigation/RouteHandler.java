package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;

import java.util.List;

class RouteHandler {

  private static final int INDEX_FIRST_ROUTE = 0;
  private static final int INDEX_FIRST_LEG = 0;
  private final MapboxNavigator mapboxNavigator;

  RouteHandler(MapboxNavigator mapboxNavigator) {
    this.mapboxNavigator = mapboxNavigator;
  }

  void updateRoute(DirectionsRoute route, DirectionsRouteType routeType) {
    if (routeType == DirectionsRouteType.NEW_ROUTE) {
      String routeJson = route.toJson();
      // TODO route_index (Which route to follow) and leg_index (Which leg to follow) are hardcoded for now
      mapboxNavigator.setRoute(routeJson, INDEX_FIRST_ROUTE, INDEX_FIRST_LEG);
    } else {
      List<RouteLeg> legs = route.legs();
      for (int i = 0; i < legs.size(); i++) {
        String annotationJson = legs.get(i).annotation().toJson();
        mapboxNavigator.updateAnnotations(annotationJson, INDEX_FIRST_ROUTE, i);
      }
    }
  }
}