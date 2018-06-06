package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

class WaynameFeatureFinder {

  private MapboxMap mapboxMap;

  WaynameFeatureFinder(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  List<Feature> queryRenderedFeatures(PointF point, String[] layerIds) {
    return mapboxMap.queryRenderedFeatures(point, layerIds);
  }
}
