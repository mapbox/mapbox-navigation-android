package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;

import java.util.HashMap;
import java.util.List;

interface OnRouteFeaturesProcessedCallback {
  void onRouteFeaturesProcessed(List<FeatureCollection> routeFeatureCollections,
                                HashMap<LineString, DirectionsRoute> routeLineStrings);
}
