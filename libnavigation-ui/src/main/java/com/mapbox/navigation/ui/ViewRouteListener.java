package com.mapbox.navigation.ui;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.geojson.Point;

interface ViewRouteListener {

  void onRouteUpdate(Route route);

  void onRouteRequestError(String errorMessage);

  void onDestinationSet(Point destination);
}
