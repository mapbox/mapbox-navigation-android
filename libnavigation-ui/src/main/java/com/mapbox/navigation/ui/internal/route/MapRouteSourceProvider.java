package com.mapbox.navigation.ui.internal.route;

import androidx.annotation.NonNull;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

public class MapRouteSourceProvider {

  @NonNull
  public GeoJsonSource build(String id, FeatureCollection featureCollection, GeoJsonOptions options) {
    return new GeoJsonSource(id, featureCollection, options);
  }
}
