package com.mapbox.services.android.navigation.ui.v5.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;

import static com.mapbox.mapboxsdk.style.functions.stops.Stop.stop;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.categorical;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.exponential;

public class MapUtils {

  private MapUtils() {
  }

  public static void updateMapSourceFromFeatureCollection(@NonNull MapboxMap mapboxMap,
                                                          @Nullable FeatureCollection collection,
                                                          @NonNull String sourceId) {
    if (collection == null) {
      collection = FeatureCollection.fromFeatures(new Feature[]{});
    }

    GeoJsonSource source = mapboxMap.getSourceAs(sourceId);
    if (source == null) {
      GeoJsonOptions routeGeoJsonOptions = new GeoJsonOptions().withMaxZoom(16);
      GeoJsonSource routeSource = new GeoJsonSource(sourceId, collection, routeGeoJsonOptions);
      mapboxMap.addSource(routeSource);
    } else {
      source.setGeoJson(collection);
    }
  }





















}
