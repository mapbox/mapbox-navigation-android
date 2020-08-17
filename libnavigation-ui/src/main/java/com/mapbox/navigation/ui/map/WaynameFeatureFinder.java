package com.mapbox.navigation.ui.map;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

class WaynameFeatureFinder {

  private MapboxMap mapboxMap;

  WaynameFeatureFinder(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  @NonNull
  List<Feature> queryRenderedFeatures(@NonNull PointF point, String[] layerIds) {
    return mapboxMap.queryRenderedFeatures(point, layerIds);
  }
}
