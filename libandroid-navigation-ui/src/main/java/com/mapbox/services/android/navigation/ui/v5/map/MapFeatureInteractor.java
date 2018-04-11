package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

public class MapFeatureInteractor {

  private MapboxMap mapboxMap;

  MapFeatureInteractor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  public List<Feature> queryRenderedFeatures(PointF point, String[] layerIds) {
    return mapboxMap.queryRenderedFeatures(point, layerIds);
  }
}
