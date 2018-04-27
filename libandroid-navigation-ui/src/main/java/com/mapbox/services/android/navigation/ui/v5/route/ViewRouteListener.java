package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

public interface ViewRouteListener {

  void onRouteUpdate(DirectionsRoute directionsRoute);

  void onRouteRequestError(Throwable throwable);

  void onDestinationSet(Point destination);
}
