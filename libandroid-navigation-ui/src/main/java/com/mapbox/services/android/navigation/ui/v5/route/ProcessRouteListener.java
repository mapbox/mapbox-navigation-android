package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.services.commons.geojson.FeatureCollection;

import java.util.List;

interface ProcessRouteListener {

  void onRouteProcessed(List<FeatureCollection> featureCollections);
}
